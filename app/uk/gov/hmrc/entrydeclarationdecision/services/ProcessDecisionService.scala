/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.entrydeclarationdecision.services

import cats.data.EitherT
import cats.implicits._
import com.codahale.metrics.MetricRegistry
import play.api.Logging
import uk.gov.hmrc.entrydeclarationdecision.config.AppConfig
import uk.gov.hmrc.entrydeclarationdecision.connectors.{OutcomeConnector, StoreConnector}
import uk.gov.hmrc.entrydeclarationdecision.logging.{ContextLogger, LoggingContext}
import uk.gov.hmrc.entrydeclarationdecision.models.ErrorCode
import uk.gov.hmrc.entrydeclarationdecision.models.decision.MessageType.{IE304, IE305, IE316, IE328}
import uk.gov.hmrc.entrydeclarationdecision.models.decision.{Decision, DecisionResponse, MessageType}
import uk.gov.hmrc.entrydeclarationdecision.models.enrichment.Enrichment
import uk.gov.hmrc.entrydeclarationdecision.models.outcome.Outcome
import uk.gov.hmrc.entrydeclarationdecision.reporting.{EisResponseTime, ReportSender}
import uk.gov.hmrc.entrydeclarationdecision.utils._
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{Clock, Duration, Instant}
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success
import scala.xml.{Node, Utility}

@Singleton
class ProcessDecisionService @Inject()(
  appConfig: AppConfig,
  outcomeConnector: OutcomeConnector,
  storeConnector: StoreConnector,
  declarationAcceptanceXMLBuilder: DeclarationAcceptanceXMLBuilder,
  declarationRejectionXMLBuilder: DeclarationRejectionXMLBuilder,
  amendmentAcceptanceXMLBuilder: AmendmentAcceptanceXMLBuilder,
  amendmentRejectionXMLBuilder: AmendmentRejectionXMLBuilder,
  schemaValidator: SchemaValidator,
  xmlWrapper: XMLWrapper,
  pagerDutyLogger: PagerDutyLogger,
  reportSender: ReportSender,
  override val clock: Clock,
  override val metrics: MetricRegistry)(implicit ex: ExecutionContext)
    extends Timer
    with Logging {

  def processDecision[R <: DecisionResponse](
    decision: Decision[R])(implicit hc: HeaderCarrier, lc: LoggingContext): Future[Either[ErrorCode, Unit]] =
    timeFuture("Service processDecision", "processDecision.total") {

      val amendment = isAmendment(decision.metadata.messageType)

      def processDecisionResponse(decisionResponse: DecisionResponse) =
        decisionResponse match {
          case resp: DecisionResponse.Acceptance =>
            if (amendment) {
              doProcessDecision(
                decision.withResponse(resp),
                amendmentAcceptanceXMLBuilder,
                storeConnector.getAcceptanceEnrichment(_, amendment))
            } else {
              doProcessDecision(
                decision.withResponse(resp),
                declarationAcceptanceXMLBuilder,
                storeConnector.getAcceptanceEnrichment(_, amendment))
            }

          case resp: DecisionResponse.Rejection =>
            if (amendment) {
              doProcessDecision(
                decision.withResponse(resp),
                amendmentRejectionXMLBuilder,
                storeConnector.getAmendmentRejectionEnrichment)
            } else {
              doProcessDecision(
                decision.withResponse(resp),
                declarationRejectionXMLBuilder,
                storeConnector.getDeclarationRejectionEnrichment)
            }
        }

      processDecisionResponse(decision.response).andThen {
        case Success(Right(_)) =>
          logLongJourneyTime(decision.metadata.receivedDateTime, appConfig.longJourneyTime)
          storeConnector.setShortTtl(decision.submissionId)
      }

    }

  private def isAmendment(messageType: MessageType): Boolean = messageType match {
    case IE304 | IE305 => true
    case IE316 | IE328 => false
  }

  private def doProcessDecision[R <: DecisionResponse, E <: Enrichment](
    decision: Decision[R],
    xmlBuilder: XMLBuilder[R, E],
    enricher: String => Future[Either[ErrorCode, E]])(implicit hc: HeaderCarrier, lc: LoggingContext) = {

    val (metricString, validationSchema) = decision.metadata.messageType match {
      case IE304 => ("AmendmentAcceptance", SchemaType.CC304A)
      case IE305 => ("AmendmentRejection", SchemaType.CC305A)
      case IE316 => ("DeclarationRejection", SchemaType.CC316A)
      case IE328 => ("DeclarationAcceptance", SchemaType.CC328A)
    }

    def getEnrichment(decision: Decision[R]): Future[Either[ErrorCode, E]] =
      timeFuture("Get Enrichment", s"processDecision.get${metricString}Enrichment") {
        enricher(decision.submissionId)
      }

    def buildXML(decision: Decision[R], enrichment: E) =
      time("Build XML", s"build${metricString}XML") {
        xmlBuilder.buildXML(decision, enrichment)
      }

    timeFuture("Service processDecision", s"processDecision.${metricString}Total") {

      val result = for {
        enrichment <- EitherT(getEnrichment(decision))
        xml        = buildXML(decision, enrichment)
        _          = validateSchema(validationSchema, xml)
        wrappedXml = xmlWrapper.wrapXml(decision.metadata.correlationId, xml)
        sendResult <- EitherT(sendOutcome(decision, wrappedXml))
      } yield {
        enrichment.eisSubmissionDateTime.foreach(time =>
          reportSender.sendReport(EisResponseTime(timeFrom("E2E.eisDecision-e2eTimer", time))))
        sendResult
      }

      result.value
    }
  }

  private def sendOutcome[R <: DecisionResponse](decision: Decision[R], xml: Node)(
    implicit hc: HeaderCarrier,
    lc: LoggingContext): Future[Either[ErrorCode, Unit]] =
    timeFuture("Save Outcome", "processDecision.sendOutcome") {
      val xmlString = Utility.trim(xml).toString

      val mrn = decision.response match {
        case DecisionResponse.Acceptance(mrn, _) => Some(mrn)
        case _                                   => None
      }

      outcomeConnector.send(
        Outcome(
          eori                    = decision.metadata.senderEORI,
          correlationId           = decision.metadata.correlationId,
          submissionId            = decision.submissionId,
          receivedDateTime        = decision.metadata.receivedDateTime,
          messageType             = decision.metadata.messageType,
          movementReferenceNumber = mrn,
          outcomeXml              = xmlString
        )
      )
    }

  private def validateSchema(schemaType: SchemaType, xml: Node)(implicit lc: LoggingContext): Unit =
    if (appConfig.validateJsonToXMLTransformation) {
      val result = schemaValidator.validateSchema(schemaType, xml)
      if (!result.isValid) {
        ContextLogger.debug(
          s"\n$xml\n is not valid against $schemaType schema:\n ${result.allErrors.map(_.getMessage).mkString("\n")}")
        ContextLogger.warn(
          s"XML is not valid against $schemaType schema:\n ${result.allErrors.map(_.getMessage).mkString("\n")}")
      }
    }

  private def logLongJourneyTime(startTime: Instant, longJourneyTime: FiniteDuration)(
    implicit lc: LoggingContext): Unit = {
    val journeyTime =
      FiniteDuration(Duration.between(startTime, Instant.now(clock)).toNanos, TimeUnit.NANOSECONDS)
    if (journeyTime >= longJourneyTime) {
      pagerDutyLogger.logLongJourneyTime(journeyTime, longJourneyTime)
    }
  }
}

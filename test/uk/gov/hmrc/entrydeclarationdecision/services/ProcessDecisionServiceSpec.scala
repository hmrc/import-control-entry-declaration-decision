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

import com.kenshoo.play.metrics.Metrics
import org.scalamock.handlers.CallHandler
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.time.{Millis, Span}
import org.scalatest.{AppendedClues, Assertion}
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import uk.gov.hmrc.entrydeclarationdecision.config.MockAppConfig
import uk.gov.hmrc.entrydeclarationdecision.connectors.{MockOutcomeConnector, MockStoreConnector}
import uk.gov.hmrc.entrydeclarationdecision.logging.LoggingContext
import uk.gov.hmrc.entrydeclarationdecision.models.ErrorCode
import uk.gov.hmrc.entrydeclarationdecision.models.decision._
import uk.gov.hmrc.entrydeclarationdecision.models.enrichment.Enrichment
import uk.gov.hmrc.entrydeclarationdecision.models.enrichment.acceptance.AcceptanceEnrichment
import uk.gov.hmrc.entrydeclarationdecision.models.enrichment.rejection.{AmendmentRejectionEnrichment, DeclarationRejectionEnrichment}
import uk.gov.hmrc.entrydeclarationdecision.models.outcome.Outcome
import uk.gov.hmrc.entrydeclarationdecision.reporting.{EisResponseTime, MockReportSender}
import uk.gov.hmrc.entrydeclarationdecision.utils._
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{Clock, Instant, ZoneOffset}
import java.util.concurrent.atomic.AtomicBoolean
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Future, Promise}
import scala.xml.{Elem, SAXParseException}

class ProcessDecisionServiceSpec
    extends PlaySpec
    with MockAppConfig
    with MockOutcomeConnector
    with MockStoreConnector
    with MockDeclarationAcceptanceXMLBuilder
    with MockAmendmentAcceptanceXMLBuilder
    with MockAmendmentRejectionXMLBuilder
    with MockDeclarationRejectionXMLBuilder
    with MockSchemaValidator
    with MockPagerDutyLogger
    with MockReportSender
    with MockXMLWrapper
    with ScalaFutures
    with AppendedClues {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(timeout = Span(500, Millis))

  implicit val hc: HeaderCarrier  = HeaderCarrier()
  implicit val lc: LoggingContext = LoggingContext("eori", "corrId", "subId", Some("mrn"))

  val time: Instant = Instant.now
  val clock: Clock  = Clock.fixed(time, ZoneOffset.UTC)

  val e2eTimerName = "E2E.eisDecision-e2eTimer"

  class Test {
    val mockedMetrics: Metrics = new MockMetrics

    val service = new ProcessDecisionService(
      mockAppConfig,
      mockOutcomeConnector,
      mockStoreConnector,
      mockDeclarationAcceptanceXMLBuilder,
      mockRejectionXMLBuilder,
      mockAmendmentAcceptanceXMLBuilder,
      mockAmendmentRejectionXMLBuilder,
      mockSchemaValidator,
      mockXMLWrapper,
      mockPagerDutyLogger,
      mockReportSender,
      clock,
      mockedMetrics
    )

    def shouldReportMetric(): Assertion =
      mockedMetrics.defaultRegistry.timer(e2eTimerName).getCount shouldBe 1 withClue "should report metric"

    def shouldNotReportMetric(): Assertion =
      mockedMetrics.defaultRegistry.timer(e2eTimerName).getCount shouldBe 0 withClue "should not report metric"
  }

  // WLOG
  private val declarationAcceptanceEnrichment =
    ResourceUtils.withInputStreamFor("jsons/DeclarationAcceptanceEnrichment.json")(
      Json.parse(_).as[AcceptanceEnrichment])
  private val amendmentAcceptanceEnrichment =
    ResourceUtils.withInputStreamFor("jsons/AmendmentAcceptanceEnrichment.json")(Json.parse(_).as[AcceptanceEnrichment])
  private val amendmentRejectionEnrichment =
    ResourceUtils.withInputStreamFor("jsons/AmendmentRejectionEnrichment.json")(
      Json.parse(_).as[AmendmentRejectionEnrichment])
  private val declarationRejectionEnrichment =
    ResourceUtils.withInputStreamFor("jsons/DeclarationRejectionEnrichment.json")(
      Json.parse(_).as[DeclarationRejectionEnrichment])

  private val shortJourneyTimeThreshold: FiniteDuration = 0.seconds
  private val longJourneyTimeThreshold: FiniteDuration = 2.seconds
  private val journeyTime: FiniteDuration     = 1.seconds

  private val rawXml              = <rawXml/>
  private val wrappedXml          = <wrapped/>
  private val submissionId        = "sumbissionID"
  private val correlationId       = "15digitCorrelationID"
  private val preparationDateTime = Instant.parse("2020-12-31T23:59:00Z")
  private val receivedDateTime    = time.minusSeconds(journeyTime.toSeconds)
  private val rejectionDateTime   = Instant.parse("2020-12-31T23:59:00Z")
  private val acceptedDateTime    = Instant.parse("2020-12-31T23:59:00Z")
  private val eisSubmissionDateTime = Instant.parse("2003-02-11T12:34:00.000Z") //From Json bodies

  private val timeDifference: java.time.Duration = java.time.Duration.between(eisSubmissionDateTime, time)

  private def validOutcome(messageType: MessageType, includeMrn: Boolean) =
    Outcome(
      "eori",
      correlationId,
      submissionId,
      receivedDateTime,
      messageType,
      if (includeMrn) Some("02CHPW67QLOYOB4IA8") else None,
      wrappedXml.toString)

  private val validDeclarationRejectionDecision = Decision(
    submissionId,
    DecisionMetadata(
      "eori",
      "pariat",
      MessageType.IE316,
      "msgId",
      preparationDateTime,
      receivedDateTime,
      correlationId,
      None),
    DecisionResponse.Rejection(List(DecisionError("cheese", "nisi cupidatat", None, None)), rejectionDateTime)
  )

  private val validDeclarationAcceptanceDecision = Decision(
    submissionId,
    DecisionMetadata(
      "eori",
      "pariat",
      MessageType.IE328,
      "msgId",
      preparationDateTime,
      receivedDateTime,
      correlationId,
      None),
    DecisionResponse.Acceptance("02CHPW67QLOYOB4IA8", acceptedDateTime)
  )

  private val validAmendmentAcceptanceDecision = Decision(
    submissionId,
    DecisionMetadata(
      "eori",
      "pariat",
      MessageType.IE304,
      "msgId",
      preparationDateTime,
      receivedDateTime,
      correlationId,
      None),
    DecisionResponse.Acceptance("02CHPW67QLOYOB4IA8", acceptedDateTime)
  )

  private val validAmendmentRejectionDecision = Decision(
    submissionId,
    DecisionMetadata(
      "eori",
      "pariat",
      MessageType.IE305,
      "msgId",
      preparationDateTime,
      receivedDateTime,
      correlationId,
      None),
    DecisionResponse.Rejection(List(DecisionError("cheese", "nisi cupidatat", None, None)), rejectionDateTime)
  )

  "ProcessDecisionService" when {
    "processing a declaration acceptance" must {
      behave like decisionProcessing(
        validDeclarationAcceptanceDecision,
        declarationAcceptanceEnrichment,
        MessageType.IE328,
        SchemaType.CC328A,
        MockStoreConnector.getAcceptanceEnrichment(_, amendment = false),
        MockDeclarationAcceptanceXMLBuilder.buildXML
      )
    }

    "processing a declaration rejection" must {
      behave like decisionProcessing(
        validDeclarationRejectionDecision,
        declarationRejectionEnrichment,
        MessageType.IE316,
        SchemaType.CC316A,
        MockStoreConnector.getDeclarationRejectionEnrichment,
        MockDeclarationRejectionXMLBuilder.buildXML
      )
    }

    "processing a amendment acceptance" must {
      behave like decisionProcessing(
        validAmendmentAcceptanceDecision,
        amendmentAcceptanceEnrichment,
        MessageType.IE304,
        SchemaType.CC304A,
        MockStoreConnector.getAcceptanceEnrichment(_, amendment = true),
        MockAmendmentAcceptanceXMLBuilder.buildXML
      )
    }

    "processing a amendment rejection" must {
      behave like decisionProcessing(
        validAmendmentRejectionDecision,
        amendmentRejectionEnrichment,
        MessageType.IE305,
        SchemaType.CC305A,
        MockStoreConnector.getAmendmentRejectionEnrichment,
        MockAmendmentRejectionXMLBuilder.buildXML
      )
    }
  }

  private def decisionProcessing[R <: DecisionResponse, E <: Enrichment](
    decision: Decision[R],
    enrichment: E,
    messageType: MessageType,
    validationSchemaType: SchemaType,
    enrichmentConnectorMock: String => CallHandler[Future[Either[ErrorCode, E]]],
    xmlBuilderMock: (Decision[R], E) => CallHandler[Elem]): Unit = {
    val acceptance = messageType.isAcceptance

    def setupMocks(validateJsonToXMLTransformation: Boolean, longJourneyTime: FiniteDuration) = {
      MockAppConfig.validateJsonToXMLTransformation returns validateJsonToXMLTransformation
      MockAppConfig.longJourneyTime returns longJourneyTime
      enrichmentConnectorMock(submissionId) returns Future.successful(Right(enrichment))
      xmlBuilderMock(decision, enrichment) returns rawXml
      MockXMLWrapper.wrapXml(correlationId, rawXml) returns wrappedXml
      MockOutcomeConnector.send(validOutcome(messageType, acceptance)) returns Future.successful(Right(()))
      MockStoreConnector.setShortTtl(submissionId) returns Future.successful(true)
    }

    "enrich, build xml and send to outcome" in new Test {
      setupMocks(validateJsonToXMLTransformation = false, longJourneyTimeThreshold)
      MockReportSender.sendReport(EisResponseTime(timeDifference)) returns Future.successful((): Unit)
      service.processDecision(decision).futureValue shouldBe Right(())

      shouldReportMetric()
    }
    "log for long journey times" in new Test {
      setupMocks(validateJsonToXMLTransformation = false, shortJourneyTimeThreshold)
      MockReportSender.sendReport(EisResponseTime(timeDifference)) returns Future.successful((): Unit)
      service.processDecision(decision).futureValue shouldBe Right(())

      shouldReportMetric()
      MockPagerDutyLogger.logLongJourneyTime(journeyTime, shortJourneyTimeThreshold) returns Unit
    }
    "process successfully despite schema validation failing" in new Test {
      setupMocks(validateJsonToXMLTransformation = true, longJourneyTimeThreshold)
      MockReportSender.sendReport(EisResponseTime(timeDifference)) returns Future.successful((): Unit)
      MockSchemaValidator.validateSchema(validationSchemaType, rawXml) returns failedValidationResult
      service.processDecision(decision).futureValue shouldBe Right(())

      shouldReportMetric()
    }

    behave like errorHandling(decision, enrichment, messageType, enrichmentConnectorMock, xmlBuilderMock, acceptance)

    behave like storeHousekeepingUpdating(
      decision,
      enrichment,
      messageType,
      enrichmentConnectorMock,
      xmlBuilderMock,
      acceptance)
  }

  private def errorHandling[E <: Enrichment, R <: DecisionResponse](
    decision: Decision[R],
    enrichment: E,
    messageType: MessageType,
    enrichmentConnectorMock: String => CallHandler[Future[Either[ErrorCode, E]]],
    xmlBuilderMock: (Decision[R], E) => CallHandler[Elem],
    acceptance: Boolean): Unit =
    "return the error code from the connector" when {
      // WLOG
      val someErrorCode = ErrorCode.NoSubmission

      "the outcome cannot be sent" in new Test {
        MockAppConfig.validateJsonToXMLTransformation returns false
        enrichmentConnectorMock(submissionId)
          .returns(Future.successful(Right(enrichment)))

        xmlBuilderMock(decision, enrichment) returns rawXml
        MockXMLWrapper.wrapXml(correlationId, rawXml) returns wrappedXml
        MockOutcomeConnector.send(validOutcome(messageType, acceptance)) returns Future.successful(Left(someErrorCode))

        service.processDecision(decision).futureValue shouldBe Left(someErrorCode)

        shouldNotReportMetric()
      }

      "the enrichment fails" in new Test {
        enrichmentConnectorMock(submissionId)
          .returns(Future.successful(Left(someErrorCode)))

        service.processDecision(decision).futureValue shouldBe Left(someErrorCode)

        shouldNotReportMetric()
      }
    }

  private def storeHousekeepingUpdating[E <: Enrichment, R <: DecisionResponse](
    decision: Decision[R],
    enrichment: E,
    messageType: MessageType,
    enrichmentConnectorMock: String => CallHandler[Future[Either[ErrorCode, E]]],
    xmlBuilderMock: (Decision[R], E) => CallHandler[Elem],
    acceptance: Boolean): Unit = {

    def setupEnrichmentAndXmlBuilderStubs() = {
      MockAppConfig.validateJsonToXMLTransformation returns false
      enrichmentConnectorMock(submissionId) returns Future.successful(Right(enrichment))
      xmlBuilderMock(decision, enrichment) returns rawXml
      MockXMLWrapper.wrapXml(correlationId, rawXml) returns wrappedXml
    }

    "not wait for setShortTtl to finish" in new Test {
      setupEnrichmentAndXmlBuilderStubs()
      MockOutcomeConnector.send(validOutcome(messageType, acceptance)) returns Future.successful(Right(()))
      MockReportSender.sendReport(EisResponseTime(timeDifference)) returns Future.successful((): Unit)
      MockAppConfig.longJourneyTime returns longJourneyTimeThreshold
      MockStoreConnector.setShortTtl(submissionId) returns Promise[Boolean].future

      service.processDecision(decision).futureValue shouldBe Right(())
    }
    "not wait for report sender to finish" in new Test {
      setupEnrichmentAndXmlBuilderStubs()
      MockOutcomeConnector.send(validOutcome(messageType, acceptance)) returns Future.successful(Right(()))
      MockReportSender.sendReport(EisResponseTime(timeDifference)) returns Promise[Unit].future
      MockAppConfig.longJourneyTime returns longJourneyTimeThreshold
      MockStoreConnector.setShortTtl(submissionId) returns Future.successful(true)

      service.processDecision(decision).futureValue shouldBe Right(())
    }

    "not call setShortTTl or isLongJourneyTime on failure" in new Test {
      setupEnrichmentAndXmlBuilderStubs()
      // WLOG
      val someErrorCode: ErrorCode = ErrorCode.NoSubmission
      MockOutcomeConnector.send(validOutcome(messageType, acceptance)) returns Future.successful(Left(someErrorCode))

      val setShortTtlCalled = new AtomicBoolean(false)
      MockStoreConnector
        .setShortTtl(submissionId)
        .onCall { _ =>
          setShortTtlCalled.set(true)
          Future.successful(true)
        }
        .anyNumberOfTimes() // not really but `never()` neither picks up failures nor calls `onCall`.
      val longJourneyTimeCalled = new AtomicBoolean(false)
      MockAppConfig.longJourneyTime
        .onCall { _ =>
          longJourneyTimeCalled.set(true)
          longJourneyTimeThreshold
        }
        .anyNumberOfTimes() // not really but `never()` neither picks up failures nor calls `onCall`.

      service.processDecision(decision).futureValue shouldBe Left(someErrorCode)
      setShortTtlCalled.get                         shouldBe false
      longJourneyTimeCalled.get                     shouldBe false
    }
  }

  private def failedValidationResult =
    new ValidationResult {
      override def isValid = false

      override def allErrors: Seq[SAXParseException] = Seq(new SAXParseException("invalid!", null))
    }
}

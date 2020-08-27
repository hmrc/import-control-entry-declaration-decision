/*
 * Copyright 2020 HM Revenue & Customs
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

import java.time.{Clock, Instant, ZoneOffset}
import java.util.concurrent.atomic.AtomicBoolean

import com.kenshoo.play.metrics.Metrics
import org.scalamock.handlers.CallHandler
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Span}
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
import uk.gov.hmrc.entrydeclarationdecision.utils._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.xml.{Elem, SAXParseException}

class ProcessDecisionServiceSpec
    extends UnitSpec
    with MockAppConfig
    with MockOutcomeConnector
    with MockStoreConnector
    with MockDeclarationAcceptanceXMLBuilder
    with MockAmendmentAcceptanceXMLBuilder
    with MockAmendmentRejectionXMLBuilder
    with MockDeclarationRejectionXMLBuilder
    with MockSchemaValidator
    with MockXMLWrapper
    with ScalaFutures {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(timeout = Span(500, Millis))

  implicit val hc: HeaderCarrier  = HeaderCarrier()
  implicit val lc: LoggingContext = LoggingContext("eori", "corrId", "subId", Some("mrn"))

  val time: Instant          = Instant.now
  val clock: Clock           = Clock.fixed(time, ZoneOffset.UTC)
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
    clock,
    mockedMetrics
  )

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

  private val rawXml              = <rawXml/>
  private val wrappedXml          = <wrapped/>
  private val submissionId        = "sumbissionID"
  private val correlationId       = "15digitCorrelationID"
  private val preparationDateTime = Instant.parse("2020-12-31T23:59:00Z")
  private val receivedDateTime    = Instant.parse("2020-12-31T23:59:00Z")
  private val rejectionDateTime   = Instant.parse("2020-12-31T23:59:00Z")
  private val acceptedDateTime    = Instant.parse("2020-12-31T23:59:00Z")

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

    "enrich, build xml and send to outcome" in {
      MockAppConfig.validateJsonToXMLTransformation returns false
      enrichmentConnectorMock(submissionId) returns Right(enrichment)

      xmlBuilderMock(decision, enrichment) returns rawXml
      MockXMLWrapper.wrapXml(correlationId, rawXml) returns wrappedXml
      MockOutcomeConnector.send(validOutcome(messageType, acceptance)) returns Future.successful(Right(()))
      MockStoreConnector.setShortTtl(submissionId) returns Future.successful(true)

      service.processDecision(decision).futureValue shouldBe Right(())
    }

    "process successfully despite schema validation failing" in {
      MockAppConfig.validateJsonToXMLTransformation returns true
      enrichmentConnectorMock(submissionId) returns Right(enrichment)

      xmlBuilderMock(decision, enrichment) returns rawXml
      MockSchemaValidator.validateSchema(validationSchemaType, rawXml) returns failedValidationResult
      MockXMLWrapper.wrapXml(correlationId, rawXml) returns wrappedXml
      MockOutcomeConnector.send(validOutcome(messageType, acceptance)) returns Future.successful(Right(()))
      MockStoreConnector.setShortTtl(submissionId) returns Future.successful(true)

      service.processDecision(decision).futureValue shouldBe Right(())
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

      "the outcome cannot be sent" in {
        MockAppConfig.validateJsonToXMLTransformation returns false
        enrichmentConnectorMock(submissionId)
          .returns(Future.successful(Right(enrichment)))

        xmlBuilderMock(decision, enrichment) returns rawXml
        MockXMLWrapper.wrapXml(correlationId, rawXml) returns wrappedXml
        MockOutcomeConnector.send(validOutcome(messageType, acceptance)) returns Future.successful(Left(someErrorCode))

        service.processDecision(decision).futureValue shouldBe Left(someErrorCode)
      }

      "the enrichment fails" in {
        enrichmentConnectorMock(submissionId)
          .returns(Future.successful(Left(someErrorCode)))

        service.processDecision(decision).futureValue shouldBe Left(someErrorCode)
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
      enrichmentConnectorMock(submissionId) returns Right(enrichment)

      xmlBuilderMock(decision, enrichment) returns rawXml
      MockXMLWrapper.wrapXml(correlationId, rawXml) returns wrappedXml
    }

    "not wait for setShortTtl to finish" in {
      setupEnrichmentAndXmlBuilderStubs()

      MockOutcomeConnector.send(validOutcome(messageType, acceptance)) returns Future.successful(Right(()))
      MockStoreConnector.setShortTtl(submissionId) returns Promise[Boolean].future

      service.processDecision(decision).futureValue shouldBe Right(())
    }

    "not call setShortTTl on failure" in {
      setupEnrichmentAndXmlBuilderStubs()

      // WLOG
      val someErrorCode = ErrorCode.NoSubmission

      MockOutcomeConnector.send(validOutcome(messageType, acceptance)) returns Future.successful(Left(someErrorCode))

      val called = new AtomicBoolean(false)
      MockStoreConnector
        .setShortTtl(submissionId)
        .onCall { _ =>
          called.set(true)
          Future.successful(true)
        }
        .anyNumberOfTimes() // not really but `never()` neither picks up failures nor calls `onCall`.

      service.processDecision(decision).futureValue shouldBe Left(someErrorCode)

      called.get shouldBe false
    }
  }

  private def failedValidationResult =
    new ValidationResult {
      override def isValid = false

      override def allErrors: Seq[SAXParseException] = Seq(new SAXParseException("invalid!", null))
    }
}

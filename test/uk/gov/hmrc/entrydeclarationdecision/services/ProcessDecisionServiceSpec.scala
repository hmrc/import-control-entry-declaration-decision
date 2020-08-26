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

import com.kenshoo.play.metrics.Metrics
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Span}
import play.api.libs.json.Json
import uk.gov.hmrc.entrydeclarationdecision.config.MockAppConfig
import uk.gov.hmrc.entrydeclarationdecision.connectors.{MockOutcomeConnector, MockStoreConnector}
import uk.gov.hmrc.entrydeclarationdecision.logging.LoggingContext
import uk.gov.hmrc.entrydeclarationdecision.models.ErrorCode
import uk.gov.hmrc.entrydeclarationdecision.models.decision._
import uk.gov.hmrc.entrydeclarationdecision.models.enrichment.acceptance.AcceptanceEnrichment
import uk.gov.hmrc.entrydeclarationdecision.models.enrichment.rejection.{AmendmentRejectionEnrichment, DeclarationRejectionEnrichment}
import uk.gov.hmrc.entrydeclarationdecision.models.outcome.Outcome
import uk.gov.hmrc.entrydeclarationdecision.utils._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.xml.SAXParseException

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

  private def validOutcome(messageType: MessageType) =
    Outcome("eori", correlationId, submissionId, receivedDateTime, messageType, None, wrappedXml.toString)

  private def validOutcomeWithMRN(messageType: MessageType) =
    Outcome(
      "eori",
      correlationId,
      submissionId,
      receivedDateTime,
      messageType,
      Some("02CHPW67QLOYOB4IA8"),
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

  "ProcessDecisionService" should {
    "return a Right" when {
      "a declaration rejection decision is supplied and the outcome successfully saved" in {
        val decision = validDeclarationRejectionDecision

        MockAppConfig.validateJsonToXMLTransformation returns false
        MockStoreConnector
          .getDeclarationRejectionEnrichment(submissionId) returns Right(declarationRejectionEnrichment)

        MockRejectionXMLBuilder.buildXML(decision, declarationRejectionEnrichment) returns rawXml
        MockXMLWrapper.wrapXml(correlationId, rawXml) returns wrappedXml
        MockOutcomeConnector.send(validOutcome(MessageType.IE316)) returns Future.successful(Right(()))
        MockStoreConnector.setShortTtl(submissionId) returns Future.successful(true)

        service.processDecision(decision).futureValue shouldBe Right(())
      }

      "a declaration rejection decision successfully processed despite schema validation failing" in {
        val decision = validDeclarationRejectionDecision

        MockAppConfig.validateJsonToXMLTransformation returns true
        MockStoreConnector
          .getDeclarationRejectionEnrichment(submissionId) returns Right(declarationRejectionEnrichment)

        MockRejectionXMLBuilder.buildXML(decision, declarationRejectionEnrichment) returns rawXml
        MockSchemaValidator.validateSchema(SchemaType.CC316A, rawXml) returns failedValidationResult
        MockXMLWrapper.wrapXml(correlationId, rawXml) returns wrappedXml
        MockOutcomeConnector.send(validOutcome(MessageType.IE316)) returns Future.successful(Right(()))
        MockStoreConnector.setShortTtl(submissionId) returns Future.successful(true)

        service.processDecision(decision).futureValue shouldBe Right(())
      }

      "an declaration acceptance decision is supplied and the outcome successfully saved" in {
        val decision = validDeclarationAcceptanceDecision

        MockAppConfig.validateJsonToXMLTransformation returns false
        MockStoreConnector
          .getAcceptanceEnrichment(submissionId, amendment = false)
          .returns(Future.successful(Right(declarationAcceptanceEnrichment)))

        MockDeclarationAcceptanceXMLBuilder.buildXML(decision, declarationAcceptanceEnrichment) returns rawXml
        MockXMLWrapper.wrapXml(correlationId, rawXml) returns wrappedXml
        MockOutcomeConnector.send(validOutcomeWithMRN(MessageType.IE328)) returns Future.successful(Right(()))
        MockStoreConnector.setShortTtl(submissionId) returns Future.successful(true)

        service.processDecision(decision).futureValue shouldBe Right(())
      }

      "a declaration acceptance decision successfully processed despite schema validation failing" in {
        val decision = validDeclarationAcceptanceDecision

        MockAppConfig.validateJsonToXMLTransformation returns true
        MockStoreConnector
          .getAcceptanceEnrichment(submissionId, amendment = false)
          .returns(Future.successful(Right(declarationAcceptanceEnrichment)))

        MockDeclarationAcceptanceXMLBuilder.buildXML(decision, declarationAcceptanceEnrichment) returns rawXml
        MockSchemaValidator.validateSchema(SchemaType.CC328A, rawXml) returns failedValidationResult
        MockXMLWrapper.wrapXml(correlationId, rawXml) returns wrappedXml
        MockOutcomeConnector.send(validOutcomeWithMRN(MessageType.IE328)) returns Future.successful(Right(()))
        MockStoreConnector.setShortTtl(submissionId) returns Future.successful(true)

        service.processDecision(decision).futureValue shouldBe Right(())
      }

      "an amendment acceptance decision is supplied and the outcome successfully saved" in {
        val decision = validAmendmentAcceptanceDecision

        MockAppConfig.validateJsonToXMLTransformation returns false
        MockStoreConnector
          .getAcceptanceEnrichment(submissionId, amendment = true)
          .returns(Future.successful(Right(amendmentAcceptanceEnrichment)))

        MockAmendmentAcceptanceXMLBuilder.buildXML(decision, amendmentAcceptanceEnrichment) returns rawXml
        MockXMLWrapper.wrapXml(correlationId, rawXml) returns wrappedXml
        MockOutcomeConnector.send(validOutcomeWithMRN(MessageType.IE304)) returns Future.successful(Right(()))
        MockStoreConnector.setShortTtl(submissionId) returns Future.successful(true)

        service.processDecision(decision).futureValue shouldBe Right(())
      }

      "an amendment acceptance decision successfully processed despite schema validation failing" in {
        val decision = validAmendmentAcceptanceDecision

        MockAppConfig.validateJsonToXMLTransformation returns true
        MockStoreConnector
          .getAcceptanceEnrichment(submissionId, amendment = true)
          .returns(Future.successful(Right(amendmentAcceptanceEnrichment)))

        MockAmendmentAcceptanceXMLBuilder.buildXML(decision, amendmentAcceptanceEnrichment) returns rawXml
        MockSchemaValidator.validateSchema(SchemaType.CC304A, rawXml) returns failedValidationResult
        MockXMLWrapper.wrapXml(correlationId, rawXml) returns wrappedXml
        MockOutcomeConnector.send(validOutcomeWithMRN(MessageType.IE304)) returns Future.successful(Right(()))
        MockStoreConnector.setShortTtl(submissionId) returns Future.successful(true)

        service.processDecision(decision).futureValue shouldBe Right(())
      }

      "an amendment rejection decision is supplied and the outcome successfully saved" in {
        val decision = validAmendmentRejectionDecision

        MockAppConfig.validateJsonToXMLTransformation returns false
        MockStoreConnector
          .getAmendmentRejectionEnrichment(submissionId)
          .returns(Future.successful(Right(amendmentRejectionEnrichment)))

        MockAmendmentRejectionXMLBuilder.buildXML(decision, amendmentRejectionEnrichment) returns rawXml
        MockXMLWrapper.wrapXml(correlationId, rawXml) returns wrappedXml
        MockOutcomeConnector.send(validOutcome(MessageType.IE305)) returns Future.successful(Right(()))
        MockStoreConnector.setShortTtl(submissionId) returns Future.successful(true)

        service.processDecision(decision).futureValue shouldBe Right(())
      }

      "an amendment rejection decision successfully processed despite schema validation failing" in {
        val decision = validAmendmentRejectionDecision

        MockAppConfig.validateJsonToXMLTransformation returns true
        MockStoreConnector
          .getAmendmentRejectionEnrichment(submissionId)
          .returns(Future.successful(Right(amendmentRejectionEnrichment)))

        MockAmendmentRejectionXMLBuilder.buildXML(decision, amendmentRejectionEnrichment) returns rawXml
        MockSchemaValidator.validateSchema(SchemaType.CC305A, rawXml) returns failedValidationResult
        MockXMLWrapper.wrapXml(correlationId, rawXml) returns wrappedXml
        MockOutcomeConnector.send(validOutcome(MessageType.IE305)) returns Future.successful(Right(()))
        MockStoreConnector.setShortTtl(submissionId) returns Future.successful(true)

        service.processDecision(decision).futureValue shouldBe Right(())
      }
      "set short TTL does not finish" in {
        //WLOG
        val decision = validAmendmentRejectionDecision

        MockAppConfig.validateJsonToXMLTransformation returns true
        MockStoreConnector
          .getAmendmentRejectionEnrichment(submissionId)
          .returns(Future.successful(Right(amendmentRejectionEnrichment)))

        MockAmendmentRejectionXMLBuilder.buildXML(decision, amendmentRejectionEnrichment) returns rawXml
        MockSchemaValidator.validateSchema(SchemaType.CC305A, rawXml) returns failedValidationResult
        MockXMLWrapper.wrapXml(correlationId, rawXml) returns wrappedXml
        MockOutcomeConnector.send(validOutcome(MessageType.IE305)) returns Future.successful(Right(()))
        MockStoreConnector.setShortTtl(submissionId) returns Promise[Boolean].future

        service.processDecision(decision).futureValue shouldBe Right(())
      }
      "set short TTL fails" in {
        //WLOG
        val decision = validAmendmentRejectionDecision

        MockAppConfig.validateJsonToXMLTransformation returns true
        MockStoreConnector
          .getAmendmentRejectionEnrichment(submissionId)
          .returns(Future.successful(Right(amendmentRejectionEnrichment)))

        MockAmendmentRejectionXMLBuilder.buildXML(decision, amendmentRejectionEnrichment) returns rawXml
        MockSchemaValidator.validateSchema(SchemaType.CC305A, rawXml) returns failedValidationResult
        MockXMLWrapper.wrapXml(correlationId, rawXml) returns wrappedXml
        MockOutcomeConnector.send(validOutcome(MessageType.IE305)) returns Future.successful(Right(()))
        MockStoreConnector.setShortTtl(submissionId) returns Future.successful(false)

        service.processDecision(decision).futureValue shouldBe Right(())
      }
      "set short TTL is called eventually" in {
        //WLOG
        val decision = validAmendmentRejectionDecision

        MockAppConfig.validateJsonToXMLTransformation returns true
        MockStoreConnector
          .getAmendmentRejectionEnrichment(submissionId)
          .returns(Future.successful(Right(amendmentRejectionEnrichment)))

        MockAmendmentRejectionXMLBuilder.buildXML(decision, amendmentRejectionEnrichment) returns rawXml
        MockSchemaValidator.validateSchema(SchemaType.CC305A, rawXml) returns failedValidationResult
        MockXMLWrapper.wrapXml(correlationId, rawXml) returns wrappedXml
        MockOutcomeConnector.send(validOutcome(MessageType.IE305)) returns Future.successful(Right(()))

        val setShortTtlComplete: Promise[Unit] = Promise[Unit]
        MockStoreConnector.setShortTtl(submissionId) returns {
          setShortTtlComplete.success(())
          Future.successful(true)
        }

        service.processDecision(decision).futureValue shouldBe Right(())
        await(setShortTtlComplete)
      }
    }

    "return the error code from the connector" when {
      // WLOG
      val someErrorCode = ErrorCode.NoSubmission

      "the rejection outcome cannot be saved" in {
        val decision = validDeclarationRejectionDecision

        MockAppConfig.validateJsonToXMLTransformation returns false
        MockStoreConnector
          .getDeclarationRejectionEnrichment(submissionId) returns Right(declarationRejectionEnrichment)

        MockRejectionXMLBuilder.buildXML(decision, declarationRejectionEnrichment) returns rawXml
        MockXMLWrapper.wrapXml(correlationId, rawXml) returns wrappedXml
        MockOutcomeConnector.send(validOutcome(MessageType.IE316)) returns Future.successful(Left(someErrorCode))

        service.processDecision(decision).futureValue shouldBe Left(someErrorCode)
      }

      "the acceptance outcome cannot be saved" in {
        val decision = validDeclarationAcceptanceDecision

        MockAppConfig.validateJsonToXMLTransformation returns false
        MockStoreConnector
          .getAcceptanceEnrichment(submissionId, amendment = false)
          .returns(Future.successful(Right(declarationAcceptanceEnrichment)))

        MockDeclarationAcceptanceXMLBuilder.buildXML(decision, declarationAcceptanceEnrichment) returns rawXml
        MockXMLWrapper.wrapXml(correlationId, rawXml) returns wrappedXml
        MockOutcomeConnector.send(validOutcomeWithMRN(MessageType.IE328)) returns Future.successful(Left(someErrorCode))

        service.processDecision(decision).futureValue shouldBe Left(someErrorCode)
      }

      "the acceptance enrichment fails" in {
        MockStoreConnector
          .getAcceptanceEnrichment(submissionId, amendment = false)
          .returns(Future.successful(Left(someErrorCode)))

        service.processDecision(validDeclarationAcceptanceDecision).futureValue shouldBe Left(someErrorCode)
      }
    }
    "not call setShortTTl on failure" in {
      val decision = validDeclarationRejectionDecision

      MockAppConfig.validateJsonToXMLTransformation returns false
      MockStoreConnector
        .getDeclarationRejectionEnrichment(submissionId) returns Right(declarationRejectionEnrichment)

      MockRejectionXMLBuilder.buildXML(decision, declarationRejectionEnrichment) returns rawXml
      MockXMLWrapper.wrapXml(correlationId, rawXml) returns wrappedXml
      //WLOG
      MockOutcomeConnector.send(validOutcome(MessageType.IE316)) returns Future.successful(Left(ErrorCode.NoSubmission))
      MockStoreConnector.setShortTtl(submissionId).never returns true

      service.processDecision(decision).futureValue shouldBe Left(ErrorCode.NoSubmission)

      Thread.sleep(100) // check that MockStoreConnector.setShortTtl(submissionId) isn't called in this time
    }
  }

  private def failedValidationResult =
    new ValidationResult {
      override def isValid = false

      override def allErrors: Seq[SAXParseException] = Seq(new SAXParseException("invalid!", null))
    }
}

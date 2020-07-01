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

import java.time.ZonedDateTime

import com.kenshoo.play.metrics.Metrics
import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Span}
import play.api.libs.json.Json
import uk.gov.hmrc.entrydeclarationdecision.config.MockAppConfig
import uk.gov.hmrc.entrydeclarationdecision.connectors.{OutcomeConnector, StoreConnector}
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
import scala.concurrent.Future
import scala.xml.{Elem, Node, SAXParseException}

class ProcessDecisionServiceSpec
    extends UnitSpec
    with MockAppConfig
    with MockOutcomeConnector
    with MockStoreConnector
    with MockDeclarationAcceptanceXMLBuilder
    with MockAmendmentAcceptanceXMLBuilder
    with MockAmendmentRejectionXMLBuilder
    with MockRejectionXMLBuilder
    with MockSchemaValidator
    with MockXMLWrapper
    with ScalaFutures {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(timeout = Span(500, Millis))

  implicit val hc: HeaderCarrier  = HeaderCarrier()
  implicit val lc: LoggingContext = LoggingContext("eori", "corrId", "subId", Some("mrn"))

  val mockedMetrics: Metrics = new MockMetrics

  private val service = new ProcessDecisionService(
    mockAppConfig,
    mockOutcomeConnector,
    mockStoreConnector,
    mockDeclarationAcceptanceXMLBuilder,
    mockRejectionXMLBuilder,
    mockAmendmentAcceptanceXMLBuilder,
    mockAmendmentRejectionXMLBuilder,
    mockSchemaValidator,
    mockXMLWrapper,
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

  private val rawXml              = <rawXml/>
  private val wrappedXml          = <wrapped/>
  private val submissionId        = "sumbissionID"
  private val correlationId       = "15digitCorrelationID"
  private val preparationDateTime = ZonedDateTime.parse("2020-12-31T23:59:00Z")
  private val receivedDateTime    = ZonedDateTime.parse("2020-12-31T23:59:00Z")
  private val rejectionDateTime   = ZonedDateTime.parse("2020-12-31T23:59:00Z")
  private val acceptedDateTime    = ZonedDateTime.parse("2020-12-31T23:59:00Z")

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
        MockRejectionXMLBuilder.buildXML(decision, DeclarationRejectionEnrichment) returns rawXml
        MockXMLWrapper.wrapXml(correlationId, rawXml) returns wrappedXml
        MockOutcomeConnector.send(validOutcome(MessageType.IE316)) returns Future.successful(Right(()))

        service.processDecision(decision).futureValue shouldBe Right(())
      }

      "a declaration rejection decision successfully processed despite schema validation failing" in {
        val decision = validDeclarationRejectionDecision

        MockAppConfig.validateJsonToXMLTransformation returns true
        MockRejectionXMLBuilder.buildXML(decision, DeclarationRejectionEnrichment) returns rawXml
        MockSchemaValidator.validateSchema(SchemaType.CC316A, rawXml) returns failedValidationResult
        MockXMLWrapper.wrapXml(correlationId, rawXml) returns wrappedXml
        MockOutcomeConnector.send(validOutcome(MessageType.IE316)) returns Future.successful(Right(()))

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

        service.processDecision(decision).futureValue shouldBe Right(())
      }
    }

    "return the error code from the connector" when {
      // WLOG
      val someErrorCode = ErrorCode.DuplicateSubmission

      "the rejection outcome cannot be saved" in {
        val decision = validDeclarationRejectionDecision

        MockAppConfig.validateJsonToXMLTransformation returns false
        MockRejectionXMLBuilder.buildXML(decision, DeclarationRejectionEnrichment) returns rawXml
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
  }

  private def failedValidationResult =
    new ValidationResult {
      override def isValid = false

      override def allErrors: Seq[SAXParseException] = Seq(new SAXParseException("invalid!", null))
    }
}

trait MockOutcomeConnector extends MockFactory {
  val mockOutcomeConnector: OutcomeConnector = mock[OutcomeConnector]

  object MockOutcomeConnector {
    def send(outcome: Outcome): CallHandler[Future[Either[ErrorCode, Unit]]] =
      (mockOutcomeConnector.send(_: Outcome)(_: HeaderCarrier, _: LoggingContext)).expects(outcome, *, *)
  }

}

trait MockStoreConnector extends MockFactory {
  val mockStoreConnector: StoreConnector = mock[StoreConnector]

  object MockStoreConnector {
    def getAcceptanceEnrichment(
      submissionId: String,
      amendment: Boolean): CallHandler[Future[Either[ErrorCode, AcceptanceEnrichment]]] =
      (mockStoreConnector
        .getAcceptanceEnrichment(_: String, _: Boolean)(_: HeaderCarrier, _: LoggingContext))
        .expects(submissionId, amendment, *, *)

    def getAmendmentRejectionEnrichment(
      submissionId: String): CallHandler[Future[Either[ErrorCode, AmendmentRejectionEnrichment]]] =
      (mockStoreConnector
        .getAmendmentRejectionEnrichment(_: String)(_: HeaderCarrier, _: LoggingContext))
        .expects(submissionId, *, *)
  }

}

trait MockXMLWrapper extends MockFactory {
  val mockXMLWrapper: XMLWrapper = mock[XMLWrapper]

  object MockXMLWrapper {
    def wrapXml(correlationId: String, xml: Elem): CallHandler[Elem] =
      (mockXMLWrapper.wrapXml(_: String, _: Elem)).expects(correlationId, xml)
  }
}

trait MockRejectionXMLBuilder extends MockFactory {
  val mockRejectionXMLBuilder: DeclarationRejectionXMLBuilder = mock[DeclarationRejectionXMLBuilder]

  object MockRejectionXMLBuilder {
    def buildXML(
      decision: Decision[DecisionResponse.Rejection],
      enrichment: DeclarationRejectionEnrichment.type): CallHandler[Elem] =
      (mockRejectionXMLBuilder
        .buildXML(_: Decision[DecisionResponse.Rejection], _: DeclarationRejectionEnrichment.type))
        .expects(decision, enrichment)
  }

}

trait MockDeclarationAcceptanceXMLBuilder extends MockFactory {
  val mockDeclarationAcceptanceXMLBuilder: DeclarationAcceptanceXMLBuilder = mock[DeclarationAcceptanceXMLBuilder]

  object MockDeclarationAcceptanceXMLBuilder {
    def buildXML(decision: Decision[DecisionResponse.Acceptance], enrichment: AcceptanceEnrichment): CallHandler[Elem] =
      (mockDeclarationAcceptanceXMLBuilder
        .buildXML(_: Decision[DecisionResponse.Acceptance], _: AcceptanceEnrichment))
        .expects(decision, enrichment)
  }

}

trait MockAmendmentAcceptanceXMLBuilder extends MockFactory {
  val mockAmendmentAcceptanceXMLBuilder: AmendmentAcceptanceXMLBuilder = mock[AmendmentAcceptanceXMLBuilder]

  object MockAmendmentAcceptanceXMLBuilder {
    def buildXML(decision: Decision[DecisionResponse.Acceptance], enrichment: AcceptanceEnrichment): CallHandler[Elem] =
      (mockAmendmentAcceptanceXMLBuilder
        .buildXML(_: Decision[DecisionResponse.Acceptance], _: AcceptanceEnrichment))
        .expects(decision, enrichment)
  }

}

trait MockAmendmentRejectionXMLBuilder extends MockFactory {
  val mockAmendmentRejectionXMLBuilder: AmendmentRejectionXMLBuilder = mock[AmendmentRejectionXMLBuilder]

  object MockAmendmentRejectionXMLBuilder {
    def buildXML(
      decision: Decision[DecisionResponse.Rejection],
      enrichment: AmendmentRejectionEnrichment): CallHandler[Elem] =
      (mockAmendmentRejectionXMLBuilder
        .buildXML(_: Decision[DecisionResponse.Rejection], _: AmendmentRejectionEnrichment))
        .expects(decision, enrichment)
  }

}

trait MockSchemaValidator extends MockFactory {
  val mockSchemaValidator: SchemaValidator = mock[SchemaValidator]

  object MockSchemaValidator {
    def validateSchema(schemaType: SchemaType, xml: Node): CallHandler[ValidationResult] =
      (mockSchemaValidator.validateSchema(_: SchemaType, _: Node)).expects(schemaType, xml)
  }

}

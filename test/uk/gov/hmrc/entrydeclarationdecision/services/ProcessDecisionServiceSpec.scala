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
import uk.gov.hmrc.entrydeclarationdecision.models.ErrorCode
import uk.gov.hmrc.entrydeclarationdecision.models.decision._
import uk.gov.hmrc.entrydeclarationdecision.models.enrichment.acceptance.AcceptanceEnrichment
import uk.gov.hmrc.entrydeclarationdecision.models.enrichment.rejection.{AmendmentRejectionEnrichment, DeclarationRejectionEnrichment}
import uk.gov.hmrc.entrydeclarationdecision.models.outcome.Outcome
import uk.gov.hmrc.entrydeclarationdecision.utils._
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
    with ScalaFutures {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(timeout = Span(500, Millis))

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

  private val submissionId        = "sumbissionID"
  private val preparationDateTime = ZonedDateTime.parse("2020-12-31T23:59:00Z")
  private val receivedDateTime    = ZonedDateTime.parse("2020-12-31T23:59:00Z")
  private val rejectionDateTime   = ZonedDateTime.parse("2020-12-31T23:59:00Z")
  private def validOutcome(messageType: MessageType) =
    Outcome("eori", "15digitCorrelationID", "sumbissionID", receivedDateTime, messageType, None, "<sample>hi</sample>")

  private def validOutcomeWithMRN(messageType: MessageType) =
    Outcome(
      "eori",
      "15digitCorrelationID",
      "sumbissionID",
      receivedDateTime,
      messageType,
      Some("02CHPW67QLOYOB4IA8"),
      "<sample>hi</sample>")

  private val validDeclarationRejectionDecision = Decision(
    submissionId,
    DecisionMetadata(
      "eori",
      "pariat",
      MessageType.IE316,
      "msgId",
      preparationDateTime,
      receivedDateTime,
      "15digitCorrelationID",
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
      "15digitCorrelationID",
      None),
    DecisionResponse.Acceptance("02CHPW67QLOYOB4IA8")
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
      "15digitCorrelationID",
      None),
    DecisionResponse.Acceptance("02CHPW67QLOYOB4IA8")
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
      "15digitCorrelationID",
      None),
    DecisionResponse.Rejection(List(DecisionError("cheese", "nisi cupidatat", None, None)), rejectionDateTime)
  )

  "ProcessDecisionService" should {
    "return a Right" when {
      "a declaration rejection decision is supplied and the outcome successfully saved" in {
        MockAppConfig.validateJsonToXMLTransformation returns false
        MockRejectionXMLBuilder
          .buildXML(validDeclarationRejectionDecision, DeclarationRejectionEnrichment)
          .returns(<sample>hi</sample>)
        MockOutcomeConnector.send(validOutcome(MessageType.IE316)).returns(Future.successful(Right(())))
        service.processDecision(validDeclarationRejectionDecision).futureValue shouldBe Right(())
      }

      "a declaration rejection decision successfully processed despite schema validation failing" in {
        MockAppConfig.validateJsonToXMLTransformation returns true

        val xml = <sample>hi</sample>
        MockRejectionXMLBuilder.buildXML(validDeclarationRejectionDecision, DeclarationRejectionEnrichment).returns(xml)

        MockSchemaValidator.validateSchema(SchemaType.CC316A, xml) returns failedValidationResult

        MockOutcomeConnector.send(validOutcome(MessageType.IE316)).returns(Future.successful(Right(())))
        service.processDecision(validDeclarationRejectionDecision).futureValue shouldBe Right(())
      }

      "an declaration acceptance decision is supplied and the outcome successfully saved" in {
        MockAppConfig.validateJsonToXMLTransformation returns false
        MockStoreConnector.getAcceptanceEnrichment(submissionId, amendment = false) returns Future.successful(
          Right(declarationAcceptanceEnrichment))

        MockDeclarationAcceptanceXMLBuilder
          .buildXML(validDeclarationAcceptanceDecision, declarationAcceptanceEnrichment)
          .returns(<sample>hi</sample>)
        MockOutcomeConnector.send(validOutcomeWithMRN(MessageType.IE328)).returns(Future.successful(Right(())))
        service.processDecision(validDeclarationAcceptanceDecision).futureValue shouldBe Right(())
      }

      "a declaration acceptance decision successfully processed despite schema validation failing" in {
        MockAppConfig.validateJsonToXMLTransformation returns true
        MockStoreConnector.getAcceptanceEnrichment(submissionId, amendment = false) returns Future.successful(
          Right(declarationAcceptanceEnrichment))

        val xml = <sample>hi</sample>
        MockDeclarationAcceptanceXMLBuilder
          .buildXML(validDeclarationAcceptanceDecision, declarationAcceptanceEnrichment)
          .returns(xml)

        MockSchemaValidator.validateSchema(SchemaType.CC328A, xml) returns failedValidationResult

        MockOutcomeConnector.send(validOutcomeWithMRN(MessageType.IE328)).returns(Future.successful(Right(())))
        service.processDecision(validDeclarationAcceptanceDecision).futureValue shouldBe Right(())
      }

      "an amendment acceptance decision is supplied and the outcome successfully saved" in {
        MockAppConfig.validateJsonToXMLTransformation returns false
        MockStoreConnector.getAcceptanceEnrichment(submissionId, amendment = true) returns Future.successful(
          Right(amendmentAcceptanceEnrichment))

        MockAmendmentAcceptanceXMLBuilder
          .buildXML(validAmendmentAcceptanceDecision, amendmentAcceptanceEnrichment)
          .returns(<sample>hi</sample>)
        MockOutcomeConnector.send(validOutcomeWithMRN(MessageType.IE304)).returns(Future.successful(Right(())))
        service.processDecision(validAmendmentAcceptanceDecision).futureValue shouldBe Right(())
      }

      "an amendment acceptance decision successfully processed despite schema validation failing" in {
        MockAppConfig.validateJsonToXMLTransformation returns true
        MockStoreConnector.getAcceptanceEnrichment(submissionId, amendment = true) returns Future.successful(
          Right(amendmentAcceptanceEnrichment))

        val xml = <sample>hi</sample>
        MockAmendmentAcceptanceXMLBuilder
          .buildXML(validAmendmentAcceptanceDecision, amendmentAcceptanceEnrichment)
          .returns(xml)

        MockSchemaValidator.validateSchema(SchemaType.CC304A, xml) returns failedValidationResult

        MockOutcomeConnector.send(validOutcomeWithMRN(MessageType.IE304)).returns(Future.successful(Right(())))
        service.processDecision(validAmendmentAcceptanceDecision).futureValue shouldBe Right(())
      }

      "an amendment rejection decision is supplied and the outcome successfully saved" in {
        MockAppConfig.validateJsonToXMLTransformation returns false
        MockStoreConnector.getAmendmentRejectionEnrichment(submissionId) returns Future.successful(
          Right(amendmentRejectionEnrichment))

        MockAmendmentRejectionXMLBuilder
          .buildXML(validAmendmentRejectionDecision, amendmentRejectionEnrichment)
          .returns(<sample>hi</sample>)
        MockOutcomeConnector.send(validOutcome(MessageType.IE305)).returns(Future.successful(Right(())))
        service.processDecision(validAmendmentRejectionDecision).futureValue shouldBe Right(())
      }

      "an amendment rejection decision successfully processed despite schema validation failing" in {
        MockAppConfig.validateJsonToXMLTransformation returns true
        MockStoreConnector.getAmendmentRejectionEnrichment(submissionId) returns Future.successful(
          Right(amendmentRejectionEnrichment))

        val xml = <sample>hi</sample>
        MockAmendmentRejectionXMLBuilder
          .buildXML(validAmendmentRejectionDecision, amendmentRejectionEnrichment)
          .returns(xml)

        MockSchemaValidator.validateSchema(SchemaType.CC305A, xml) returns failedValidationResult

        MockOutcomeConnector.send(validOutcome(MessageType.IE305)).returns(Future.successful(Right(())))
        service.processDecision(validAmendmentRejectionDecision).futureValue shouldBe Right(())
      }
    }

    "return the error code from the connector" when {
      // WLOG
      val someErrorCode = ErrorCode.DuplicateSubmission

      "the rejection outcome cannot be saved" in {
        MockAppConfig.validateJsonToXMLTransformation returns false
        MockRejectionXMLBuilder
          .buildXML(validDeclarationRejectionDecision, DeclarationRejectionEnrichment)
          .returns(<sample>hi</sample>)
        MockOutcomeConnector.send(validOutcome(MessageType.IE316)).returns(Future.successful(Left(someErrorCode)))
        service.processDecision(validDeclarationRejectionDecision).futureValue shouldBe Left(someErrorCode)
      }

      "the acceptance outcome cannot be saved" in {
        MockAppConfig.validateJsonToXMLTransformation returns false
        MockStoreConnector.getAcceptanceEnrichment(submissionId, amendment = false) returns Future.successful(
          Right(declarationAcceptanceEnrichment))

        MockDeclarationAcceptanceXMLBuilder
          .buildXML(validDeclarationAcceptanceDecision, declarationAcceptanceEnrichment)
          .returns(<sample>hi</sample>)
        MockOutcomeConnector
          .send(validOutcomeWithMRN(MessageType.IE328))
          .returns(Future.successful(Left(someErrorCode)))
        service.processDecision(validDeclarationAcceptanceDecision).futureValue shouldBe Left(someErrorCode)
      }

      "the acceptance enrichment fails" in {
        MockStoreConnector.getAcceptanceEnrichment(submissionId, amendment = false) returns Future.successful(
          Left(someErrorCode))

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
      (mockOutcomeConnector.send(_: Outcome)).expects(outcome)
  }

}

trait MockStoreConnector extends MockFactory {
  val mockStoreConnector: StoreConnector = mock[StoreConnector]

  object MockStoreConnector {
    def getAcceptanceEnrichment(
      submissionId: String,
      amendment: Boolean): CallHandler[Future[Either[ErrorCode, AcceptanceEnrichment]]] =
      (mockStoreConnector.getAcceptanceEnrichment _).expects(submissionId, amendment)

    def getAmendmentRejectionEnrichment(
      submissionId: String): CallHandler[Future[Either[ErrorCode, AmendmentRejectionEnrichment]]] =
      (mockStoreConnector.getAmendmentRejectionEnrichment _).expects(submissionId)
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

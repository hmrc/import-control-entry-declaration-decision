/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.entrydeclarationdecision.controllers

import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.{HeaderNames, Status}
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers.{status, _}
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.entrydeclarationdecision.config.MockAppConfig
import uk.gov.hmrc.entrydeclarationdecision.logging.LoggingContext
import uk.gov.hmrc.entrydeclarationdecision.models.decision.DecisionResponse.{Acceptance, Rejection}
import uk.gov.hmrc.entrydeclarationdecision.models.decision.{Decision, DecisionResponse, MessageType}
import uk.gov.hmrc.entrydeclarationdecision.models.{ErrorCode, ErrorResponse}
import uk.gov.hmrc.entrydeclarationdecision.reporting.{DecisionReceived, MockReportSender, ResultSummary}
import uk.gov.hmrc.entrydeclarationdecision.services.ProcessDecisionService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DecisionReceiverControllerSpec
    extends WordSpec
    with Matchers
    with GuiceOneAppPerSuite
    with MockProcessDecisionService
    with MockAppConfig
    with MockReportSender {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val bearerToken = "bearerToken"
  private val fakeRequest = FakeRequest().withHeaders(HeaderNames.AUTHORIZATION -> s"Bearer $bearerToken")

  private val controller =
    new DecisionReceiverController(
      mockAppConfig,
      Helpers.stubControllerComponents(),
      mockProcessDecisionService,
      mockReportSender)

  private val eori          = "eori"
  private val correlationid = "correlationid"
  private val submissionId  = "submissionId"
  private val movementReferenceNumber= "12345678"

  private def decisionReceivedReport(
    validDecision: JsValue,
    messageType: MessageType,
    resultSummary: ResultSummary,
    failure: Option[ErrorCode],
    mrn: Option[String]) =
    DecisionReceived(
      eori          = eori,
      correlationId = correlationid,
      submissionId  = submissionId,
      messageType,
      validDecision,
      resultSummary,
      failure,
      mrn)

  val validAcceptance: JsValue = Json.parse(s"""{
                                               |  "submissionId": "$submissionId",
                                               |  "metadata": {
                                               |    "senderEORI": "$eori",
                                               |    "senderBranch": "pariat",
                                               |    "preparationDateTime": "2020-12-31T23:59:00Z",
                                               |    "messageType": "IE328",
                                               |    "messageIdentification": "msgId",
                                               |    "correlationId": "$correlationid",
                                               |    "receivedDateTime": "2020-12-31T23:59:00Z"
                                               |  },
                                               |  "response": {
                                               |    "movementReferenceNumber": "$movementReferenceNumber",
                                               |    "acceptedDateTime": "2005-03-15T12:41:00Z"
                                               |  }
                                               |}""".stripMargin)

  val validRejection: JsValue = Json.parse(s"""{
                                              |  "submissionId": "$submissionId",
                                              |  "metadata": {
                                              |    "senderEORI": "$eori",
                                              |    "senderBranch": "pariat",
                                              |    "preparationDateTime": "2020-12-31T23:59:00Z",
                                              |    "messageType": "IE305",
                                              |    "messageIdentification": "msgId",
                                              |    "correlationId": "$correlationid",
                                              |    "receivedDateTime": "2020-12-31T23:59:00Z"
                                              |  },
                                              |  "response": {
                                              |    "functionalError": [
                                              |      {
                                              |        "errorType": "cheese",
                                              |        "errorPointer": "nisi cupidatat"
                                              |      }
                                              |    ],
                                              |    "rejectionDateTime": "2020-12-31T23:59:00Z"
                                              |  }
                                              |}""".stripMargin)

  "Post a valid acceptance message" should {
    "return a 201 Created " in {
      val request = fakeRequest.withBody(validAcceptance)
      MockAppConfig.eisInboundBearerToken.returns(bearerToken)
      MockAppConfig.validateIncomingJson.returns(false)
      MockProcessDecisionService
        .processDecision(validAcceptance.as[Decision[Acceptance]])
        .returns(Future.successful(Right(())))
      MockReportSender.sendReport(
        decisionReceivedReport(validAcceptance, MessageType.IE328, ResultSummary.Accepted, None, Some(movementReferenceNumber)))

      val result = controller.handlePost(request)

      status(result) shouldBe Status.CREATED
    }
  }

  "Post a valid rejection message" should {
    "return a 201 Created " in {
      val request = fakeRequest.withBody(validRejection)
      MockAppConfig.eisInboundBearerToken.returns(bearerToken)
      MockAppConfig.validateIncomingJson.returns(false)
      MockProcessDecisionService
        .processDecision(validRejection.as[Decision[Rejection]])
        .returns(Future.successful(Right(())))
      MockReportSender.sendReport(
        decisionReceivedReport(validRejection, MessageType.IE305, ResultSummary.Rejected(1), None, None))

      val result = controller.handlePost(request)

      status(result) shouldBe Status.CREATED
    }
  }

  "Post a decision that cannot be processed" should {
    "convert error codes from the service to the appropriate responses" when {
      def run(errorCode: ErrorCode, expectedStatus: Int, expectedBody: JsValue): Unit =
        s"a $errorCode error is returned from the service" in {
          val request = fakeRequest.withBody(validRejection)
          MockAppConfig.eisInboundBearerToken.returns(bearerToken)
          MockAppConfig.validateIncomingJson.returns(false)
          MockProcessDecisionService
            .processDecision(validRejection.as[Decision[Rejection]])
            .returns(Future.successful(Left(errorCode)))

          MockReportSender.sendReport(
            decisionReceivedReport(validRejection, MessageType.IE305, ResultSummary.Rejected(1), Some(errorCode), None))

          val result = controller.handlePost(request)

          status(result)        shouldBe expectedStatus
          contentAsJson(result) shouldBe expectedBody
        }

      val input = Seq(
        (ErrorCode.ConnectorError, SERVICE_UNAVAILABLE, Json.toJson(ErrorResponse.unavailable)),
        (ErrorCode.NoSubmission, BAD_REQUEST, Json.toJson(ErrorResponse.noSubmission))
      )

      input.foreach(args => (run _).tupled(args))
    }
  }

  "Post a valid message when schema validation is enabled" must {
    "return a 201 Created" in {
      val validSubmissionId  = "1234567890-1234567890-1234567890-123"
      val validCorrelationId = "1234567890-123"
      val decision           = Json.parse(s"""{
                                   |  "submissionId": "$validSubmissionId",
                                   |  "metadata": {
                                   |    "senderEORI": "$eori",
                                   |    "senderBranch": "branch",
                                   |    "preparationDateTime": "2020-02-01T13:10:10.000Z",
                                   |    "messageType": "IE316",
                                   |    "receivedDateTime": "2020-02-01T13:10:10.000Z",
                                   |    "messageIdentification": "Rejectione",
                                   |    "correlationId": "$validCorrelationId",
                                   |    "localReferenceNumber": "01/Failure"
                                   |  },
                                   |  "response": {
                                   |    "rejectionDateTime": "2020-02-01T13:10:10.000Z",
                                   |    "functionalError": [
                                   |      {
                                   |        "errorType": "XX",
                                   |        "errorPointer": "This is a bad submission",
                                   |        "errorReason": "Bad",
                                   |        "originalAttributeValue": "XXXXX"
                                   |      }
                                   |    ]
                                   |  }
                                   |}""".stripMargin)

      val request = fakeRequest.withBody(decision)
      MockAppConfig.eisInboundBearerToken.returns(bearerToken)
      MockAppConfig.validateIncomingJson.returns(true)
      MockProcessDecisionService.processDecision(decision.as[Decision[Rejection]]).returns(Future.successful(Right(())))
      MockReportSender.sendReport(
        DecisionReceived(
          eori,
          validCorrelationId,
          validSubmissionId,
          MessageType.IE316,
          decision,
          ResultSummary.Rejected(1),
          None,
          None))

      val result = controller.handlePost(request)

      status(result) shouldBe Status.CREATED
    }
  }

  "Post an invalid message" should {
    "return 400 Bad Request when reject message type does have reject body" in {
      val request = fakeRequest.withBody(Json.parse(s"""
                                                       |{
                                                       |  "submissionId": "$submissionId",
                                                       |  "metadata": {
                                                       |    "senderEORI": "volup",
                                                       |    "senderBranch": "pariat",
                                                       |    "preparationDateTime": "2020-12-31T23:59:00.000Z",
                                                       |    "messageType": "IE305",
                                                       |    "messageIdentification": "msgId",
                                                       |    "correlationId": "$correlationid",
                                                       |    "receivedDateTime": "2020-12-31T23:59:00.000Z"
                                                       |  },
                                                       |  "response": {
                                                       |   "movementReferenceNumber": "oops!",
                                                       |   "acceptedDateTime": "2005-03-15T12:41:00Z"
                                                       |  }
                                                       |}
                                                       |""".stripMargin))
      MockAppConfig.eisInboundBearerToken.returns(bearerToken)
      MockAppConfig.validateIncomingJson.returns(false)
      val result = controller.handlePost(request)

      status(result)        shouldBe Status.BAD_REQUEST
      contentAsJson(result) shouldBe Json.toJson(ErrorResponse.errorMutualExclusive)
    }

    "return 400 Bad Request when accept message type does have accept body" in {
      val request = fakeRequest.withBody(Json.parse(s"""
                                                       |{
                                                       |  "submissionId": "$submissionId",
                                                       |  "metadata": {
                                                       |    "senderEORI": "volup",
                                                       |    "senderBranch": "pariat",
                                                       |    "preparationDateTime": "2020-12-31T23:59:00.000Z",
                                                       |    "messageType": "IE328",
                                                       |    "messageIdentification": "msgId",
                                                       |    "correlationId": "$correlationid",
                                                       |    "receivedDateTime": "2020-12-31T23:59:00.000Z"
                                                       |  },
                                                       |  "response": {
                                                       |    "functionalError": [
                                                       |      {
                                                       |        "errorType": "too long",
                                                       |        "errorPointer": "nisi cupidatat"
                                                       |      }
                                                       |    ],
                                                       |    "rejectionDateTime": "2020-12-31T23:59:00.000Z"
                                                       |  }
                                                       |}
                                                       |""".stripMargin))
      MockAppConfig.eisInboundBearerToken.returns(bearerToken)
      MockAppConfig.validateIncomingJson.returns(false)
      val result = controller.handlePost(request)

      status(result)        shouldBe Status.BAD_REQUEST
      contentAsJson(result) shouldBe Json.toJson(ErrorResponse.errorMutualExclusive)
    }

    "return a 400 Bad Request when schema validation is enabled" in {
      val request = fakeRequest.withBody(Json.parse(s"""
                                                       |{
                                                       |  "submissionId": "$submissionId",
                                                       |  "metadata": {
                                                       |    "senderEORI": "volup",
                                                       |    "senderBranch": "pariat",
                                                       |    "preparationDateTime": "2020-12-31T23:59:00.000Z",
                                                       |    "messageType": "IE305",
                                                       |    "messageIdentification": "msgId",
                                                       |    "correlationId": "$correlationid",
                                                       |    "receivedDateTime": "2020-12-31T23:59:00.000Z"
                                                       |  },
                                                       |  "response": {
                                                       |    "functionalError": [
                                                       |      {
                                                       |        "errorType": "too long",
                                                       |        "errorPointer": "nisi cupidatat"
                                                       |      }
                                                       |    ],
                                                       |    "rejectionDateTime": "2020-12-31T23:59:00.000Z"
                                                       |  }
                                                       |}
                                                       |""".stripMargin))
      MockAppConfig.eisInboundBearerToken.returns(bearerToken)
      MockAppConfig.validateIncomingJson.returns(true)
      val result = controller.handlePost(request)

      status(result)        shouldBe Status.BAD_REQUEST
      contentAsJson(result) shouldBe Json.toJson(ErrorResponse.errorSchema)
    }

    "return 400 Bad Request for un-parsable json" in {
      val decision = Json.parse(s"""
                                   |{
                                   |  "submissionId": "$submissionId",
                                   |  "metadata": {
                                   |    "senderEORI": "volup",
                                   |    "senderBranch": "pariat",
                                   |    "preparationDateTime": "2020-12-31T23:59:00.000Z",
                                   |    "messageType": "IE305",
                                   |    "messageIdentification": "msgId",
                                   |    "correlationId": "$correlationid",
                                   |    "receivedDateTime": "2020-12-31T23:59:00.000Z"
                                   |  },
                                   |  "response": "beans"
                                   |}
                                   |""".stripMargin)

      MockAppConfig.eisInboundBearerToken.returns(bearerToken)
      val request = fakeRequest.withBody(decision)
      val result  = controller.handlePost(request)

      status(result)        shouldBe Status.BAD_REQUEST
      contentAsJson(result) shouldBe Json.toJson(ErrorResponse.errorParse)
    }
    "return 403" when {
      "no authentication fails" in {
        MockAppConfig.eisInboundBearerToken returns "XXXX"

        val request = fakeRequest.withBody(validAcceptance)
        val result  = controller.handlePost(request)

        status(result) shouldBe FORBIDDEN
      }
    }
  }
}

trait MockProcessDecisionService extends MockFactory {
  val mockProcessDecisionService: ProcessDecisionService = mock[ProcessDecisionService]

  object MockProcessDecisionService {
    def processDecision[R <: DecisionResponse](decision: Decision[R]): CallHandler[Future[Either[ErrorCode, Unit]]] =
      (mockProcessDecisionService
        .processDecision(_: Decision[R])(_: HeaderCarrier, _: LoggingContext))
        .expects(decision, *, *)
  }

}

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

package uk.gov.hmrc.entrydeclarationdecision.reporting

import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsObject, JsString, Json}
import uk.gov.hmrc.entrydeclarationdecision.models.ErrorCode
import uk.gov.hmrc.entrydeclarationdecision.models.decision.MessageType

import java.time.{Clock, Instant, ZoneOffset}

class DecisionReceivedSpec extends PlaySpec {

  val now: Instant = Instant.now
  val clock: Clock = Clock.fixed(now, ZoneOffset.UTC)

  def report(resultSummary: ResultSummary, failure: Option[ErrorCode], mrn: Option[String]): DecisionReceived = DecisionReceived(
    eori          = "eori",
    correlationId = "correlationId",
    submissionId  = "submissionId",
    messageType   = if (resultSummary == ResultSummary.Accepted) MessageType.IE328 else MessageType.IE316,
    body          = JsObject(Seq("body1" -> JsString("value"))),
    resultSummary = resultSummary,
    failure       = failure,
    mrn           = mrn
  )

  "DecisionReceived" must {
    "have the correct associated JSON event" when {
      "accepted" in {
        val event = implicitly[EventSources[DecisionReceived]].eventFor(clock, report(ResultSummary.Accepted, None, mrn = Some("00GB12345678912340"))).get

        Json.toJson(event) shouldBe
          Json.parse(s"""
                        |{
                        |    "eventCode" : "ENS_RESP",
                        |    "eventTimestamp" : "${now.toString}",
                        |    "submissionId" : "submissionId",
                        |    "eori" : "eori",
                        |    "correlationId" : "correlationId",
                        |    "messageType" : "IE328",
                        |    "detail": {
                        |       "summary" : {
                        |         "type": "ACCEPTED"
                        |       },
                        |       "decisionMrn" : "00GB12345678912340"
                        |    }
                        |}
                        |""".stripMargin)
      }

      "rejected" in {
        val event =
          implicitly[EventSources[DecisionReceived]].eventFor(clock, report(ResultSummary.Rejected(123), None, None)).get

        Json.toJson(event) shouldBe
          Json.parse(s"""
                        |{
                        |    "eventCode" : "ENS_RESP",
                        |    "eventTimestamp" : "${now.toString}",
                        |    "submissionId" : "submissionId",
                        |    "eori" : "eori",
                        |    "correlationId" : "correlationId",
                        |    "messageType" : "IE316",
                        |    "detail": {
                        |       "summary" : {
                        |         "type": "REJECTED",
                        |         "errorCount": 123
                        |       }
                        |    }
                        |}
                        |""".stripMargin)
      }

      "processing errors occur" in {
        val event = implicitly[EventSources[DecisionReceived]]
          .eventFor(clock, report(ResultSummary.Accepted, Some(ErrorCode.NoSubmission), Some("00GB12345678912340")))
          .get

        Json.toJson(event) shouldBe
          Json.parse(s"""
                        |{
                        |    "eventCode" : "ENS_RESP",
                        |    "eventTimestamp" : "${now.toString}",
                        |    "submissionId" : "submissionId",
                        |    "eori" : "eori",
                        |    "correlationId" : "correlationId",
                        |    "messageType" : "IE328",
                        |    "detail": {
                        |       "summary" : {
                        |         "type": "ACCEPTED"
                        |       },
                        |       "failure": {
                        |         "type": "NO_SUBMISSION"
                        |       },
                        |       "decisionMrn" : "00GB12345678912340"
                        |    }
                        |}
                        |""".stripMargin)
      }
    }

    "have the correct associated audit event" in {
      val event = implicitly[EventSources[DecisionReceived]].auditEventFor(report(ResultSummary.Accepted, None, None)).get

      event.auditType       shouldBe "DecisionReceived"
      event.transactionName shouldBe "ENS decision received from EIS"

      Json.toJson(event.detail) shouldBe
        Json.parse("""
                     |{
                     |    "eori" : "eori",
                     |    "correlationId": "correlationId",
                     |    "decisionBody": {
                     |      "body1": "value"
                     |    }
                     |}
                     |""".stripMargin)
    }
  }
}

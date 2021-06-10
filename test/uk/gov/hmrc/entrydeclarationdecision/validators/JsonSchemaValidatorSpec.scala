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

package uk.gov.hmrc.entrydeclarationdecision.validators

import org.scalatest.Matchers.convertToAnyShouldWrapper
import org.scalatest.WordSpec
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.entrydeclarationdecision.logging.LoggingContext

class JsonSchemaValidatorSpec extends WordSpec {

  implicit val lc: LoggingContext = LoggingContext("eori", "corrId", "subId", Some("mrn"))

  "JsonSchemaValidator" must {
    "return true " when {
      "a valid message is supplied" in {
        val validRejection: JsValue = Json.parse("""{
                                                   |  "submissionId": "conversationid-1234567890-1234567890",
                                                   |  "metadata": {
                                                   |    "senderEORI": "sender_eori",
                                                   |    "senderBranch": "branch",
                                                   |    "preparationDateTime": "2020-02-01T13:10:10.000Z",
                                                   |    "messageType": "IE316",
                                                   |    "receivedDateTime": "2020-02-01T13:10:10.000Z",
                                                   |    "messageIdentification": "Rejectione",
                                                   |    "correlationId": "correlationid1",
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

        JsonSchemaValidator.validateJSONAgainstSchema(validRejection) shouldBe true

      }
    }

    "return false" when {
      "an invalid message is supplied" in {
        val validRejection: JsValue = Json.parse("""{
                                                   |  "submissionId": "submissionId",
                                                   |  "metadata": {
                                                   |    "senderEORI": "sender_eori",
                                                   |    "senderBranch": "branch",
                                                   |    "preparationDateTime": "2020-02-01T13:10:10.000Z",
                                                   |    "messageType": "IE316",
                                                   |    "receivedDateTime": "2020-02-01T13:10:10.000Z",
                                                   |    "messageIdentification": "Rejectione",
                                                   |    "correlationId": "correlationid",
                                                   |    "localReferenceNumber": "01/Failure"
                                                   |  },
                                                   |  "response": {
                                                   |    "rejectionDateTime": "2020-02-01T13:10:10.000Z",
                                                   |    "functionalError": [
                                                   |      {
                                                   |        "errorType": "XX",
                                                   |        "errorPointer": "This is a bad submission",
                                                   |        "errorReason": "Bad",
                                                   |        "Ooops this looks wrong!!!": "XXXXX"
                                                   |      }
                                                   |    ]
                                                   |  }
                                                   |}""".stripMargin)

        JsonSchemaValidator.validateJSONAgainstSchema(validRejection) shouldBe false
      }
    }
  }
}

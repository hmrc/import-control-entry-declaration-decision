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

package uk.gov.hmrc.entrydeclarationdecision.models.decision

import java.time.Instant

import play.api.libs.json.Json
import uk.gov.hmrc.play.test.UnitSpec

class DecisionResponseSpec extends UnitSpec {
  "Json reads" must {
    "deserialize DecisionResponse.Accepted from Json" in {
      Json.parse("""
                   |{
                   |  "movementReferenceNumber": "someMrn",
                   |  "acceptedDateTime": "2005-03-15T12:41:00Z"
                   |}
                   |""".stripMargin).as[DecisionResponse] shouldBe
        DecisionResponse.Acceptance("someMrn", Instant.parse("2005-03-15T12:41:00Z"))
    }

    "deserialize DecisionResponse.Rejected from Json" in {
      Json.parse("""
                   |{
                   |    "functionalError": [
                   |      {
                   |        "errorType": "errorType1",
                   |        "errorPointer": "errorPointer1",
                   |        "errorReason": "reason1",
                   |        "originalAttributeValue": "originalAttrib1"
                   |      },
                   |      {
                   |        "errorType": "errorType2",
                   |        "errorPointer": "errorPointer2",
                   |        "errorReason": "reason2",
                   |        "originalAttributeValue": "originalAttrib2"
                   |      }
                   |    ],
                   |    "rejectionDateTime": "2020-01-13T15:22:44.000Z"
                   |  }
                   |""".stripMargin).as[DecisionResponse] shouldBe
        DecisionResponse.Rejection(
          List(
            DecisionError("errorType1", "errorPointer1", Some("reason1"), Some("originalAttrib1")),
            DecisionError("errorType2", "errorPointer2", Some("reason2"), Some("originalAttrib2"))
          ),
          Instant.parse("2020-01-13T15:22:44.000Z")
        )
    }
  }

}

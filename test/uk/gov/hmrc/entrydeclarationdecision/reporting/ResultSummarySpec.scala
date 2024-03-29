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
import play.api.libs.json.Json
import uk.gov.hmrc.entrydeclarationdecision.reporting.ResultSummary.{Accepted, Rejected}

class ResultSummarySpec extends PlaySpec {

  "ResultSummary" must {
    "write to Json correctly" in {
      Json.toJson[ResultSummary](Accepted) shouldBe
        Json.parse("""
                     |{
                     |  "type": "ACCEPTED"
                     |}
                     |""".stripMargin)

      Json.toJson[ResultSummary](Rejected(123)) shouldBe
        Json.parse("""
                     |{
                     |  "type": "REJECTED",
                     |  "errorCount": 123
                     |}
                     |""".stripMargin)
    }
  }

}

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

package uk.gov.hmrc.entrydeclarationdecision.services

import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.entrydeclarationdecision.services.XMLBuilder._

import java.time.Instant

class XMLBuilderSpec extends PlaySpec {
  "XMLBuilder" must {
    "use the correct format" when {
      "formatting with getDateFromDateTime" in {
        getDateFromDateTime(Instant.parse("2019-01-01T10:00:00.000Z")) shouldBe "190101"
      }
      "formatting with getTimeFromDateTime" in {
        getTimeFromDateTime(Instant.parse("2019-03-29T23:12:00.000Z")) shouldBe "2312"
      }
      "formatting with getDateTimeInXSDFormat" in {
        getDateTimeInXSDFormat(Instant.parse("2019-03-29T23:12:00.000Z")) shouldBe "201903292312"
      }
    }
  }
}

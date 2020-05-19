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

import uk.gov.hmrc.entrydeclarationdecision.services.XMLBuilder._
import uk.gov.hmrc.play.test.UnitSpec

class XMLBuilderSpec extends UnitSpec {
  "XMLBuilder" when {
    "formatting with getDateFromDateTime" must {
      "format in UTC" when {
        "given time with GMT offset" in {
          getDateFromDateTime(ZonedDateTime.parse("2019-01-01T10:00:00.000+00:00")) shouldBe "190101"
        }

        "given time with BST offset" in {
          getDateFromDateTime(ZonedDateTime.parse("2019-06-01T00:00:00.000+01:00")) shouldBe "190531"
        }
      }
    }

    "formatting with getTimeFromDateTime" must {
      "use the correct format" in {
        getTimeFromDateTime(ZonedDateTime.parse("2019-03-29T23:12:00.000Z")) shouldBe "2312"
      }

      "format in UTC" when {
        "given time with GMT offset" in {
          getTimeFromDateTime(ZonedDateTime.parse("2019-01-01T10:00:00.000+00:00")) shouldBe "1000"
        }

        "given time with BST offset" in {
          getTimeFromDateTime(ZonedDateTime.parse("2019-06-01T11:00:00.000+01:00")) shouldBe "1000"
        }
      }
    }

    "formatting with getDateTimeInXSDFormat" must {
      "use the correct format" in {
        getDateTimeInXSDFormat(ZonedDateTime.parse("2019-03-29T23:12:00.000Z")) shouldBe "201903292312"
      }

      "format in UTC" when {
        "given time with GMT offset GMT" in {
          getDateTimeInXSDFormat(ZonedDateTime.parse("2019-01-01T10:00:00.000+00:00")) shouldBe "201901011000"
        }

        "given time with BST offset" in {
          getDateTimeInXSDFormat(ZonedDateTime.parse("2019-06-01T00:00:00.000+01:00")) shouldBe "201905312300"
        }
      }
    }
  }
}

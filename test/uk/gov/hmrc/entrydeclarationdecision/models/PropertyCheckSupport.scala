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

package uk.gov.hmrc.entrydeclarationdecision.models

import org.scalacheck.Gen.choose

import java.time.Instant
import org.scalacheck.{Arbitrary, Gen}

trait PropertyCheckSupport {
  implicit val arbInstant: Arbitrary[Instant] =
    Arbitrary(Gen.choose(0, Long.MaxValue).map(ts => Instant.ofEpochMilli(ts)))

  //The two below lazy vals have been added to circumvent a bug in scalacheck
  implicit lazy val arbChar: Arbitrary[Char] = Arbitrary {
    // valid ranges are [0x0000, 0xD7FF] and [0xE000, 0xFFFD].
    //
    // ((0xFFFD + 1) - 0xE000) + ((0xD7FF + 1) - 0x0000)
    choose(0, 63485).map { i =>
      if (i <= 0xD7FF) i.toChar
      else (i + 2048).toChar
    }
  }

  implicit lazy val arbString: Arbitrary[String] =
    Arbitrary(Gen.stringOf(arbChar.arbitrary))
}

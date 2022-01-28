/*
 * Copyright 2022 HM Revenue & Customs
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

import java.time.{Clock, Duration, Instant, ZoneOffset}

class EisResponseTimeSpec extends PlaySpec {

  val now: Instant = Instant.now
  val clock: Clock = Clock.fixed(now, ZoneOffset.UTC)

  val timeTaken = 100

  val eisResponseTime: EisResponseTime = EisResponseTime(Duration.ofMillis(timeTaken))

  "EisResponseTime" must {
    "have the correct associated JSON event" in {
      val event = implicitly[EventSources[EisResponseTime]].eventFor(clock, eisResponseTime)

      event shouldBe None
    }

    "have the correct associated audit event" in {
      val event = implicitly[EventSources[EisResponseTime]].auditEventFor(eisResponseTime).get

      event.auditType       shouldBe "EisResponseTime"
      event.transactionName shouldBe "EIS Response Time"
      event.detail          shouldBe Json.obj("duration" -> timeTaken)
    }
  }
}

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

package uk.gov.hmrc.entrydeclarationdecision.utils

import java.time.{Clock, Duration, Instant, ZoneOffset}

import com.kenshoo.play.metrics.Metrics
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{FiniteDuration, SECONDS}

class TimerSpec extends UnitSpec with Timer with EventLogger {
  val metrics: Metrics   = new MockMetrics
  val startTime: Instant = Instant.now
  val endTime: Instant   = startTime.plusSeconds(2)
  val clock: Clock       = Clock.fixed(endTime, ZoneOffset.UTC)

  var timeMs: Long = _

  override def stopAndLog[A](name: String, timer: com.codahale.metrics.Timer.Context): Unit =
    timeMs = timer.stop() / 1000000

  "Timer" should {
    val sleepMs = 300

    "Time a future correctly" in {
      await(timeFuture("test timer", "test.sleep") {
        Thread.sleep(sleepMs)
      })
      val beWithinTolerance = be >= sleepMs.toLong and be <= (sleepMs + 100).toLong
      timeMs should beWithinTolerance
    }

    "Time a block correctly" in {
      await(time("test timer", "test.sleep") {
        Thread.sleep(sleepMs)
      })
      val beWithinTolerance = be >= sleepMs.toLong and be <= (sleepMs + 100).toLong
      timeMs should beWithinTolerance
    }
    "TimeFrom calculates the time correctly" in {
      timeFrom("test timer", startTime) shouldBe Duration.between(startTime, endTime)
    }
    "isLongJourneyTime returns true" when {
      "startTime - endTime > longJourneyTime" in {
        val longJourneyTime = new FiniteDuration(1, SECONDS)
        isLongJourneyTime(startTime, longJourneyTime) shouldBe true
      }
      "startTime - endTime = longJourneyTime" in {
        val longJourneyTime = new FiniteDuration(2, SECONDS)
        isLongJourneyTime(startTime, longJourneyTime) shouldBe true
      }
    }
    "isLongJourneyTime returns false" when {
      "startTime - endTime < longJourneyTime" in {
        val longJourneyTime = new FiniteDuration(3, SECONDS)
        isLongJourneyTime(startTime, longJourneyTime) shouldBe false
      }
    }
  }
}

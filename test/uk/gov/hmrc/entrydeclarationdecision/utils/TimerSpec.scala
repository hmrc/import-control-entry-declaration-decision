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

package uk.gov.hmrc.entrydeclarationdecision.utils

import com.codahale.metrics.MetricRegistry
import org.apache.pekko.util.Timeout
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import java.time.{Clock, Duration, Instant, ZoneOffset}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.Logging

class TimerSpec extends PlaySpec with Timer with Logging {
  val metrics: MetricRegistry   = new MetricRegistry()
  val startTime: Instant = Instant.now
  val endTime: Instant   = startTime.plusSeconds(2)
  val clock: Clock       = Clock.fixed(endTime, ZoneOffset.UTC)

  var timeMs: Long = _

  override def stopAndLog[A](name: String, timer: com.codahale.metrics.Timer.Context): Unit =
    timeMs = timer.stop() / 1000000

  "Timer" must {
    val sleepMs = 300
    val timeout: Timeout = defaultAwaitTimeout

    "Time a future correctly" in {
      await(timeFuture("test timer", "test.sleep") {
        Future.successful(Thread.sleep(sleepMs))
      })(timeout)
      val beWithinTolerance = be >= sleepMs.toLong and be <= (sleepMs + 100).toLong
      timeMs should beWithinTolerance
    }

    "Time a block correctly" in {
      await(time("test timer", "test.sleep") {
        Future.successful(Thread.sleep(sleepMs))
      })(timeout)
      val beWithinTolerance = be >= sleepMs.toLong and be <= (sleepMs + 100).toLong
      timeMs should beWithinTolerance
    }
    "TimeFrom calculates the time correctly" in {
      timeFrom("test timer", startTime) shouldBe Duration.between(startTime, endTime)
    }
  }
}

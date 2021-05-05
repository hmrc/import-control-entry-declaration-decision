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

package uk.gov.hmrc.entrydeclarationdecision.utils

import uk.gov.hmrc.entrydeclarationdecision.logging.{ContextLogger, LoggingContext}

import scala.concurrent.duration.FiniteDuration

class PagerDutyLogger {
  def logEventFailure(statusCode: Int)(implicit lc: LoggingContext): Unit =
    ContextLogger.error(s"Send event failed with status $statusCode")

  def logEventError(e: Throwable)(implicit lc: LoggingContext): Unit =
    ContextLogger.error(s"Send event failed with error", e)

  def logLongJourneyTime(journeyTime: FiniteDuration, longJourneyTime: FiniteDuration)(
    implicit lc: LoggingContext): Unit =
    ContextLogger.warn("LONG_END_TO_END_JOURNEY_TIME - " +
      s"End to End journey is greater than ${longJourneyTime.toSeconds} seconds. Journey took ${journeyTime.toSeconds} seconds")
}

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

package uk.gov.hmrc.entrydeclarationdecision.logging

import play.api.Logger

object ContextLogger {

  def info(message: => String)(implicit lc: LoggingContext): Unit =
    Logger.info(formatMessage(message))

  def warn(message: => String)(implicit lc: LoggingContext): Unit =
    Logger.warn(formatMessage(message))

  def error(message: => String)(implicit lc: LoggingContext): Unit =
    Logger.error(formatMessage(message))

  def error(message: => String, e: => Throwable)(implicit lc: LoggingContext): Unit =
    Logger.error(formatMessage(message), e)

  private def formatMessage(message: => String)(implicit lc: LoggingContext): String =
    s"$message (${lc.context})"
}
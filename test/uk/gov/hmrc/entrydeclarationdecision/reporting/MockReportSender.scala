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

import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.entrydeclarationdecision.logging.LoggingContext
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait MockReportSender extends MockFactory {
  val mockReportSender: ReportSender = mock[ReportSender]

  object MockReportSender {
    def sendReport[R](report: R): CallHandler[Future[Unit]] =
      (mockReportSender
        .sendReport(_: R)(_: EventSources[R], _: HeaderCarrier, _: LoggingContext)) expects (report, *, *, *)
  }

}

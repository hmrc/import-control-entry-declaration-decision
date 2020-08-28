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

package uk.gov.hmrc.entrydeclarationdecision.connectors

import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.entrydeclarationdecision.logging.LoggingContext
import uk.gov.hmrc.entrydeclarationdecision.models.ErrorCode
import uk.gov.hmrc.entrydeclarationdecision.models.outcome.Outcome
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait MockOutcomeConnector extends MockFactory {
  val mockOutcomeConnector: OutcomeConnector = mock[OutcomeConnector]

  object MockOutcomeConnector {
    def send(outcome: Outcome): CallHandler[Future[Either[ErrorCode, Unit]]] =
      (mockOutcomeConnector.send(_: Outcome)(_: HeaderCarrier, _: LoggingContext)).expects(outcome, *, *)
  }

}

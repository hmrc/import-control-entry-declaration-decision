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

import javax.inject.{Inject, Singleton}
import play.api.http.Status.CREATED
import uk.gov.hmrc.entrydeclarationdecision.config.AppConfig
import uk.gov.hmrc.entrydeclarationdecision.logging.{ContextLogger, LoggingContext}
import uk.gov.hmrc.entrydeclarationdecision.models.ErrorCode
import uk.gov.hmrc.entrydeclarationdecision.models.outcome.Outcome
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class OutcomeConnector @Inject()(client: HttpClient, appConfig: AppConfig)(implicit ec: ExecutionContext) {

  lazy val url = s"${appConfig.outcomeHost}/import-control/outcome"

  def send(outcome: Outcome)(implicit hc: HeaderCarrier, lc: LoggingContext): Future[Either[ErrorCode, Unit]] = {
    ContextLogger.info(s"sending POST request to $url")

    client
      .POST[Outcome, HttpResponse](url, outcome)
      .map { response =>
        response.status match {
          case CREATED =>
            ContextLogger.info("Outcome sent successfully")
            Right(())
          case status =>
            ContextLogger.warn(s"Unable to send outcome. Got status code $status")
            Left(ErrorCode.ConnectorError)
        }
      }
      .recover {
        case NonFatal(e) =>
          ContextLogger.error(s"Unable to send request to $url", e)
          Left(ErrorCode.ConnectorError)
      }
  }
}

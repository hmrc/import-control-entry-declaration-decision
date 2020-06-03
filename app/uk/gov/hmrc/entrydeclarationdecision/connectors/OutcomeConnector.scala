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

import java.io.IOException

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.entrydeclarationdecision.config.AppConfig
import uk.gov.hmrc.entrydeclarationdecision.models.ErrorCode
import uk.gov.hmrc.entrydeclarationdecision.models.outcome.Outcome
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class OutcomeConnector @Inject()(client: HttpClient, appConfig: AppConfig)(implicit ec: ExecutionContext) {

  lazy val url = s"${appConfig.outcomeHost}/import-control/outcome"

  implicit object ResultReads extends HttpReads[Either[ErrorCode, Unit]] {
    override def read(method: String, url: String, response: HttpResponse): Either[ErrorCode, Unit] =
      response.status match {
        case CREATED  => Right(())
        case CONFLICT => Left(ErrorCode.DuplicateSubmission)
        case _        => Left(ErrorCode.ConnectorError)
      }
  }

  def send(outcome: Outcome)(implicit hc: HeaderCarrier): Future[Either[ErrorCode, Unit]] = {
    Logger.info(s"sending POST request to $url")

    client
      .POST(url, Json.toJson(outcome))
      .recover {
        case e: IOException =>
          Logger.error(s"Unable to send request to $url", e)
          Left(ErrorCode.ConnectorError)
      }
  }
}

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
import play.api.libs.ws.WSClient
import uk.gov.hmrc.entrydeclarationdecision.config.AppConfig
import uk.gov.hmrc.entrydeclarationdecision.models.ErrorCode
import uk.gov.hmrc.entrydeclarationdecision.models.outcome.Outcome

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class OutcomeConnector @Inject()(ws: WSClient, appConfig: AppConfig)(implicit ec: ExecutionContext) {

  lazy val url = s"${appConfig.outcomeHost}/import-control/outcome"

  def send(outcome: Outcome): Future[Either[ErrorCode, Unit]] = {
    Logger.info(s"sending POST request to $url")

    ws.url(url)
      .post(Json.toJson(outcome))
      .map { response =>
        response.status match {
          case CREATED  => Right(())
          case CONFLICT => Left(ErrorCode.DuplicateSubmission)
          case _        => Left(ErrorCode.ConnectorError)
        }
      }
      .recover {
        case e: IOException =>
          Logger.error(s"Unable to send request to $url", e)
          Left(ErrorCode.ConnectorError)
      }
  }
}

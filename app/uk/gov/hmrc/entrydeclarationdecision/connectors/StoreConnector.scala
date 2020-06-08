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
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.Reads
import uk.gov.hmrc.entrydeclarationdecision.config.AppConfig
import uk.gov.hmrc.entrydeclarationdecision.models.ErrorCode
import uk.gov.hmrc.entrydeclarationdecision.models.enrichment.acceptance.AcceptanceEnrichment
import uk.gov.hmrc.entrydeclarationdecision.models.enrichment.rejection.AmendmentRejectionEnrichment
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class StoreConnector @Inject()(client: HttpClient, appConfig: AppConfig)(implicit ec: ExecutionContext) {

  def getAcceptanceEnrichment(submissionId: String, amendment: Boolean)(
    implicit hc: HeaderCarrier): Future[Either[ErrorCode, AcceptanceEnrichment]] = {
    val amendmentParam = if (amendment) "amendment" else "declaration"
    val url =
      s"${appConfig.storeHost}/import-control/$amendmentParam/acceptance-enrichment/$submissionId"

    getEnrichment[AcceptanceEnrichment](url)
  }

  def getAmendmentRejectionEnrichment(submissionId: String)(
    implicit hc: HeaderCarrier): Future[Either[ErrorCode, AmendmentRejectionEnrichment]] = {
    val url =
      s"${appConfig.storeHost}/import-control/amendment/rejection-enrichment/$submissionId"

    getEnrichment[AmendmentRejectionEnrichment](url)
  }

  implicit private def httpReads[E: Reads]: HttpReads[Either[ErrorCode, E]] = new HttpReads[Either[ErrorCode, E]] {
    override def read(method: String, url: String, response: HttpResponse): Either[ErrorCode, E] =
      response.status match {
        case OK        => Right(response.json.as[E])
        case NOT_FOUND => Left(ErrorCode.NoSubmission)
        case _: Int    => Left(ErrorCode.ConnectorError)
      }
  }

  private def getEnrichment[E: Reads](url: String)(implicit hc: HeaderCarrier): Future[Either[ErrorCode, E]] = {
    Logger.info(s"sending GET request to $url")

    client
      .GET(url)
      .recover {
        case NonFatal(e) =>
          Logger.error(s"Unable to send request to $url", e)
          Left(ErrorCode.ConnectorError)
      }
  }
}
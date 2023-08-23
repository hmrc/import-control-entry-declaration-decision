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

package uk.gov.hmrc.entrydeclarationdecision.connectors

import javax.inject.{Inject, Singleton}
import play.api.http.Status._
import play.api.libs.json.{JsObject, Reads}
import uk.gov.hmrc.entrydeclarationdecision.config.AppConfig
import uk.gov.hmrc.entrydeclarationdecision.logging.{ContextLogger, LoggingContext}
import uk.gov.hmrc.entrydeclarationdecision.models.ErrorCode
import uk.gov.hmrc.entrydeclarationdecision.models.enrichment.acceptance.AcceptanceEnrichment
import uk.gov.hmrc.entrydeclarationdecision.models.enrichment.rejection.{AmendmentRejectionEnrichment, DeclarationRejectionEnrichment}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class StoreConnector @Inject()(client: HttpClient, appConfig: AppConfig)(implicit ec: ExecutionContext) {

  def getAcceptanceEnrichment(submissionId: String, amendment: Boolean)(
    implicit hc: HeaderCarrier,
    lc: LoggingContext): Future[Either[ErrorCode, AcceptanceEnrichment]] = {
    val amendmentParam = if (amendment) "amendment" else "declaration"
    val url            = s"${appConfig.storeHost}/import-control/$amendmentParam/acceptance-enrichment/$submissionId"

    getEnrichment[AcceptanceEnrichment](url)
  }

  def getAmendmentRejectionEnrichment(submissionId: String)(
    implicit hc: HeaderCarrier,
    lc: LoggingContext): Future[Either[ErrorCode, AmendmentRejectionEnrichment]] = {
    val url = s"${appConfig.storeHost}/import-control/amendment/rejection-enrichment/$submissionId"

    getEnrichment[AmendmentRejectionEnrichment](url)
  }

  def getDeclarationRejectionEnrichment(submissionId: String)(
    implicit hc: HeaderCarrier,
    lc: LoggingContext): Future[Either[ErrorCode, DeclarationRejectionEnrichment]] = {
    val url = s"${appConfig.storeHost}/import-control/declaration/rejection-enrichment/$submissionId"

    getEnrichment[DeclarationRejectionEnrichment](url)
  }

  private def getEnrichment[E: Reads](
    url: String)(implicit hc: HeaderCarrier, lc: LoggingContext): Future[Either[ErrorCode, E]] = {
    ContextLogger.info(s"sending GET request to $url")

    client
      .GET[HttpResponse](url)
      .map(response =>
        response.status match {
          case OK =>
            ContextLogger.info("Got enrichment")
            Right(response.json.as[E])
          case NOT_FOUND =>
            ContextLogger.info("Enrichment not found")
            Left(ErrorCode.NoSubmission)
          case status: Int =>
            ContextLogger.warn(s"Unable to get enrichment. Got status code $status")
            Left(ErrorCode.ConnectorError)
      })
      .recover {
        case NonFatal(e) =>
          ContextLogger.error(s"Unable to send request to $url", e)
          Left(ErrorCode.ConnectorError)
      }
  }

  def setShortTtl(submissionId: String)(implicit hc: HeaderCarrier, lc: LoggingContext): Future[Boolean] = {
    val url = s"${appConfig.storeHost}/import-control/housekeeping/submissionid/$submissionId"
    ContextLogger.info(s"sending PUT request to $url")

    client
      .PUT[JsObject, HttpResponse](url, JsObject.empty)
      .map(response =>
        response.status match {
          case NO_CONTENT => true
          case code =>
            ContextLogger.warn(s"Unable to set short TTL. Got status code $code")
            false
      })
  }
}

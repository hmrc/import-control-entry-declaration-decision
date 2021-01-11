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

package uk.gov.hmrc.entrydeclarationdecision.connectors

import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.entrydeclarationdecision.logging.LoggingContext
import uk.gov.hmrc.entrydeclarationdecision.models.ErrorCode
import uk.gov.hmrc.entrydeclarationdecision.models.enrichment.acceptance.AcceptanceEnrichment
import uk.gov.hmrc.entrydeclarationdecision.models.enrichment.rejection.{AmendmentRejectionEnrichment, DeclarationRejectionEnrichment}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait MockStoreConnector extends MockFactory {
  val mockStoreConnector: StoreConnector = mock[StoreConnector]

  object MockStoreConnector {
    def getAcceptanceEnrichment(
      submissionId: String,
      amendment: Boolean): CallHandler[Future[Either[ErrorCode, AcceptanceEnrichment]]] =
      (mockStoreConnector
        .getAcceptanceEnrichment(_: String, _: Boolean)(_: HeaderCarrier, _: LoggingContext))
        .expects(submissionId, amendment, *, *)

    def getAmendmentRejectionEnrichment(
      submissionId: String): CallHandler[Future[Either[ErrorCode, AmendmentRejectionEnrichment]]] =
      (mockStoreConnector
        .getAmendmentRejectionEnrichment(_: String)(_: HeaderCarrier, _: LoggingContext))
        .expects(submissionId, *, *)

    def getDeclarationRejectionEnrichment(
      submissionId: String): CallHandler[Future[Either[ErrorCode, DeclarationRejectionEnrichment]]] =
      (mockStoreConnector
        .getDeclarationRejectionEnrichment(_: String)(_: HeaderCarrier, _: LoggingContext))
        .expects(submissionId, *, *)

    def setShortTtl(submissionId: String): CallHandler[Future[Boolean]] =
      (mockStoreConnector.setShortTtl(_: String)(_: HeaderCarrier, _: LoggingContext)).expects(submissionId, *, *)
  }
}

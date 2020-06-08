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

package uk.gov.hmrc.entrydeclarationdecision.controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsResult, JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.entrydeclarationdecision.config.AppConfig
import uk.gov.hmrc.entrydeclarationdecision.models.decision.{Decision, DecisionResponse}
import uk.gov.hmrc.entrydeclarationdecision.models.{ErrorCode, ErrorResponse}
import uk.gov.hmrc.entrydeclarationdecision.reporting.{DecisionReceived, ReportSender, ResultSummary}
import uk.gov.hmrc.entrydeclarationdecision.services.ProcessDecisionService
import uk.gov.hmrc.entrydeclarationdecision.validators.JsonSchemaValidator
import uk.gov.hmrc.play.bootstrap.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DecisionReceiverController @Inject()(
  appConfig: AppConfig,
  cc: ControllerComponents,
  service: ProcessDecisionService,
  reportSender: ReportSender)(implicit ec: ExecutionContext)
    extends EisInboundAuthorisedController(cc, appConfig) {

  val handlePost: Action[JsValue] = authorisedAction.async(parse.json) { implicit request =>
    val model: JsResult[Decision[DecisionResponse]] = request.body.validate[Decision[DecisionResponse]]

    if (model.isSuccess) {
      getValidationErrors(model.get, request.body) match {
        case Some(errorMsg) => Future.successful(BadRequest(errorMsg))
        case None =>
          val decision = model.get

          def report(failure: Option[ErrorCode]) = {
            val resultSummary: ResultSummary = decision.response match {
              case _: DecisionResponse.Acceptance      => ResultSummary.Accepted
              case DecisionResponse.Rejection(errs, _) => ResultSummary.Rejected(errs.size)
            }

            DecisionReceived(
              eori          = decision.metadata.senderEORI,
              correlationId = decision.metadata.correlationId,
              submissionId  = decision.submissionId,
              decision.metadata.messageType,
              request.body,
              resultSummary,
              failure
            )
          }

          service.processDecision(decision).map {
            case Right(()) =>
              reportSender.sendReport(report(None))
              Created

            case Left(errorCode) =>
              reportSender.sendReport(report(Some(errorCode)))
              errorCode match {
                case ErrorCode.NoSubmission        => Conflict(Json.toJson(ErrorResponse.noSubmission))
                case ErrorCode.DuplicateSubmission => Conflict(Json.toJson(ErrorResponse.duplicate))
                case ErrorCode.ConnectorError      => ServiceUnavailable(Json.toJson(ErrorResponse.unavailable))
              }
          }
      }
    } else {
      Future.successful(BadRequest(Json.toJson(ErrorResponse.errorParse)).as("application/json"))
    }
  }

  private def getValidationErrors[R <: DecisionResponse](decision: Decision[R], json: JsValue): Option[JsValue] =
    if (appConfig.validateIncomingJson) {
      if (!JsonSchemaValidator.validateJSONAgainstSchema(json)) {
        Some(Json.toJson(ErrorResponse.errorSchema))
      } else {
        None
      }
    } else {
      if (decision.metadata.messageType.isAcceptance != decision.response.isAcceptance) {
        Some(Json.toJson(ErrorResponse.errorMutualExclusive))
      } else {
        None
      }
    }

}

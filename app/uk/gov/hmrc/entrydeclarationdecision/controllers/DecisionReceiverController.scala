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

package uk.gov.hmrc.entrydeclarationdecision.controllers

import javax.inject.{Inject, Singleton}
import play.api.Logging
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.entrydeclarationdecision.config.AppConfig
import uk.gov.hmrc.entrydeclarationdecision.logging.{ContextLogger, LoggingContext}
import uk.gov.hmrc.entrydeclarationdecision.models.decision.{Decision, DecisionResponse}
import uk.gov.hmrc.entrydeclarationdecision.models.{ErrorCode, ErrorResponse}
import uk.gov.hmrc.entrydeclarationdecision.reporting.{DecisionReceived, ReportSender, ResultSummary}
import uk.gov.hmrc.entrydeclarationdecision.services.ProcessDecisionService
import uk.gov.hmrc.entrydeclarationdecision.validators.JsonSchemaValidator

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DecisionReceiverController @Inject()(
  appConfig: AppConfig,
  cc: ControllerComponents,
  service: ProcessDecisionService,
  reportSender: ReportSender)(implicit ec: ExecutionContext)
    extends EisInboundAuthorisedController(cc, appConfig) with Logging {

  val handlePost: Action[JsValue] = authorisedAction.async(parse.json) { implicit request =>
    request.body.validate[Decision[DecisionResponse]] match {
      case JsSuccess(decision, _) =>
        implicit val lc: LoggingContext = LoggingContext(
          eori                    = decision.metadata.senderEORI,
          correlationId           = decision.metadata.correlationId,
          submissionId            = decision.submissionId,
          movementReferenceNumber = decision.movementReferenceNumber,
          messageType             = decision.metadata.messageType
        )

        ContextLogger.info("Decision received")

        getValidationErrors(decision, request.body) match {
          case Some(errorMsg) => Future.successful(BadRequest(errorMsg))
          case None =>
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
                failure,
                mrn = decision.movementReferenceNumber
              )
            }

            service.processDecision(decision).map {
              case Right(()) =>
                reportSender.sendReport(report(None))
                Created

              case Left(errorCode) =>
                reportSender.sendReport(report(Some(errorCode)))
                errorCode match {
                  case ErrorCode.NoSubmission   => BadRequest(Json.toJson(ErrorResponse.noSubmission))
                  case ErrorCode.ConnectorError => ServiceUnavailable(Json.toJson(ErrorResponse.unavailable))
                }
            }
        }

      case JsError(errs) =>
        logger.error(s"Unable to parse decision payload: $errs")
        Future.successful(BadRequest(Json.toJson(ErrorResponse.errorParse)).as("application/json"))
    }
  }

  private def getValidationErrors[R <: DecisionResponse](decision: Decision[R], json: JsValue)(
    implicit lc: LoggingContext): Option[JsValue] =
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

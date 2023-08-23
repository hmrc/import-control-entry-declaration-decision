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

package uk.gov.hmrc.entrydeclarationdecision.reporting

import java.time.{Clock, Instant}

import play.api.libs.json._
import uk.gov.hmrc.entrydeclarationdecision.models.ErrorCode
import uk.gov.hmrc.entrydeclarationdecision.models.decision.MessageType
import uk.gov.hmrc.entrydeclarationdecision.reporting.audit.AuditEvent
import uk.gov.hmrc.entrydeclarationdecision.reporting.events.{Event, EventCode}

case class DecisionReceived(
  eori: String,
  correlationId: String,
  submissionId: String,
  messageType: MessageType,
  body: JsValue,
  resultSummary: ResultSummary,
  failure: Option[ErrorCode],
  mrn: Option[String]
) extends Report

object DecisionReceived {
  implicit val eventSources: EventSources[DecisionReceived] = new EventSources[DecisionReceived] {
    override def eventFor(clock: Clock, report: DecisionReceived): Option[Event] = {
      import report._

      val detailJson = JsObject(
        Seq(
          "summary" -> Json.toJson(resultSummary)
        ) ++ failure.map { errorCode =>
          "failure" -> Json.toJson(errorCode)
        } ++ mrn.map("decisionMrn" -> Json.toJson(_))
      )

      val event = Event(
        eventCode      = EventCode.ENS_RESP,
        eventTimestamp = Instant.now(clock),
        submissionId   = submissionId,
        eori           = eori,
        correlationId  = correlationId,
        messageType    = messageType,
        detail         = Some(detailJson)
      )

      Some(event)
    }

    override def auditEventFor(report: DecisionReceived): Option[AuditEvent] = {
      import report._
      val auditEvent = AuditEvent(
        auditType       = "DecisionReceived",
        transactionName = "ENS decision received from EIS",
        JsObject(Seq("eori" -> JsString(eori), "correlationId" -> JsString(correlationId), "decisionBody" -> body))
      )

      Some(auditEvent)
    }
  }
}

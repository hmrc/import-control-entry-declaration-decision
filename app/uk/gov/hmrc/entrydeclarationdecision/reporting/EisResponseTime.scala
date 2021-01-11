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

package uk.gov.hmrc.entrydeclarationdecision.reporting

import java.time.{Clock, Duration}

import play.api.libs.json.Json
import uk.gov.hmrc.entrydeclarationdecision.reporting.audit.AuditEvent
import uk.gov.hmrc.entrydeclarationdecision.reporting.events.Event

case class EisResponseTime(timeTaken: Duration)

object EisResponseTime {
  implicit val eventSources: EventSources[EisResponseTime] = new EventSources[EisResponseTime] {
    override def eventFor(clock: Clock, report: EisResponseTime): Option[Event] = None

    override def auditEventFor(report: EisResponseTime): Option[AuditEvent] =
      Some(AuditEvent("EisResponseTime", "EIS Response Time", Json.obj("duration" -> report.timeTaken.toMillis)))
  }
}

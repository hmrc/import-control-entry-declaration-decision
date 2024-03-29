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

import com.codahale.metrics.MetricRegistry
import com.google.inject.Inject
import play.api.Logging
import uk.gov.hmrc.entrydeclarationdecision.logging.LoggingContext
import uk.gov.hmrc.entrydeclarationdecision.reporting.audit.{AuditEvent, AuditHandler}
import uk.gov.hmrc.entrydeclarationdecision.reporting.events.{Event, EventConnector}
import uk.gov.hmrc.entrydeclarationdecision.utils.Timer
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Clock
import scala.concurrent.{ExecutionContext, Future}

class ReportSender @Inject()(
  auditHandler: AuditHandler,
  eventConnector: EventConnector,
  override val clock: Clock,
  override val metrics: MetricRegistry)(implicit ec: ExecutionContext)
    extends Timer
    with Logging {
  def sendReport[R: EventSources](report: R)(implicit hc: HeaderCarrier, lc: LoggingContext): Future[Unit] = {

    val eventSources: EventSources[R] = implicitly

    eventSources.eventFor(clock, report).foreach(event => sendEvent(event))

    eventSources.auditEventFor(report).foreach(event => audit(event))

    Future.successful(())
  }

  private def audit[R: EventSources](event: AuditEvent)(implicit hc: HeaderCarrier) =
    timeFuture("ReportSender audit", "reporting.audit") {
      auditHandler.audit(event)
    }

  private def sendEvent[R: EventSources](event: Event)(implicit hc: HeaderCarrier, lc: LoggingContext) =
    timeFuture("ReportSender send event", "reporting.sendEvent") {
      eventConnector.sendEvent(event)
    }
}

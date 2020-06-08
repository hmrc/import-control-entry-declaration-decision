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

package uk.gov.hmrc.entrydeclarationdecision.models.outcome

import java.time.ZonedDateTime

import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.entrydeclarationdecision.models.decision.MessageType

case class Outcome(
  eori: String,
  correlationId: String,
  submissionId: String,
  receivedDateTime: ZonedDateTime,
  messageType: MessageType,
  movementReferenceNumber: Option[String],
  outcomeXml: String
)

object Outcome {
  implicit val jsonFormat: Format[Outcome] = Json.format[Outcome]
}
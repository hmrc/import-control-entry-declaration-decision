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

package uk.gov.hmrc.entrydeclarationdecision.models

import play.api.libs.json.{Json, Writes}

case class ErrorResponse(errorCode: String, message: String)

object ErrorResponse {
  implicit val writes: Writes[ErrorResponse] = Json.writes[ErrorResponse]

  val noSubmission: ErrorResponse = ErrorResponse("NO_SUBMISSION", "Unable to find original submission in store")

  val unavailable: ErrorResponse = ErrorResponse("TEMPORARILY_UNAVAILABLE", "Temporarily unavailable")

  val errorMutualExclusive: ErrorResponse =
    ErrorResponse("NOT_SCHEMA_VALID", "Message type does not correspond to body")

  val errorParse: ErrorResponse = ErrorResponse("UNPARSABLE_JSON", "Failed to parse JSON")

  val errorSchema: ErrorResponse = ErrorResponse("NOT_SCHEMA_VALID", "Failed to validate JSON against schema")
}

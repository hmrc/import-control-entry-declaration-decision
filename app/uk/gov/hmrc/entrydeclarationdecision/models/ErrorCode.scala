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

package uk.gov.hmrc.entrydeclarationdecision.models

import play.api.libs.json.{JsObject, JsString, Writes}

sealed trait ErrorCode

object ErrorCode {

  case object NoSubmission extends ErrorCode

  case object ConnectorError extends ErrorCode

  implicit val writes: Writes[ErrorCode] = Writes {
    case NoSubmission        => JsObject(Seq("type" -> JsString("NO_SUBMISSION")))
    case ConnectorError      => JsObject(Seq("type" -> JsString("CONNECTOR_ERROR")))
  }
}

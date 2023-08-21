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

package uk.gov.hmrc.entrydeclarationdecision.models.decision

import java.time.Instant

import play.api.libs.json.{JsPath, Json, Reads}

sealed trait DecisionResponse {
  def isAcceptance: Boolean
}

object DecisionResponse {
  case class Acceptance(
    movementReferenceNumber: String,
    acceptedDateTime: Instant
  ) extends DecisionResponse {
    override def isAcceptance: Boolean = true
  }

  object Acceptance {
    implicit val jsonReads: Reads[Acceptance] = Json.reads[Acceptance]
  }

  case class Rejection(
    functionalError: Seq[DecisionError],
    rejectionDateTime: Instant
  ) extends DecisionResponse {
    override def isAcceptance: Boolean = false
  }

  object Rejection {
    implicit val jsonReads: Reads[Rejection] = Json.reads[Rejection]
  }

  implicit val jsonReads: Reads[DecisionResponse] =
    (JsPath \ "movementReferenceNumber").readNullable[String].flatMap {
      case Some(_) => implicitly[Reads[Acceptance]].map(x => x: DecisionResponse)
      case None    => implicitly[Reads[Rejection]].map(x => x: DecisionResponse)
    }
}

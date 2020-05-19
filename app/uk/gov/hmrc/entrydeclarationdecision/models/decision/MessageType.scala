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

package uk.gov.hmrc.entrydeclarationdecision.models.decision

import play.api.libs.json.Format
import uk.gov.hmrc.entrydeclarationdecision.utils.Enums

sealed trait MessageType {
  def isAcceptance: Boolean
}

object MessageType {

  case object IE328 extends MessageType {
    override def isAcceptance: Boolean = true
  }

  case object IE304 extends MessageType {
    override def isAcceptance: Boolean = true
  }

  case object IE316 extends MessageType {
    override def isAcceptance: Boolean = false
  }

  case object IE305 extends MessageType {
    override def isAcceptance: Boolean = false
  }

  implicit val formats: Format[MessageType] = Enums.format[MessageType]
}

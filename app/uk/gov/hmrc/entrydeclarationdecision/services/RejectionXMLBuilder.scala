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

package uk.gov.hmrc.entrydeclarationdecision.services

import uk.gov.hmrc.entrydeclarationdecision.models.decision.Decision
import uk.gov.hmrc.entrydeclarationdecision.models.decision.DecisionResponse.Rejection
import uk.gov.hmrc.entrydeclarationdecision.models.enrichment.Enrichment

import scala.xml.Node

trait RejectionXMLBuilder[E <: Enrichment] extends XMLBuilder[Rejection, E] {
  protected final def getErrorsXMLSection(decision: Decision[Rejection]): Seq[Node] =
    //@formatter:off
    for {
      err <- decision.response.functionalError
    } yield <FUNERRER1>
      <ErrTypER11>{err.errorType}</ErrTypER11>
      <ErrPoiER12>{err.errorPointer}</ErrPoiER12>
      {for (value <- err.errorReason.toSeq) yield <ErrReaER13>{value}</ErrReaER13>}
      {for (value <- err.originalAttributeValue.toSeq) yield <OriAttValER14>{value}</OriAttValER14>}
    </FUNERRER1>
  //@formatter:on
}

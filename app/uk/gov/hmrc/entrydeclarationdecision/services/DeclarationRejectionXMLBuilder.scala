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

package uk.gov.hmrc.entrydeclarationdecision.services

import javax.inject.Singleton
import uk.gov.hmrc.entrydeclarationdecision.models.decision.Decision
import uk.gov.hmrc.entrydeclarationdecision.models.decision.DecisionResponse.Rejection
import uk.gov.hmrc.entrydeclarationdecision.models.enrichment.rejection.DeclarationRejectionEnrichment
import uk.gov.hmrc.entrydeclarationdecision.services.XMLBuilder._

import scala.xml.Elem

@Singleton
class DeclarationRejectionXMLBuilder
    extends RejectionXMLBuilder[DeclarationRejectionEnrichment] {

  def buildXML(decision: Decision[Rejection], enrichment: DeclarationRejectionEnrichment): Elem =
    //Message Receiver is Message Sender as we don't currently support sending the message to anyone other than the message sender.
    //@formatter:off
    <cc3:CC316A xmlns:cc3="http://ics.dgtaxud.ec/CC316A">
      <MesSenMES3>{ s"${decision.metadata.senderEORI}/${decision.metadata.senderBranch}" }</MesSenMES3>
      <MesRecMES6>{ s"${decision.metadata.senderEORI}/${decision.metadata.senderBranch}" }</MesRecMES6>
      <DatOfPreMES9>{ getDateFromDateTime(decision.metadata.preparationDateTime) }</DatOfPreMES9>
      <TimOfPreMES10>{ getTimeFromDateTime(decision.metadata.preparationDateTime) }</TimOfPreMES10>
      <MesIdeMES19>{ decision.metadata.messageIdentification }</MesIdeMES19>
      <MesTypMES20>{ xmlMessageType(decision.metadata.messageType)}</MesTypMES20>
      <CorIdeMES25>{ decision.metadata.correlationId }</CorIdeMES25>
      <HEAHEA>
        {for (value <- decision.metadata.localReferenceNumber.toSeq) yield <RefNumHEA4>{ value }</RefNumHEA4>}
        <DecRejDatTimHEA116>{getDateTimeInXSDFormat(decision.response.rejectionDateTime)}</DecRejDatTimHEA116>
      </HEAHEA>
      { getErrorsXMLSection(decision) }
    </cc3:CC316A>
  //@formatter:on
}

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

import uk.gov.hmrc.entrydeclarationdecision.models.decision.Decision
import uk.gov.hmrc.entrydeclarationdecision.models.decision.DecisionResponse.Rejection
import uk.gov.hmrc.entrydeclarationdecision.models.enrichment.rejection.{AmendmentRejectionEnrichment, OfficeOfFirstEntry}
import uk.gov.hmrc.entrydeclarationdecision.services.XMLBuilder.{getDateFromDateTime, getDateTimeInXSDFormat, getTimeFromDateTime, xmlMessageType}

import scala.xml.Elem

class AmendmentRejectionXMLBuilder extends RejectionXMLBuilder[AmendmentRejectionEnrichment] {
  override def buildXML(decision: Decision[Rejection], enrichment: AmendmentRejectionEnrichment): Elem =
    //Message Receiver is Message Sender as we don't currently support sending the message to anyone other than the message sender.
    //@formatter:off
    <cc3:CC305A xmlns:cc3="http://ics.dgtaxud.ec/CC305A">
      <MesSenMES3>{ s"${decision.metadata.senderEORI}/${decision.metadata.senderBranch}" }</MesSenMES3>
      <MesRecMES6>{ s"${decision.metadata.senderEORI}/${decision.metadata.senderBranch}" }</MesRecMES6>
      <DatOfPreMES9>{ getDateFromDateTime(decision.metadata.preparationDateTime) }</DatOfPreMES9>
      <TimOfPreMES10>{ getTimeFromDateTime(decision.metadata.preparationDateTime) }</TimOfPreMES10>
      <MesIdeMES19>{ decision.metadata.messageIdentification }</MesIdeMES19>
      <MesTypMES20>{ xmlMessageType(decision.metadata.messageType)}</MesTypMES20>
      <CorIdeMES25>{ decision.metadata.correlationId }</CorIdeMES25>
      <HEAHEA>
        <DocNumHEA5>{enrichment.payload.amendment.movementReferenceNumber}</DocNumHEA5>
        <DatTimAmeHEA113>{getDateTimeInXSDFormat(enrichment.payload.amendment.dateTime)}</DatTimAmeHEA113>
        <AmeRejDatTimHEA112>{getDateTimeInXSDFormat(decision.response.rejectionDateTime)}</AmeRejDatTimHEA112>
      </HEAHEA>
      { getErrorsXMLSection(decision) }
      { for(rep <- enrichment.payload.parties.representative.toSeq) yield getTRAREP(rep) }
      {  getPERLODSUMDEC(enrichment.payload.parties.declarant)}
      { getCUSOFFFENT730(enrichment.payload.itinerary.officeOfFirstEntry)}
    </cc3:CC305A>
  //@formatter:on

  // Note - no ExpDatOfArrFIRENT733 element...
  private def getCUSOFFFENT730(officeOfFirstEntry: OfficeOfFirstEntry): Elem =
    //@formatter:off
    <CUSOFFFENT730>
      <RefNumCUSOFFFENT731>{officeOfFirstEntry.reference}</RefNumCUSOFFFENT731>
    </CUSOFFFENT730>
  //@formatter:on
}

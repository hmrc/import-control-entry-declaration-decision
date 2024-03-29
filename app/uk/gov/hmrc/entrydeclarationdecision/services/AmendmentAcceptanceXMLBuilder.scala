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

package uk.gov.hmrc.entrydeclarationdecision.services

import uk.gov.hmrc.entrydeclarationdecision.models.decision.Decision
import uk.gov.hmrc.entrydeclarationdecision.models.decision.DecisionResponse.Acceptance
import uk.gov.hmrc.entrydeclarationdecision.models.enrichment.acceptance.AcceptanceEnrichment
import uk.gov.hmrc.entrydeclarationdecision.services.XMLBuilder._

import scala.xml.Elem

class AmendmentAcceptanceXMLBuilder extends XMLBuilder[Acceptance, AcceptanceEnrichment] {

  def buildXML(decision: Decision[Acceptance], enrichment: AcceptanceEnrichment): Elem =
    //Message Receiver is Message Sender as we don't currently support sending the message to anyone other than the message sender.
    //@formatter:off
    <cc3:CC304A xmlns:cc3="http://ics.dgtaxud.ec/CC304A">
      <MesSenMES3>{ s"${decision.metadata.senderEORI}/${decision.metadata.senderBranch}" }</MesSenMES3>
      <MesRecMES6>{ s"${decision.metadata.senderEORI}/${decision.metadata.senderBranch}" }</MesRecMES6>
      <DatOfPreMES9>{ getDateFromDateTime(decision.metadata.preparationDateTime) }</DatOfPreMES9>
      <TimOfPreMES10>{ getTimeFromDateTime(decision.metadata.preparationDateTime) }</TimOfPreMES10>
      <MesIdeMES19>{ decision.metadata.messageIdentification }</MesIdeMES19>
      <MesTypMES20>{ xmlMessageType(decision.metadata.messageType) }</MesTypMES20>
      <CorIdeMES25>{ decision.metadata.correlationId }</CorIdeMES25>

      { getHEAHEA (decision,enrichment) }
      { getGOOITEGDS(enrichment) }
      { for(rep <- enrichment.payload.parties.representative.toSeq) yield getTRAREP(rep) }
      { getPERLODSUMDEC(enrichment.payload.parties.declarant) }
      { getCUSOFFFENT730( enrichment.payload.itinerary.officeOfFirstEntry) }
      { getTRACARENT601(enrichment) }
    </cc3:CC304A>
  //@formatter:on

  private def getHEAHEA(decision: Decision[Acceptance], enrichment: AcceptanceEnrichment) =
    //@formatter:off
    <HEAHEA>
      <DocNumHEA5>{ decision.response.movementReferenceNumber }</DocNumHEA5>

      <TraModAtBorHEA76>{enrichment.payload.itinerary.modeOfTransportAtBorder}</TraModAtBorHEA76>

      {for (identityOfMeansOfCrossingBorder <- enrichment.payload.itinerary.identityOfMeansOfCrossingBorder.toSeq)
        yield <IdeOfMeaOfTraCroHEA85>{ identityOfMeansOfCrossingBorder.identity }</IdeOfMeaOfTraCroHEA85>}

      {for {
        identityOfMeansOfCrossingBorder <- enrichment.payload.itinerary.identityOfMeansOfCrossingBorder.toSeq
        value <- identityOfMeansOfCrossingBorder.language.toSeq
       } yield <IdeOfMeaOfTraCroHEA85LNG>{ value }</IdeOfMeaOfTraCroHEA85LNG>}
      {for {
        identityOfMeansOfCrossingBorder <- enrichment.payload.itinerary.identityOfMeansOfCrossingBorder.toSeq
        value <- identityOfMeansOfCrossingBorder.nationality.toSeq
       } yield <NatOfMeaOfTraCroHEA87>{ value }</NatOfMeaOfTraCroHEA87>}
      {for (value <- enrichment.payload.itinerary.commercialReferenceNumber.toSeq) yield <ComRefNumHEA>{ value }</ComRefNumHEA>}
      {for (value <- enrichment.payload.itinerary.conveyanceReference.toSeq) yield <ConRefNumHEA>{ value }</ConRefNumHEA>}
      <AmeAccDatTimHEA111>{ getDateTimeInXSDFormat(decision.response.acceptedDateTime) }</AmeAccDatTimHEA111>
      {for(value <- enrichment.payload.amendment.toSeq) yield <DatTimAmeHEA113>{ getDateTimeInXSDFormat(value.dateTime) }</DatTimAmeHEA113>}
    </HEAHEA>
  //@formatter:on
}

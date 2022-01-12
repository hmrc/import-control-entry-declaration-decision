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

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneOffset}

import uk.gov.hmrc.entrydeclarationdecision.models.decision.{Decision, DecisionResponse, MessageType}
import uk.gov.hmrc.entrydeclarationdecision.models.enrichment.acceptance.{AcceptanceEnrichment, GoodsItem, OfficeOfFirstEntry}
import uk.gov.hmrc.entrydeclarationdecision.models.enrichment.{Enrichment, Trader}
import uk.gov.hmrc.entrydeclarationdecision.services.XMLBuilder.getDateTimeInXSDFormat

import scala.xml.{Elem, Node}

object XMLBuilder {
  // All dates and time in XML are UTC - make this explicit here...
  private val xsdDateFormatter     = DateTimeFormatter.ofPattern("uuMMdd").withZone(ZoneOffset.UTC)
  private val xsdTimeFormatter     = DateTimeFormatter.ofPattern("HHmm").withZone(ZoneOffset.UTC)
  private val xsdDateTimeFormatter = DateTimeFormatter.ofPattern("uuuuMMddHHmm").withZone(ZoneOffset.UTC)

  def getDateFromDateTime(dt: Instant): String = xsdDateFormatter.format(dt)

  def getTimeFromDateTime(dt: Instant): String = xsdTimeFormatter.format(dt)

  def getDateTimeInXSDFormat(dt: Instant): String = xsdDateTimeFormatter.format(dt)

  def xmlMessageType(messageType: MessageType): String =
    messageType match {
      case MessageType.IE304 => "CC304A"
      case MessageType.IE305 => "CC305A"
      case MessageType.IE316 => "CC316A"
      case MessageType.IE328 => "CC328A"
    }
}

trait XMLBuilder[R <: DecisionResponse, E <: Enrichment] {
  def buildXML(decision: Decision[R], enrichment: E): Elem

  protected final def getCUSOFFFENT730(officeOfFirstEntry: OfficeOfFirstEntry): Elem =
    //@formatter:off
    <CUSOFFFENT730>
      <RefNumCUSOFFFENT731>{officeOfFirstEntry.reference}</RefNumCUSOFFFENT731>
      <ExpDatOfArrFIRENT733>{getDateTimeInXSDFormat(officeOfFirstEntry.expectedDateTimeOfArrival)}</ExpDatOfArrFIRENT733>
    </CUSOFFFENT730>
  //@formatter:on

  protected final def getGOOITEGDS(enrichment: AcceptanceEnrichment): Seq[Node] =
    for {
      goodsItems <- enrichment.payload.goods.goodsItems.toSeq
      item       <- goodsItems
    } yield {
      //@formatter:off
      <GOOITEGDS>
        <IteNumGDS7>{item.itemNumber}</IteNumGDS7>
        {for (value <- item.commercialReferenceNumber.toSeq) yield <ComRefNumGIM1>{value}</ComRefNumGIM1>}
        { getPRODOCDC2( item) }
        { getCONNR2(item) }
        { getIDEMEATRAGI970( item) }
      </GOOITEGDS>
      //@formatter:on
    }

  private def getPRODOCDC2(item: GoodsItem): Seq[Node] =
    for {
      documents <- item.documents.toSeq
      document  <- documents
    } yield {
      //@formatter:off
      <PRODOCDC2>
        <DocTypDC21>{document.`type`}</DocTypDC21>
        <DocRefDC23>{document.reference}</DocRefDC23>
        {for (value <- document.language.toSeq) yield <DocRefDCLNG>{value}</DocRefDCLNG>}
      </PRODOCDC2>
      //@formatter:on
    }

  private def getCONNR2(item: GoodsItem): Seq[Node] =
    for {
      containers <- item.containers.toSeq
      container  <- containers
    } yield {
      //@formatter:off
      <CONNR2>
        <ConNumNR21>{container.containerNumber}</ConNumNR21>
      </CONNR2>
      //@formatter:on
    }

  private def getIDEMEATRAGI970(item: GoodsItem): Seq[Node] =
    for {
      identities <- item.identityOfMeansOfCrossingBorder.toSeq
      identity   <- identities
    } yield {
      //@formatter:off
      <IDEMEATRAGI970>
        {for (value <- identity.nationality.toSeq) yield <NatIDEMEATRAGI973>{value}</NatIDEMEATRAGI973>}
        <IdeMeaTraGIMEATRA971>{identity.identity}</IdeMeaTraGIMEATRA971>
        {for  ( value <- identity.language.toSeq) yield <IdeMeaTraGIMEATRA972LNG>{value}</IdeMeaTraGIMEATRA972LNG>
        }
      </IDEMEATRAGI970>
      //@formatter:on
    }

  protected final def getTRACARENT601(enrichment: AcceptanceEnrichment): Seq[Node] =
    for (carrier <- enrichment.payload.parties.carrier.toSeq) yield {
      //@formatter:off
      <TRACARENT601>
        { for (value <- carrier.name.toSeq) yield <NamTRACARENT604>{ value } </NamTRACARENT604>}
        { for ( address <- carrier.address.toSeq) yield {
        <StrNumTRACARENT607>{address.streetAndNumber}</StrNumTRACARENT607>
          <PstCodTRACARENT606>{address.postalCode}</PstCodTRACARENT606>
          <CtyTRACARENT603>{address.city}</CtyTRACARENT603>
          <CouCodTRACARENT605>{address.countryCode}</CouCodTRACARENT605>
      }
        }
        { for (value <- carrier.language.toSeq)  yield <TRACARENT601LNG>{ value }</TRACARENT601LNG> }
        { for (value <- carrier.eori.toSeq) yield <TINTRACARENT602>{ value }</TINTRACARENT602> }
      </TRACARENT601>
      //@formatter:on
    }

  protected final def getTRAREP(representative: Trader): Seq[Node] =
    //@formatter:off
    <TRAREP>
        { for (value <- representative.name.toSeq) yield <NamTRE1>{ value } </NamTRE1>}
        { for ( address <- representative.address.toSeq) yield {
        <StrAndNumTRE1>{address.streetAndNumber}</StrAndNumTRE1>
          <PosCodTRE1>{address.postalCode}</PosCodTRE1>
          <CitTRE1>{address.city}</CitTRE1>
          <CouCodTRE1>{address.countryCode}</CouCodTRE1>
      }
        }
        { for (value <- representative.language.toSeq)  yield <TRAREPLNG>{ value }</TRAREPLNG> }
        { for (value <- representative.eori.toSeq) yield <TINTRE1>{ value }</TINTRE1> }
      </TRAREP>
  //@formatter:on

  protected final def getPERLODSUMDEC(declarant: Trader): Seq[Node] =
    //@formatter:off
    <PERLODSUMDEC>
      { for (value <- declarant.name.toSeq) yield <NamPLD1>{value}</NamPLD1> }
      { for (address <- declarant.address.toSeq) yield {
      <StrAndNumPLD1>{address.streetAndNumber}</StrAndNumPLD1>
        <PosCodPLD1>{address.postalCode}</PosCodPLD1>
        <CitPLD1>{address.city}</CitPLD1>
        <CouCodPLD1>{address.countryCode}</CouCodPLD1>
    }
      }
      { for (value <- declarant.language.toSeq) yield <PERLODSUMDECLNG>{value}</PERLODSUMDECLNG> }
      { for (value <- declarant.eori.toSeq) yield <TINPLD1>{value}</TINPLD1>
      }
    </PERLODSUMDEC>
  //@formatter:on
}

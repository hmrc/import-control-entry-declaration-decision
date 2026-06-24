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

package uk.gov.hmrc.entrydeclarationdecision.models.enrichment.acceptance

import java.time.Instant

import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary.*
import uk.gov.hmrc.entrydeclarationdecision.models.PropertyCheckSupport
import uk.gov.hmrc.entrydeclarationdecision.models.decision.MessageType
import uk.gov.hmrc.entrydeclarationdecision.models.enrichment.{Address, Trader}

trait ArbitraryAcceptanceEnrichment extends PropertyCheckSupport {

  given arbitraryDocument: Arbitrary[Document] = Arbitrary(
    for {
      documentType      <- arbitrary[String]
      documentReference <- arbitrary[String]
      language          <- arbitrary[Option[String]]
    } yield Document(documentType, documentReference, language)
  )

  given arbitraryAmendment: Arbitrary[Amendment] = Arbitrary(
    for {
      dateTime <- arbitrary[Instant]
    } yield Amendment(dateTime)
  )

  given arbitraryAddress: Arbitrary[Address] = Arbitrary(
    for {
      streetAndNumber <- arbitrary[String]
      city            <- arbitrary[String]
      postalCode      <- arbitrary[String]
      countryCode     <- arbitrary[String]
    } yield Address(streetAndNumber, city, postalCode, countryCode)
  )

  given arbitraryIdentityOfMeansOfCrossingBorder: Arbitrary[IdentityOfMeansOfCrossingBorder] = Arbitrary(
    for {
      nationality <- arbitrary[Option[String]]
      identity    <- arbitrary[String]
      language    <- arbitrary[Option[String]]
    } yield IdentityOfMeansOfCrossingBorder(nationality, identity, language)
  )

  given arbitraryContainer: Arbitrary[Container] = Arbitrary(
    for {
      containerNumber <- arbitrary[String]
    } yield Container(containerNumber)
  )

  given arbitraryGoodItem: Arbitrary[GoodsItem] = Arbitrary(
    for {
      itemNumber                      <- arbitrary[String]
      commercialReferenceNumber       <- arbitrary[Option[String]]
      documents                       <- arbitrary[Option[Seq[Document]]]
      containers                      <- arbitrary[Option[Seq[Container]]]
      identityOfMeansOfCrossingBorder <- arbitrary[Option[Seq[IdentityOfMeansOfCrossingBorder]]]
    } yield GoodsItem(itemNumber, commercialReferenceNumber, documents, containers, identityOfMeansOfCrossingBorder)
  )

  given arbitraryGoods: Arbitrary[Goods] = Arbitrary(
    for {
      goodsItems <- arbitrary[Option[Seq[GoodsItem]]]
    } yield Goods(goodsItems)
  )

  given arbitraryTrader: Arbitrary[Trader] = Arbitrary(
    for {
      name     <- arbitrary[Option[String]]
      address  <- arbitrary[Option[Address]]
      language <- arbitrary[Option[String]]
      eori     <- arbitrary[String]
    } yield Trader(name, address, language, Some(eori))
  )

  given arbitraryParties: Arbitrary[Parties] = Arbitrary(
    for {
      declarant      <- arbitrary[Trader]
      representative <- arbitrary[Option[Trader]]
      carrier        <- arbitrary[Option[Trader]]
    } yield Parties(declarant, representative, carrier)
  )

  given arbitraryDeclaration: Arbitrary[Declaration] = Arbitrary(
    for {
      officeOfLodgement <- arbitrary[Option[String]]
    } yield Declaration(officeOfLodgement)
  )

  given arbitraryOfficeOfFirstEntry: Arbitrary[OfficeOfFirstEntry] = Arbitrary(
    for {
      reference                 <- arbitrary[String]
      expectedDateTimeOfArrival <- arbitrary[Instant]
    } yield OfficeOfFirstEntry(reference, expectedDateTimeOfArrival)
  )

  given arbitraryItinerary: Arbitrary[Itinerary] = Arbitrary(
    for {
      modeOfTransportAtBorder         <- arbitrary[String]
      identityOfMeansOfCrossingBorder <- arbitrary[Option[IdentityOfMeansOfCrossingBorder]]
      commercialReferenceNumber       <- arbitrary[Option[String]]
      conveyanceReference             <- arbitrary[Option[String]]
      officeOfFirstEntry              <- arbitrary[OfficeOfFirstEntry]
    } yield
      Itinerary(
        modeOfTransportAtBorder,
        identityOfMeansOfCrossingBorder,
        commercialReferenceNumber,
        conveyanceReference,
        officeOfFirstEntry)
  )

  given arbitraryEntrySummaryDeclaration(
    using messageType: MessageType): Arbitrary[EntrySummaryDeclaration] = {

    val declaration =
      for {
        declaration <- arbitrary[Declaration]
        parties     <- arbitrary[Parties]
        goods       <- arbitrary[Goods]
        itinerary   <- arbitrary[Itinerary]
      } yield EntrySummaryDeclaration(Some(declaration), parties, goods, itinerary, None)

    val amendment = for {
      parties   <- arbitrary[Parties]
      goods     <- arbitrary[Goods]
      itinerary <- arbitrary[Itinerary]
      amendment <- arbitrary[Amendment]
    } yield EntrySummaryDeclaration(None, parties, goods, itinerary, Some(amendment))

    Arbitrary(messageType match {
      case MessageType.IE316 | MessageType.IE328 => declaration
      case MessageType.IE305 | MessageType.IE304 => amendment
    })
  }

  given arbitraryRejectionEnrichment(using messageType: MessageType): Arbitrary[AcceptanceEnrichment] =
    Arbitrary(
      for {
        eisSubmissionDateTime <- arbitrary[Option[Instant]]
        payload               <- arbitrary[EntrySummaryDeclaration]
      } yield AcceptanceEnrichment(eisSubmissionDateTime, payload)
    )
}

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

package uk.gov.hmrc.entrydeclarationdecision.models.enrichment.rejection

import java.time.Instant

import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary._
import uk.gov.hmrc.entrydeclarationdecision.models.PropertyCheckSupport
import uk.gov.hmrc.entrydeclarationdecision.models.enrichment.{Address, Trader}

trait ArbitraryAmendmentRejectionEnrichment extends PropertyCheckSupport {

  implicit val arbitraryAmendment: Arbitrary[Amendment] = Arbitrary(
    for {
      movementReferenceNumber <- arbitrary[String]
      dateTime                <- arbitrary[Instant]
    } yield Amendment(movementReferenceNumber, dateTime)
  )

  implicit val arbitraryAddress: Arbitrary[Address] = Arbitrary(
    for {
      streetAndNumber <- arbitrary[String]
      city            <- arbitrary[String]
      postalCode      <- arbitrary[String]
      countryCode     <- arbitrary[String]
    } yield Address(streetAndNumber, city, postalCode, countryCode)
  )

  implicit val arbitraryTrader: Arbitrary[Trader] = Arbitrary(
    for {
      name     <- arbitrary[Option[String]]
      address  <- arbitrary[Option[Address]]
      language <- arbitrary[Option[String]]
      eori     <- arbitrary[String]
    } yield Trader(name, address, language, Some(eori))
  )

  implicit val arbitraryParties: Arbitrary[Parties] = Arbitrary(
    for {
      declarant      <- arbitrary[Trader]
      representative <- arbitrary[Option[Trader]]
    } yield Parties(declarant, representative)
  )

  implicit val arbitraryOfficeOfFirstEntry: Arbitrary[OfficeOfFirstEntry] = Arbitrary(
    for {
      reference <- arbitrary[String]
    } yield OfficeOfFirstEntry(reference)
  )

  implicit val arbitraryItinerary: Arbitrary[Itinerary] = Arbitrary(
    for {
      officeOfFirstEntry <- arbitrary[OfficeOfFirstEntry]
    } yield Itinerary(officeOfFirstEntry)
  )

  implicit def arbitraryEntrySummaryDeclaration: Arbitrary[EntrySummaryDeclaration] =
    Arbitrary(for {
      parties   <- arbitrary[Parties]
      itinerary <- arbitrary[Itinerary]
      amendment <- arbitrary[Amendment]
    } yield EntrySummaryDeclaration(parties, itinerary, amendment))

  implicit def arbitraryRejectionEnrichment: Arbitrary[AmendmentRejectionEnrichment] = Arbitrary(
    for {
      eisSubmissionDateTime <- arbitrary[Option[Instant]]
      payload               <- arbitrary[EntrySummaryDeclaration]
    } yield AmendmentRejectionEnrichment(eisSubmissionDateTime, payload)
  )
}

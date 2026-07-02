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

package uk.gov.hmrc.entrydeclarationdecision.models.enrichment.rejection

import java.time.Instant

import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary.*
import uk.gov.hmrc.entrydeclarationdecision.models.PropertyCheckSupport
import uk.gov.hmrc.entrydeclarationdecision.models.enrichment.{Address, Trader}

trait ArbitraryAmendmentRejectionEnrichment extends PropertyCheckSupport {

  given arbitraryAmendment: Arbitrary[Amendment] = Arbitrary(
    for {
      movementReferenceNumber <- arbitrary[String]
      dateTime                <- arbitrary[Instant]
    } yield Amendment(movementReferenceNumber, dateTime)
  )

  given arbitraryAddress: Arbitrary[Address] = Arbitrary(
    for {
      streetAndNumber <- arbitrary[String]
      city            <- arbitrary[String]
      postalCode      <- arbitrary[String]
      countryCode     <- arbitrary[String]
    } yield Address(streetAndNumber, city, postalCode, countryCode)
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
    } yield Parties(declarant, representative)
  )

  given arbitraryOfficeOfFirstEntry: Arbitrary[OfficeOfFirstEntry] = Arbitrary(
    for {
      reference <- arbitrary[String]
    } yield OfficeOfFirstEntry(reference)
  )

  given arbitraryItinerary: Arbitrary[Itinerary] = Arbitrary(
    for {
      officeOfFirstEntry <- arbitrary[OfficeOfFirstEntry]
    } yield Itinerary(officeOfFirstEntry)
  )

  given arbitraryEntrySummaryDeclaration: Arbitrary[EntrySummaryDeclaration] =
    Arbitrary(for {
      parties   <- arbitrary[Parties]
      itinerary <- arbitrary[Itinerary]
      amendment <- arbitrary[Amendment]
    } yield EntrySummaryDeclaration(parties, itinerary, amendment))

  given arbitraryRejectionEnrichment: Arbitrary[AmendmentRejectionEnrichment] = Arbitrary(
    for {
      eisSubmissionDateTime <- arbitrary[Option[Instant]]
      payload               <- arbitrary[EntrySummaryDeclaration]
    } yield AmendmentRejectionEnrichment(eisSubmissionDateTime, payload)
  )
}

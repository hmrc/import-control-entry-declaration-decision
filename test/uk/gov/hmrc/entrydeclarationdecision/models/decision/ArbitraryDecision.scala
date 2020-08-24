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

import java.time.Instant

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}
import uk.gov.hmrc.entrydeclarationdecision.models.PropertyCheckSupport

trait ArbitraryDecision extends PropertyCheckSupport {

  implicit val messageType: Arbitrary[MessageType] = Arbitrary(
    Gen
      .oneOf(MessageType.IE304, MessageType.IE305, MessageType.IE316, MessageType.IE328))

  implicit val arbitraryDecisionMetaData: Arbitrary[DecisionMetadata] = Arbitrary(
    for {
      senderEORI            <- arbitrary[String]
      senderBranch          <- arbitrary[String]
      messageType           <- arbitrary[MessageType]
      messageIdentification <- arbitrary[String]
      preparationDateTime   <- arbitrary[Instant]
      receivedDateTime      <- arbitrary[Instant]
      correlationId         <- arbitrary[String]
      localReferenceNumber  <- arbitrary[String]
    } yield
      DecisionMetadata(
        senderEORI,
        senderBranch,
        messageType,
        messageIdentification,
        preparationDateTime,
        receivedDateTime,
        correlationId,
        Some(localReferenceNumber))
  )

  implicit val arbitraryAcceptanceDecisionResponse: Arbitrary[DecisionResponse.Acceptance] =
    Arbitrary(for {
      movementReferenceNumber <- arbitrary[String]
      acceptedDateTime      <- arbitrary[Instant]
    } yield DecisionResponse.Acceptance(movementReferenceNumber, acceptedDateTime))

  implicit val arbitraryRejectionDecisionResponse: Arbitrary[DecisionResponse.Rejection] =
    Arbitrary(for {
      functionalError   <- arbitrary[Seq[DecisionError]]
      rejectionDateTime <- arbitrary[Instant]
    } yield DecisionResponse.Rejection(functionalError, rejectionDateTime))

  implicit val arbitraryDecisionError: Arbitrary[DecisionError] = Arbitrary(for {
    errorType              <- arbitrary[String]
    errorPointer           <- arbitrary[String]
    errorReason            <- arbitrary[Option[String]]
    originalAttributeValue <- arbitrary[Option[String]]
  } yield DecisionError(errorType, errorPointer, errorReason, originalAttributeValue))

  implicit def arbitraryAcceptanceDecision(
    implicit messageType: MessageType): Arbitrary[Decision[DecisionResponse.Acceptance]] = Arbitrary(
    for {
      submissionId <- arbitrary[String]
      metadata     <- arbitrary[DecisionMetadata]
      response     <- arbitrary[DecisionResponse.Acceptance]
    } yield Decision(submissionId, metadata, response)
  )

  implicit def arbitraryRejectionDecision(
    implicit messageType: MessageType): Arbitrary[Decision[DecisionResponse.Rejection]] = Arbitrary(
    for {
      submissionId <- arbitrary[String]
      metadata     <- arbitrary[DecisionMetadata]
      response     <- arbitrary[DecisionResponse.Rejection]
    } yield Decision(submissionId, metadata, response)
  )
}

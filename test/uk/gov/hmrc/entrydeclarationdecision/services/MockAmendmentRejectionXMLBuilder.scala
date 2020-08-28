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

import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.entrydeclarationdecision.models.decision.{Decision, DecisionResponse}
import uk.gov.hmrc.entrydeclarationdecision.models.enrichment.rejection.AmendmentRejectionEnrichment

import scala.xml.Elem

trait MockAmendmentRejectionXMLBuilder extends MockFactory {
  val mockAmendmentRejectionXMLBuilder: AmendmentRejectionXMLBuilder = mock[AmendmentRejectionXMLBuilder]

  object MockAmendmentRejectionXMLBuilder {
    def buildXML(
      decision: Decision[DecisionResponse.Rejection],
      enrichment: AmendmentRejectionEnrichment): CallHandler[Elem] =
      (mockAmendmentRejectionXMLBuilder
        .buildXML(_: Decision[DecisionResponse.Rejection], _: AmendmentRejectionEnrichment))
        .expects(decision, enrichment)
  }

}

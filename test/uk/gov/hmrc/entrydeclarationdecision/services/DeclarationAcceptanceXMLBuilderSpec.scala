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

import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.libs.json.Json
import uk.gov.hmrc.entrydeclarationdecision.models.decision.DecisionResponse.Acceptance
import uk.gov.hmrc.entrydeclarationdecision.models.decision.{ArbitraryDecision, Decision, MessageType}
import uk.gov.hmrc.entrydeclarationdecision.models.enrichment.acceptance.{AcceptanceEnrichment, ArbitraryAcceptanceEnrichment}
import uk.gov.hmrc.entrydeclarationdecision.utils.{ResourceUtils, SchemaType, SchemaValidator}
import uk.gov.hmrc.play.test.UnitSpec

import scala.xml.{Utility, XML}

class DeclarationAcceptanceXMLBuilderSpec
    extends UnitSpec
    with ScalaCheckDrivenPropertyChecks
    with ArbitraryDecision
    with ArbitraryAcceptanceEnrichment {

  val xmlBuilder = new DeclarationAcceptanceXMLBuilder

  "AcceptanceXMLBuilder" should {
    "return XML formatted correctly" when {
      "an acceptance decision is supplied" in {
        val enrichmentJson = ResourceUtils.withInputStreamFor("jsons/DeclarationAcceptanceEnrichment.json")(Json.parse)
        val enrichment     = enrichmentJson.as[AcceptanceEnrichment]
        val decisionJson   = ResourceUtils.withInputStreamFor("jsons/DeclarationAcceptanceDecision.json")(Json.parse)
        val decision       = decisionJson.as[Decision[Acceptance]]

        val expected = ResourceUtils.withInputStreamFor("xmls/DeclarationAcceptanceXML.xml")(XML.load)

        Utility.trim(xmlBuilder.buildXML(decision, enrichment)) shouldBe Utility.trim(expected)
      }

      "with the correct namespace and prefix" in {

        val enrichmentJson = ResourceUtils.withInputStreamFor("jsons/DeclarationAcceptanceEnrichment.json")(Json.parse)
        val enrichment     = enrichmentJson.as[AcceptanceEnrichment]
        val decisionJson   = ResourceUtils.withInputStreamFor("jsons/DeclarationAcceptanceDecision.json")(Json.parse)
        val decision       = decisionJson.as[Decision[Acceptance]]

        val xml = xmlBuilder.buildXML(decision, enrichment)

        xml.namespace shouldBe "http://ics.dgtaxud.ec/CC328A"
        xml.prefix    shouldBe "cc3"
      }

      "an acceptance decision is supplied with all optional fields" in {

        val enrichmentJson =
          ResourceUtils.withInputStreamFor("jsons/DeclarationAcceptanceEnrichmentAllOptional.json")(Json.parse)
        val enrichment = enrichmentJson.as[AcceptanceEnrichment]
        val decisionJson =
          ResourceUtils.withInputStreamFor("jsons/DeclarationAcceptanceDecisionAllOptional.json")(Json.parse)
        val decision = decisionJson.as[Decision[Acceptance]]
        val expected = ResourceUtils.withInputStreamFor("xmls/DeclarationAcceptanceAllOptionalXML.xml")(XML.load)

        Utility.trim(xmlBuilder.buildXML(decision, enrichment)) shouldBe Utility.trim(expected)
      }
    }

    "generate schema valid XML for all inputs" in {
      val schemaValidator = new SchemaValidator

      implicit val messageType: MessageType = MessageType.IE328

      forAll { (decision: Decision[Acceptance], enrichment: AcceptanceEnrichment) =>
        val xml = xmlBuilder
          .buildXML(decision, enrichment)

        schemaValidator.validateSchema(SchemaType.CC328A, xml).allErrors.filterNot { ex =>
          // Ignore type related errors
          val msg = ex.toString
          msg.contains("cvc-pattern-valid") || msg.contains("cvc-type.3.1.3") || msg.contains("cvc-enumeration-valid")
        } shouldBe Nil
      }
    }
  }
}

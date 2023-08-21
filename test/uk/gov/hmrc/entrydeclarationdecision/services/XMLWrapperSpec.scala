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

import org.apache.commons.lang3.StringUtils
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatestplus.play.PlaySpec

import scala.xml.{Elem, Utility}

class XMLWrapperSpec extends PlaySpec {
  val xmlWrapper = new XMLWrapper
  val correlationId = "corrid"

  "XMLWrapper wrapXml" must {
    "return wrapped xml for an acceptance decision" in {
      val xml: Elem = <cc3:abc xmlns:cc3="http://ics.dgtaxud.ec/CC304A">abcd</cc3:abc>
      val expected: Elem =
      //@formatter:off
       <outcomeResponse xmlns:cc3="http://ics.dgtaxud.ec/CC304A">
         <response>
          <cc3:abc>abcd</cc3:abc>
         </response>
         <acknowledgement method='DELETE' href='/customs/imports/outcomes/corrid'/>
       </outcomeResponse>
     //@formatter:on

      val wrappedXml = xmlWrapper.wrapXml(correlationId, xml)

      Utility.trim(wrappedXml) shouldBe Utility.trim(expected)
      wrappedXml.getNamespace("cc3") shouldBe "http://ics.dgtaxud.ec/CC304A"
    }

    "return wrapped xml for an rejection decision" in {
      val xml: Elem = <cc3:abc xmlns:cc3="http://ics.dgtaxud.ec/CC305A">abcd</cc3:abc>
      val expected: Elem =
      //@formatter:off
       <outcomeResponse xmlns:cc3="http://ics.dgtaxud.ec/CC305A">
         <response>
          <cc3:abc>abcd</cc3:abc>
         </response>
         <acknowledgement method='DELETE' href='/customs/imports/outcomes/corrid'/>
       </outcomeResponse>
     //@formatter:on

      val wrappedXml = xmlWrapper.wrapXml(correlationId, xml)

      Utility.trim(wrappedXml) shouldBe Utility.trim(expected)
      wrappedXml.getNamespace("cc3") shouldBe "http://ics.dgtaxud.ec/CC305A"
    }
    "remove the namespace of the xml" in {
      val xml: Elem = <cc3:abc xmlns:cc3="http://ics.dgtaxud.ec/CC305A">
        <abc2>2</abc2> <abc3>3</abc3>
      </cc3:abc>
      val xmlWrapper = new XMLWrapper
      val wrapped = xmlWrapper.wrapXml(correlationId, xml)
      StringUtils.countMatches(wrapped.toString(), xml.namespace) shouldBe 1
    }
  }
}

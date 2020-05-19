/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.entrydeclarationdecision.services

import org.apache.commons.lang3.StringUtils
import uk.gov.hmrc.play.test.UnitSpec

import scala.xml.{Elem, Utility}

class XMLWrapperSpec extends UnitSpec {
  val xmlWrapper = new XMLWrapper
  val correlationId = "corrid"

  "XMLWrapper wrapXml" should {
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

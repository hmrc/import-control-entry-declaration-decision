/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.entrydeclarationdecision.services

import scala.xml.Elem

class XMLWrapper {
  def wrapXml(correlationId: String, xml: Elem): Elem = {
    //@formatter:off
    <outcomeResponse xmlns:cc3={xml.namespace}>
      <response>
        {xml}
      </response>
      <acknowledgement method='DELETE' href={s"/customs/imports/outcomes/${correlationId}"}/>
    </outcomeResponse>
    //@formatter:on
  }
}

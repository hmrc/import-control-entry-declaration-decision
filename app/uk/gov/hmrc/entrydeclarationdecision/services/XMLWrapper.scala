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

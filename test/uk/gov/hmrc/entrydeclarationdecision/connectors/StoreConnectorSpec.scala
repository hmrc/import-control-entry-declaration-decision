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

package uk.gov.hmrc.entrydeclarationdecision.connectors

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.http.Fault
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.test.{DefaultAwaitTimeout, FutureAwaits, Injecting}
import play.api.{Application, Environment, Mode}
import uk.gov.hmrc.entrydeclarationdecision.config.MockAppConfig
import uk.gov.hmrc.entrydeclarationdecision.logging.LoggingContext
import uk.gov.hmrc.entrydeclarationdecision.models.ErrorCode
import uk.gov.hmrc.entrydeclarationdecision.models.enrichment.acceptance.AcceptanceEnrichment
import uk.gov.hmrc.entrydeclarationdecision.models.enrichment.rejection.{AmendmentRejectionEnrichment, DeclarationRejectionEnrichment}
import uk.gov.hmrc.entrydeclarationdecision.utils.ResourceUtils
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import scala.concurrent.ExecutionContext.Implicits.global

class StoreConnectorSpec
    extends WordSpec
    with Matchers
    with FutureAwaits
    with DefaultAwaitTimeout
    with BeforeAndAfterAll
    with GuiceOneAppPerSuite
    with Injecting
    with MockAppConfig {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  override lazy val app: Application = new GuiceApplicationBuilder()
    .in(Environment.simple(mode = Mode.Dev))
    .configure("metrics.enabled" -> "false")
    .build()

  val httpClient: HttpClient = inject[HttpClient]

  implicit val lc: LoggingContext = LoggingContext("eori", "corrId", "subId", Some("mrn"))

  private val wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())

  var port: Int = _

  override def beforeAll(): Unit = {
    wireMockServer.start()
    port = wireMockServer.port()
  }

  override def afterAll(): Unit =
    wireMockServer.stop()

  //Doesn't matter what enrichments are.
  val acceptanceEnrichmentJson: JsValue = ResourceUtils
    .withInputStreamFor("jsons/DeclarationAcceptanceEnrichmentAllOptional.json")(Json.parse)
  val acceptanceEnrichment: AcceptanceEnrichment =
    acceptanceEnrichmentJson.as[AcceptanceEnrichment]

  val amendmentRejectionEnrichmentJson: JsValue = ResourceUtils
    .withInputStreamFor("jsons/AmendmentRejectionEnrichmentAllOptional.json")(Json.parse)
  val amendmentRejectionEnrichment: AmendmentRejectionEnrichment =
    amendmentRejectionEnrichmentJson.as[AmendmentRejectionEnrichment]

  private val declarationRejectionEnrichmentJson: JsValue = ResourceUtils
    .withInputStreamFor("jsons/DeclarationRejectionEnrichment.json")(Json.parse)
  val declarationRejectionEnrichment: DeclarationRejectionEnrichment =
    declarationRejectionEnrichmentJson
      .as[DeclarationRejectionEnrichment]

  val submissionId = "submissionId"

  class Test {
    MockAppConfig.storeHost.returns(s"http://localhost:$port")
    val connector = new StoreConnector(httpClient, mockAppConfig)

    def stubRequest(url: String, responseStatus: Int): StubMapping =
      wireMockServer.stubFor(
        get(urlPathEqualTo(url))
          .willReturn(aResponse()
            .withStatus(responseStatus)))

    def stubPutRequest(url: String, responseStatus: Int): StubMapping =
      wireMockServer.stubFor(
        put(urlPathEqualTo(url))
          .willReturn(aResponse()
            .withStatus(responseStatus)))

    def stubConnectionFault(url: String): StubMapping =
      wireMockServer.stubFor(
        get(urlPathEqualTo(url))
          .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)))
  }

  "StoreConnector.getAcceptanceEnrichment" when {

    val declarationUrl =
      s"/import-control/declaration/acceptance-enrichment/$submissionId"
    val amendmentUrl =
      s"/import-control/amendment/acceptance-enrichment/$submissionId"

    "called for a declaration" when {
      "store responds 200 Ok" must {
        "return Right with the enrichment" in new Test {
          wireMockServer.stubFor(
            get(urlPathEqualTo(declarationUrl))
              .willReturn(aResponse()
                .withBody(acceptanceEnrichmentJson.toString)
                .withStatus(OK)))

          await(connector.getAcceptanceEnrichment(submissionId, amendment = false)) shouldBe Right(acceptanceEnrichment)
        }
      }
    }

    "called for an amendment" when {
      "store responds 200 Ok" must {
        "return Right with the enrichment" in new Test {
          wireMockServer.stubFor(
            get(urlPathEqualTo(amendmentUrl))
              .willReturn(aResponse()
                .withBody(acceptanceEnrichmentJson.toString)
                .withStatus(OK)))

          await(connector.getAcceptanceEnrichment(submissionId, amendment = true)) shouldBe Right(acceptanceEnrichment)
        }
      }
    }

    "store responds 404" must {
      "return Left with ErrorCode.NoSubmission" in new Test {
        stubRequest(declarationUrl, NOT_FOUND)

        await(connector.getAcceptanceEnrichment(submissionId, amendment = false)) shouldBe Left(ErrorCode.NoSubmission)
      }
    }

    "store responds 4xx" must {
      "return Left with ErrorCode.ConnectorError" in new Test {
        stubRequest(declarationUrl, BAD_REQUEST)

        await(connector.getAcceptanceEnrichment(submissionId, amendment = false)) shouldBe Left(
          ErrorCode.ConnectorError)
      }
    }

    "store responds 5xx" must {
      "return Left with ErrorCode.ConnectorError" in new Test {
        stubRequest(declarationUrl, INTERNAL_SERVER_ERROR)

        await(connector.getAcceptanceEnrichment(submissionId, amendment = false)) shouldBe Left(
          ErrorCode.ConnectorError)
      }
    }

    "unable to connect" must {
      "return Left with ErrorCode.ConnectorError" in new Test {
        stubConnectionFault(declarationUrl)

        await(connector.getAcceptanceEnrichment(submissionId, amendment = false)) shouldBe Left(
          ErrorCode.ConnectorError)
      }
    }
  }

  "StoreConnector.getAmendmentRejectionEnrichment" when {

    val amendmentUrl =
      s"/import-control/amendment/rejection-enrichment/$submissionId"

    "store responds 200 Ok" must {
      "return Right with the enrichment" in new Test {
        wireMockServer.stubFor(
          get(urlPathEqualTo(amendmentUrl))
            .willReturn(
              aResponse()
                .withBody(amendmentRejectionEnrichmentJson.toString)
                .withStatus(OK)))

        await(connector.getAmendmentRejectionEnrichment(submissionId)) shouldBe Right(amendmentRejectionEnrichment)
      }
    }

    "store responds 404" must {
      "return Left with ErrorCode.NoSubmission" in new Test {
        stubRequest(amendmentUrl, NOT_FOUND)

        await(connector.getAmendmentRejectionEnrichment(submissionId)) shouldBe Left(ErrorCode.NoSubmission)
      }
    }

    "store responds 4xx" must {
      "return Left with ErrorCode.ConnectorError" in new Test {
        stubRequest(amendmentUrl, BAD_REQUEST)

        await(connector.getAmendmentRejectionEnrichment(submissionId)) shouldBe Left(ErrorCode.ConnectorError)
      }
    }

    "store responds 5xx" must {
      "return Left with ErrorCode.ConnectorError" in new Test {
        stubRequest(amendmentUrl, INTERNAL_SERVER_ERROR)

        await(connector.getAmendmentRejectionEnrichment(submissionId)) shouldBe Left(ErrorCode.ConnectorError)
      }
    }

    "unable to connect" must {
      "return Left with ErrorCode.ConnectorError" in new Test {
        stubConnectionFault(amendmentUrl)

        await(connector.getAmendmentRejectionEnrichment(submissionId)) shouldBe Left(ErrorCode.ConnectorError)
      }
    }
  }

  "StoreConnector.getDeclarationRejectionEnrichment" when {

    val amendmentUrl =
      s"/import-control/declaration/rejection-enrichment/$submissionId"

    "store responds 200 Ok" must {
      "return Right with the enrichment" in new Test {
        wireMockServer.stubFor(
          get(urlPathEqualTo(amendmentUrl))
            .willReturn(
              aResponse()
                .withBody(declarationRejectionEnrichmentJson.toString)
                .withStatus(OK)))

        await(connector.getDeclarationRejectionEnrichment(submissionId)) shouldBe Right(declarationRejectionEnrichment)
      }
    }

    "store responds 404" must {
      "return Left with ErrorCode.NoSubmission" in new Test {
        stubRequest(amendmentUrl, NOT_FOUND)

        await(connector.getDeclarationRejectionEnrichment(submissionId)) shouldBe Left(ErrorCode.NoSubmission)
      }
    }

    "store responds 4xx" must {
      "return Left with ErrorCode.ConnectorError" in new Test {
        stubRequest(amendmentUrl, BAD_REQUEST)

        await(connector.getDeclarationRejectionEnrichment(submissionId)) shouldBe Left(ErrorCode.ConnectorError)
      }
    }

    "store responds 5xx" must {
      "return Left with ErrorCode.ConnectorError" in new Test {
        stubRequest(amendmentUrl, INTERNAL_SERVER_ERROR)

        await(connector.getDeclarationRejectionEnrichment(submissionId)) shouldBe Left(ErrorCode.ConnectorError)
      }
    }

    "unable to connect" must {
      "return Left with ErrorCode.ConnectorError" in new Test {
        stubConnectionFault(amendmentUrl)

        await(connector.getDeclarationRejectionEnrichment(submissionId)) shouldBe Left(ErrorCode.ConnectorError)
      }
    }
  }

  "StoreConnector.setShortTtl" when {
    val url: String = s"/import-control/housekeeping/submissionid/$submissionId"
    "successful" must {
      "return true" in new Test {
        stubPutRequest(url, NO_CONTENT)
        await(connector.setShortTtl(submissionId)) shouldBe true
      }
    }
    "unsuccessful" must {
      "return false" in new Test {
        stubPutRequest(url, NOT_FOUND)
        await(connector.setShortTtl(submissionId)) shouldBe false
      }
    }
  }
}

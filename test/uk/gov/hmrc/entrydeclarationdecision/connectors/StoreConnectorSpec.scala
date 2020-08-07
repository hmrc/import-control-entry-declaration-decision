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
import play.api.libs.json.Json
import play.api.test.{DefaultAwaitTimeout, FutureAwaits, Injecting}
import play.api.{Application, Environment, Mode}
import uk.gov.hmrc.entrydeclarationdecision.config.MockAppConfig
import uk.gov.hmrc.entrydeclarationdecision.logging.LoggingContext
import uk.gov.hmrc.entrydeclarationdecision.models.ErrorCode
import uk.gov.hmrc.entrydeclarationdecision.models.enrichment.acceptance.AcceptanceEnrichment
import uk.gov.hmrc.entrydeclarationdecision.models.enrichment.rejection.AmendmentRejectionEnrichment
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
  val acceptanceEnrichment: AcceptanceEnrichment =
    ResourceUtils
      .withInputStreamFor("jsons/DeclarationAcceptanceEnrichmentAllOptional.json")(Json.parse)
      .as[AcceptanceEnrichment]

  val amendmentRejectionEnrichment: AmendmentRejectionEnrichment =
    ResourceUtils
      .withInputStreamFor("jsons/AmendmentRejectionEnrichmentAllOptional.json")(Json.parse)
      .as[AmendmentRejectionEnrichment]

  val submissionId = "submissionId"

  class Test {
    MockAppConfig.storeHost.returns(s"http://localhost:$port")
    val connector = new StoreConnector(httpClient, mockAppConfig)

    def stubRequest(url: String, responseStatus: Int): StubMapping =
      wireMockServer.stubFor(
        get(urlPathEqualTo(url))
          .willReturn(aResponse()
            .withStatus(responseStatus)))

    def stubConnectionFault(url: String): StubMapping =
      wireMockServer.stubFor(
        get(urlPathEqualTo(url))
          .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)))
  }

  "StoreConnector.getAcceptanceEnrichment for a declaration" when {

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
                .withBody(Json.toJson(acceptanceEnrichment).toString)
                .withStatus(OK)))

          val result: Either[ErrorCode, AcceptanceEnrichment] =
            await(connector.getAcceptanceEnrichment(submissionId, amendment = false))
          result shouldBe Right(acceptanceEnrichment)
        }
      }
    }

    "called for an amendment" when {
      "store responds 200 Ok" must {
        "return Right with the enrichment" in new Test {
          wireMockServer.stubFor(
            get(urlPathEqualTo(amendmentUrl))
              .willReturn(aResponse()
                .withBody(Json.toJson(acceptanceEnrichment).toString)
                .withStatus(OK)))

          val result: Either[ErrorCode, AcceptanceEnrichment] =
            await(connector.getAcceptanceEnrichment(submissionId, amendment = true))
          result shouldBe Right(acceptanceEnrichment)
        }
      }
    }

    "store responds 404" must {
      "return Left with ErrorCode.NoSubmission" in new Test {
        stubRequest(declarationUrl, NOT_FOUND)

        val result: Either[ErrorCode, AcceptanceEnrichment] =
          await(connector.getAcceptanceEnrichment(submissionId, amendment = false))
        result shouldBe Left(ErrorCode.NoSubmission)
      }
    }

    "store responds 4xx" must {
      "return Left with ErrorCode.ConnectorError" in new Test {
        stubRequest(declarationUrl, BAD_REQUEST)

        val result: Either[ErrorCode, AcceptanceEnrichment] =
          await(connector.getAcceptanceEnrichment(submissionId, amendment = false))
        result shouldBe Left(ErrorCode.ConnectorError)
      }
    }

    "store responds 5xx" must {
      "return Left with ErrorCode.ConnectorError" in new Test {
        stubRequest(declarationUrl, INTERNAL_SERVER_ERROR)

        val result: Either[ErrorCode, AcceptanceEnrichment] =
          await(connector.getAcceptanceEnrichment(submissionId, amendment = false))
        result shouldBe Left(ErrorCode.ConnectorError)
      }
    }

    "unable to connect" must {
      "return Left with ErrorCode.ConnectorError" in new Test {
        stubConnectionFault(declarationUrl)

        val result: Either[ErrorCode, AcceptanceEnrichment] =
          await(connector.getAcceptanceEnrichment(submissionId, amendment = false))
        result shouldBe Left(ErrorCode.ConnectorError)
      }
    }
  }

  "StoreConnector.getAmendmentRejectionEnrichment for a declaration" when {

    val amendmentUrl =
      s"/import-control/amendment/rejection-enrichment/$submissionId"

    "called for a amendment" when {
      "store responds 200 Ok" must {
        "return Right with the enrichment" in new Test {
          wireMockServer.stubFor(
            get(urlPathEqualTo(amendmentUrl))
              .willReturn(aResponse()
                .withBody(Json.toJson(amendmentRejectionEnrichment).toString)
                .withStatus(OK)))

          val result: Either[ErrorCode, AmendmentRejectionEnrichment] =
            await(connector.getAmendmentRejectionEnrichment(submissionId))
          result shouldBe Right(amendmentRejectionEnrichment)
        }
      }
    }

    "store responds 404" must {
      "return Left with ErrorCode.NoSubmission" in new Test {
        stubRequest(amendmentUrl, NOT_FOUND)

        val result: Either[ErrorCode, AmendmentRejectionEnrichment] =
          await(connector.getAmendmentRejectionEnrichment(submissionId))
        result shouldBe Left(ErrorCode.NoSubmission)
      }
    }

    "store responds 4xx" must {
      "return Left with ErrorCode.ConnectorError" in new Test {
        stubRequest(amendmentUrl, BAD_REQUEST)

        val result: Either[ErrorCode, AmendmentRejectionEnrichment] =
          await(connector.getAmendmentRejectionEnrichment(submissionId))
        result shouldBe Left(ErrorCode.ConnectorError)
      }
    }

    "store responds 5xx" must {
      "return Left with ErrorCode.ConnectorError" in new Test {
        stubRequest(amendmentUrl, INTERNAL_SERVER_ERROR)

        val result: Either[ErrorCode, AmendmentRejectionEnrichment] =
          await(connector.getAmendmentRejectionEnrichment(submissionId))
        result shouldBe Left(ErrorCode.ConnectorError)
      }
    }

    "unable to connect" must {
      "return Left with ErrorCode.ConnectorError" in new Test {
        stubConnectionFault(amendmentUrl)

        val result: Either[ErrorCode, AmendmentRejectionEnrichment] =
          await(connector.getAmendmentRejectionEnrichment(submissionId))
        result shouldBe Left(ErrorCode.ConnectorError)
      }
    }
  }
}

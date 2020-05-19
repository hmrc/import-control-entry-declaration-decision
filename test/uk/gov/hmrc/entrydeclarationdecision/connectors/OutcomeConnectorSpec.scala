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

import java.time.ZonedDateTime

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
import play.api.libs.ws.WSClient
import play.api.test.{DefaultAwaitTimeout, FutureAwaits, Injecting}
import play.api.{Application, Environment, Mode}
import play.mvc.Http.HeaderNames._
import play.mvc.Http.MimeTypes
import uk.gov.hmrc.entrydeclarationdecision.config.MockAppConfig
import uk.gov.hmrc.entrydeclarationdecision.models.ErrorCode
import uk.gov.hmrc.entrydeclarationdecision.models.decision.MessageType
import uk.gov.hmrc.entrydeclarationdecision.models.outcome.Outcome

import scala.concurrent.ExecutionContext.Implicits.global

class OutcomeConnectorSpec
    extends WordSpec
    with Matchers
    with FutureAwaits
    with DefaultAwaitTimeout
    with BeforeAndAfterAll
    with GuiceOneAppPerSuite
    with Injecting
    with MockAppConfig {

  override lazy val app: Application = new GuiceApplicationBuilder()
    .in(Environment.simple(mode = Mode.Dev))
    .configure("metrics.enabled" -> "false")
    .build()

  val ws: WSClient = inject[WSClient]

  private val wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())

  var port: Int = _

  override def beforeAll(): Unit = {
    wireMockServer.start()
    port = wireMockServer.port()
  }

  override def afterAll(): Unit =
    wireMockServer.stop()

  class Test {
    MockAppConfig.outcomeHost.returns(s"http://localhost:$port")
    val connector = new OutcomeConnector(ws, mockAppConfig)

    val url = "/import-control/outcome"

    def stubRequest(responseStatus: Int): StubMapping =
      wireMockServer.stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.toJson(outcome).toString()))
          .withHeader(CONTENT_TYPE, equalTo(MimeTypes.JSON))
          .willReturn(aResponse()
            .withStatus(responseStatus)))

    def stubConnectionFault: StubMapping =
      wireMockServer.stubFor(
        post(urlPathEqualTo(url))
          .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)))
  }

  val outcome: Outcome = Outcome(
    "eori",
    "correlationId",
    "submissionId",
    ZonedDateTime.parse("2020-12-31T23:59:00Z"),
    MessageType.IE316,
    Some("TheMRN"),
    "outcomeXml"
  )

  "OutcomeConnector.save" when {
    "outcome responds 201 Created" must {
      "return Right" in new Test {
        stubRequest(CREATED)

        val result: Either[ErrorCode, Unit] = await(connector.send(outcome))
        result shouldBe Right(())
      }
    }

    "outcome responds 409 Conflict" must {
      "return Left with ErrorCode.DuplicateSubmission" in new Test {
        stubRequest(CONFLICT)

        val result: Either[ErrorCode, Unit] = await(connector.send(outcome))
        result shouldBe Left(ErrorCode.DuplicateSubmission)
      }
    }

    "outcome responds 4xx" must {
      "return Left with ErrorCode.ConnectorError" in new Test {
        stubRequest(BAD_REQUEST)

        val result: Either[ErrorCode, Unit] = await(connector.send(outcome))
        result shouldBe Left(ErrorCode.ConnectorError)
      }
    }

    "outcome responds 5xx" must {
      "return Left with ErrorCode.ConnectorError" in new Test {
        stubRequest(INTERNAL_SERVER_ERROR)

        val result: Either[ErrorCode, Unit] = await(connector.send(outcome))
        result shouldBe Left(ErrorCode.ConnectorError)
      }
    }

    "unable to connect" must {
      "return Left with ErrorCode.ConnectorError" in new Test {
        stubConnectionFault

        val result: Either[ErrorCode, Unit] = await(connector.send(outcome))
        result shouldBe Left(ErrorCode.ConnectorError)
      }
    }
  }
}

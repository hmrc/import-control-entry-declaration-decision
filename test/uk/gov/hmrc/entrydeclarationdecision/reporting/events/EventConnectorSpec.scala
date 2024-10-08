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

package uk.gov.hmrc.entrydeclarationdecision.reporting.events

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.http.Fault
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.{DefaultAwaitTimeout, FutureAwaits, Injecting}
import play.api.{Application, Environment, Mode}
import play.mvc.Http.Status.{BAD_REQUEST, CREATED}
import uk.gov.hmrc.entrydeclarationdecision.config.MockAppConfig
import uk.gov.hmrc.entrydeclarationdecision.logging.LoggingContext
import uk.gov.hmrc.entrydeclarationdecision.models.decision.MessageType
import uk.gov.hmrc.entrydeclarationdecision.utils.MockPagerDutyLogger
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.HttpClientV2

import scala.concurrent.ExecutionContext

class EventConnectorSpec
    extends PlaySpec
    with FutureAwaits
    with DefaultAwaitTimeout
    with BeforeAndAfterAll
    with GuiceOneAppPerSuite
    with Injecting
    with MockAppConfig
    with MockPagerDutyLogger {

  override lazy val app: Application = new GuiceApplicationBuilder()
    .in(Environment.simple(mode = Mode.Dev))
    .configure("metrics.enabled" -> "false")
    .build()

  val httpClient: HttpClientV2 = inject[HttpClientV2]

  implicit val hc: HeaderCarrier    = HeaderCarrier()
  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val lc: LoggingContext   = LoggingContext("eori", "corrId", "subId", Some("mrn"))

  private val wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())

  var port: Int = _

  override def beforeAll(): Unit = {
    wireMockServer.start()
    port = wireMockServer.port()
    WireMock.configureFor("localhost", port)
  }

  override def afterAll(): Unit =
    wireMockServer.stop()

  class Test {
    MockAppConfig.eventsHost returns s"http://localhost:$port"
    val url = "/import-control/event"

    val connector    = new EventConnectorImpl(httpClient, mockAppConfig, mockPagerDutyLogger)
    val submissionId = "743aa85b-5077-438f-8f30-01ab2a39d945"
    val event: Event = Event(
      EventCode.ENS_RESP,
      java.time.Instant.now(),
      submissionId,
      "eori",
      "correlationId",
      MessageType.IE316,
      None
    )

    def stubResponse(statusCode: Int): StubMapping =
      wireMockServer
        .stubFor(
          post(urlPathEqualTo(url))
            .withRequestBody(equalToJson(Json.toJson(event).toString))
            .willReturn(aResponse()
              .withHeader("Content-Type", "application/json")
              .withStatus(statusCode)))

  }

  "Calling .sendEvent" when {
    "events responds 201 (Created)" must {
      "return Future(Unit) and not log" in new Test {

        wireMockServer.stubFor(
          post(urlPathEqualTo(url))
            .withRequestBody(equalToJson(Json.toJson(event).toString))
            .willReturn(aResponse()
              .withHeader("Content-Type", "application/json")
              .withStatus(CREATED)))

        val result: Unit = await(connector.sendEvent(event))

        result shouldBe ()
      }
    }

    "events responds with 400" must {
      "return Future(Unit) and log" in new Test {
        stubResponse(BAD_REQUEST)
        val result: Unit = await(connector.sendEvent(event))

        result shouldBe ()

        MockPagerDutyLogger.logEventFailure.once()
      }
    }

    "exception thrown" must {
      "return Future(Unit) and log" in new Test {
        wireMockServer
          .stubFor(
            post(urlPathEqualTo(url))
              .withRequestBody(equalToJson(Json.toJson(event).toString))
              .willReturn(aResponse().withFault(Fault.MALFORMED_RESPONSE_CHUNK)))

        val result: Unit = await(connector.sendEvent(event))

        result shouldBe ()

        MockPagerDutyLogger.logEventError.once()
      }
    }
  }
}

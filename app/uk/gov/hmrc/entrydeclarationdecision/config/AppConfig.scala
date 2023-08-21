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

package uk.gov.hmrc.entrydeclarationdecision.config

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.{AppName, ServicesConfig}

import scala.concurrent.duration.{Duration, FiniteDuration}

trait AppConfig {

  def authBaseUrl: String

  def appName: String

  def auditingEnabled: Boolean

  def graphiteHost: String

  def eisInboundBearerToken: String

  def eventsHost: String

  def outcomeHost: String

  def storeHost: String

  def validateIncomingJson: Boolean

  def validateJsonToXMLTransformation: Boolean

  def longJourneyTime: FiniteDuration
}

@Singleton
class AppConfigImpl @Inject()(config: Configuration, servicesConfig: ServicesConfig) extends AppConfig {

  private final def getFiniteDuration(config: Configuration, path: String): FiniteDuration = {
    val string = config.get[String](path)

    Duration.create(string) match {
      case f: FiniteDuration => f
      case _                 => throw new RuntimeException(s"Not a finite duration '$string' for $path")
    }
  }

  val authBaseUrl: String = servicesConfig.baseUrl("auth")

  lazy val appName: String = AppName.fromConfiguration(config)

  val auditingEnabled: Boolean = config.get[Boolean]("auditing.enabled")

  val graphiteHost: String = config.get[String]("microservice.metrics.graphite.host")

  lazy val eisInboundBearerToken: String =
    config.get[String]("microservice.services.import-control-entry-declaration-eis.inboundBearerToken")

  lazy val eventsHost: String = servicesConfig.baseUrl("import-control-entry-declaration-events")

  lazy val outcomeHost: String = servicesConfig.baseUrl("import-control-entry-declaration-outcome")

  lazy val storeHost: String = servicesConfig.baseUrl("import-control-entry-declaration-store")

  lazy val validateIncomingJson: Boolean = config.getOptional[Boolean]("validateIncomingJson").getOrElse(false)

  lazy val validateJsonToXMLTransformation: Boolean =
    config.getOptional[Boolean]("validateJsonToXMLTransformation").getOrElse(false)

  lazy val longJourneyTime: FiniteDuration = getFiniteDuration(config, "longJourneyTime")
}

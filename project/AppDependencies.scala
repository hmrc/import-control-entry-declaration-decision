import play.core.PlayVersion.current
import sbt._

object AppDependencies {

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"    %% "bootstrap-backend-play-26" % "3.0.0",
    "com.github.fge" % "json-schema-validator"      % "2.2.6",
    "org.typelevel"  %% "cats-core"                 % "2.6.0",
    "com.chuusai"    %% "shapeless"                 % "2.3.4"
  )

  val test: Seq[ModuleID] = Seq(
    "org.scalatest"          %% "scalatest"          % "3.0.9"         % "test, it",
    "com.typesafe.play"      %% "play-test"          % current         % "test, it",
    "org.pegdown"            % "pegdown"             % "1.6.0"         % "test, it",
    "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.3"         % "test, it",
    "org.scalamock"          %% "scalamock"          % "5.1.0"         % "test, it",
    "org.scalacheck"         %% "scalacheck"         % "1.14.3"        % "test, it",
    "uk.gov.hmrc"            %% "hmrctest"           % "3.10.0-play-26" % "test, it",
    "com.github.tomakehurst" % "wiremock"            % "2.27.2"        % "test, it"
  )

  // Fixes a transitive dependency clash between wiremock and scalatestplus-play
  val overrides: Seq[ModuleID] = {
    val jettyFromWiremockVersion = "9.2.24.v20180105"
    Seq(
      "org.eclipse.jetty"           % "jetty-client"       % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-continuation" % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-http"         % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-io"           % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-security"     % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-server"       % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-servlet"      % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-servlets"     % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-util"         % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-webapp"       % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-xml"          % jettyFromWiremockVersion,
      "org.eclipse.jetty.websocket" % "websocket-api"      % jettyFromWiremockVersion,
      "org.eclipse.jetty.websocket" % "websocket-client"   % jettyFromWiremockVersion,
      "org.eclipse.jetty.websocket" % "websocket-common"   % jettyFromWiremockVersion
    )
  }

}

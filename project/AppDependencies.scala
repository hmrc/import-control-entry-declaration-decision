import play.core.PlayVersion.current
import sbt._

object AppDependencies {

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                %% "bootstrap-backend-play-28" % "5.18.0",
    "com.github.java-json-tools" %  "json-schema-validator"     % "2.2.14",
    "org.typelevel"              %% "cats-core"                 % "2.7.0",
    "com.chuusai"                %% "shapeless"                 % "2.3.7"
  )

  val test: Seq[ModuleID] = Seq(
    "com.typesafe.play"            %% "play-test"            % current    % "test, it",
    "org.pegdown"                  %  "pegdown"              % "1.6.0"    % "test, it",
    "org.scalatestplus.play"       %% "scalatestplus-play"   % "5.1.0"    % "test, it",
    "org.scalamock"                %% "scalamock"            % "5.2.0"    % "test, it",
    "org.scalacheck"               %% "scalacheck"           % "1.15.4"   % "test, it",
    "com.github.tomakehurst"       %  "wiremock-jre8"        % "2.32.0"   % "test, it",
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.13.1"   % "test, it",
    "org.scalatestplus"            %% "scalacheck-1-15"      % "3.2.10.0" % "test, it",
    "com.vladsch.flexmark"         %  "flexmark-all"         % "0.36.8"   % "test, it"
  )

  // Fixes a transitive dependency clash between wiremock and scalatestplus-play
  val overrides: Seq[ModuleID] = {
    val jettyFromWiremockVersion = "9.4.44.v20210927"
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

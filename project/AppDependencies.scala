import play.core.PlayVersion.current
import sbt._

object AppDependencies {

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                %% "bootstrap-backend-play-28" % "5.25.0",
    "com.github.java-json-tools" %  "json-schema-validator"     % "2.2.14",
    "org.typelevel"              %% "cats-core"                 % "2.8.0",
    "com.chuusai"                %% "shapeless"                 % "2.3.9"
  )

  val test: Seq[ModuleID] = Seq(
    "com.typesafe.play"            %% "play-test"              % current    % "test, it",
    "org.pegdown"                  %  "pegdown"                % "1.6.0"    % "test, it",
    "org.scalatestplus.play"       %% "scalatestplus-play"     % "5.1.0"    % "test, it",
    "org.scalamock"                %% "scalamock"              % "5.2.0"    % "test, it",
    "org.scalacheck"               %% "scalacheck"             % "1.16.0"   % "test, it",
    "com.github.tomakehurst"       %  "wiremock-jre8"          % "2.33.2"   % "test, it",
    "com.fasterxml.jackson.module" %% "jackson-module-scala"   % "2.13.3"   % "test, it",
    "org.scalatestplus"            %% "scalacheck-1-15"        % "3.2.11.0" % "test, it",
    "uk.gov.hmrc"                  %% "bootstrap-test-play-28" % "5.25.0"   % "test, it"
  )

}

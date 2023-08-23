import play.core.PlayVersion.current
import sbt._

object AppDependencies {

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                %% "bootstrap-backend-play-28" % "7.21.0",
    "com.github.java-json-tools" %  "json-schema-validator"     % "2.2.14",
    "org.typelevel"              %% "cats-core"                 % "2.10.0",
    "com.chuusai"                %% "shapeless"                 % "2.3.10"
  )

  val test: Seq[ModuleID] = Seq(
    "com.typesafe.play"            %% "play-test"              % current    % "test, it",
    "org.scalatestplus.play"       %% "scalatestplus-play"     % "5.1.0"    % "test, it",
    "org.scalamock"                %% "scalamock"              % "5.2.0"    % "test, it",
    "org.scalacheck"               %% "scalacheck"             % "1.17.0"   % "test, it",
    "com.github.tomakehurst"       %  "wiremock-jre8"          % "2.35.0"   % "test, it",
    "com.fasterxml.jackson.module" %% "jackson-module-scala"   % "2.15.2"   % "test, it",
    "org.scalatestplus"            %% "scalacheck-1-15"        % "3.2.11.0" % "test, it",
    "uk.gov.hmrc"                  %% "bootstrap-test-play-28" % "7.21.0"   % "test, it"
  )

}

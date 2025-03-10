import sbt.*

object AppDependencies {

  val bootstrapVersion = "9.11.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                %% "bootstrap-backend-play-30" % bootstrapVersion,
    "com.github.java-json-tools" %  "json-schema-validator"     % "2.2.14",
    "org.typelevel"              %% "cats-core"                 % "2.13.0",
    "com.chuusai"                %% "shapeless"                 % "2.3.13"
  )

  val test: Seq[ModuleID] = Seq(
  "uk.gov.hmrc"                  %% "bootstrap-test-play-30" % bootstrapVersion  % Test,
  "org.scalamock"                %% "scalamock"              % "6.2.0"           % Test,
  "org.scalacheck"               %% "scalacheck"             % "1.18.1"          % Test,
  "com.fasterxml.jackson.module" %% "jackson-module-scala"   % "2.18.3"          % Test,
  "org.wiremock"                 %  "wiremock"               % "3.12.1"           % Test,
  "org.scalatestplus"            %% "scalacheck-1-18"        % "3.2.19.0"        % Test
  )

}

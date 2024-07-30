import sbt.*

object AppDependencies {

  val bootstrapVersion = "8.6.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                %% "bootstrap-backend-play-30" % bootstrapVersion,
    "com.github.java-json-tools" %  "json-schema-validator"     % "2.2.14",
    "org.typelevel"              %% "cats-core"                 % "2.12.0",
    "com.chuusai"                %% "shapeless"                 % "2.3.12"
  )

  val test: Seq[ModuleID] = Seq(
  "uk.gov.hmrc"                  %% "bootstrap-test-play-30" % bootstrapVersion  % Test,
  "org.scalamock"                %% "scalamock"              % "6.0.0"           % Test,
  "org.scalacheck"               %% "scalacheck"             % "1.18.0"          % Test,
  "com.fasterxml.jackson.module" %% "jackson-module-scala"   % "2.17.2"          % Test,
  "org.wiremock"                 %  "wiremock"               % "3.9.1"           % Test,
  "org.scalatestplus"            %% "scalacheck-1-18"        % "3.2.19.0"        % Test
  )

}

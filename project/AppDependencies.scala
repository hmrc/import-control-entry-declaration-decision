import sbt.*

object AppDependencies {

  val bootstrapVersion = "10.8.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                  %% "bootstrap-backend-play-30" % bootstrapVersion,
    "com.github.java-json-tools"   %% "json-schema-validator"     % "2.2.14" cross CrossVersion.for3Use2_13 exclude("org.mozilla", "rhino"),
    "org.mozilla"                  %  "rhino"                     % "1.9.1",
    "org.typelevel"                %% "cats-core"                 % "2.13.0",
  )

  val test: Seq[ModuleID] = Seq(
  "uk.gov.hmrc"                  %% "bootstrap-test-play-30" % bootstrapVersion  % Test,
  "org.scalamock"                %% "scalamock"              % "7.5.5"           % Test,
  "org.scalacheck"               %% "scalacheck"             % "1.19.0"          % Test,
  "com.fasterxml.jackson.module" %% "jackson-module-scala"   % "2.22.2"          % Test,
  "org.wiremock"                 %  "wiremock"               % "3.13.2"           % Test,
  "org.scalatestplus"            %% "scalacheck-1-18"        % "3.2.19.0"        % Test
  )

}

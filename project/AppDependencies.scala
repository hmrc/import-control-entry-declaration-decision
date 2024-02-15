import sbt.*

object AppDependencies {

  val bootstrapVersion = "8.4.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                %% "bootstrap-backend-play-30" % bootstrapVersion,
    "com.github.java-json-tools" %  "json-schema-validator"     % "2.2.14",
    "org.typelevel"              %% "cats-core"                 % "2.10.0",
    "com.chuusai"                %% "shapeless"                 % "2.3.10"
  )

  val test: Seq[ModuleID] = Seq(
  "uk.gov.hmrc"                  %% "bootstrap-test-play-30" % bootstrapVersion  % Test,
  "org.scalamock"                %% "scalamock"              % "5.2.0"           % Test,
  "org.scalacheck"               %% "scalacheck"             % "1.17.0"          % Test,
  "com.fasterxml.jackson.module" %% "jackson-module-scala"   % "2.16.1"          % Test,
  "org.scalatestplus"            %% "scalacheck-1-15"        % "3.2.11.0"        % Test
  )

  val itDependencies: Seq[ModuleID] = Seq(
    "org.wiremock"               % "wiremock"                % "3.3.1"           % Test
  )

}

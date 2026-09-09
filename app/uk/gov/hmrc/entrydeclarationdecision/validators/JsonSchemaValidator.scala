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

package uk.gov.hmrc.entrydeclarationdecision.validators

import java.net.URL
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.networknt.schema.{Schema, SchemaRegistry, SpecificationVersion}
import play.api.libs.json.JsValue
import uk.gov.hmrc.entrydeclarationdecision.logging.{ContextLogger, LoggingContext}

import java.io.FileInputStream
import scala.jdk.CollectionConverters.*

object JsonSchemaValidator {

  val basePath: String = System.getProperty("user.dir")
  private val registry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_4)


  def validateJSONAgainstSchema(
                                 inputDoc: JsValue,
                                 schemaDoc: String = "conf/jsonSchemas/EntrySummaryDeclarationResponse.json")
                               (using lc: LoggingContext): Either[List[String], Unit] =
    try {
      val mapper: ObjectMapper = new ObjectMapper()
      val inputJson: JsonNode = mapper.readTree(inputDoc.toString())
      val schema: Schema = registry.getSchema(new FileInputStream(s"$basePath/$schemaDoc"))
      val errors: List[String] = schema.validate(inputJson).asScala.toList.map(_.getMessage)
      if (errors.nonEmpty) {
        ContextLogger.error(s"Failed to validate $errors")
        ContextLogger.debug(s"Failed to validate $inputDoc and $errors")
        Left(errors)
      } else {
        Right(())
      }
    } catch {
      case e: Exception =>
        ContextLogger.error(s"Failed to validate", e)
        ContextLogger.debug(s"Failed to validate $inputDoc", e)
        Left(List(e.getMessage))
    }

  def url(resourceName: String): URL = Thread.currentThread().getContextClassLoader.getResource(resourceName)
}

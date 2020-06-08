/*
 * Copyright 2020 HM Revenue & Customs
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
import com.github.fge.jsonschema.core.report.ProcessingReport
import com.github.fge.jsonschema.main.{JsonSchemaFactory, JsonValidator}
import play.Logger
import play.api.libs.json.JsValue

object JsonSchemaValidator {

  private val factory = JsonSchemaFactory.byDefault()

  def validateJSONAgainstSchema(
    inputDoc: JsValue,
    schemaDoc: String = "jsonSchemas/EntrySummaryDeclarationResponse.json"): Boolean =
    try {
      val mapper: ObjectMapper     = new ObjectMapper()
      val inputJson: JsonNode      = mapper.readTree(inputDoc.toString())
      val jsonSchema: JsonNode     = mapper.readTree(url(schemaDoc))
      val validator: JsonValidator = factory.getValidator
      val report: ProcessingReport = validator.validate(jsonSchema, inputJson)
      if (!report.isSuccess) Logger.error(s"Failed to validate $inputDoc and $report")

      report.isSuccess
    } catch {
      case e: Exception =>
        Logger.error(s"Failed to validate $inputDoc", e)
        false
    }

  def url(resourceName: String): URL = Thread.currentThread().getContextClassLoader.getResource(resourceName)
}
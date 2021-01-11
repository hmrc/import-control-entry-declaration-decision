/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.entrydeclarationdecision.utils

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

import javax.xml.XMLConstants
import javax.xml.parsers.SAXParserFactory
import javax.xml.validation.{Schema, SchemaFactory}
import org.xml.sax.{ErrorHandler, InputSource}

import scala.xml.{Node, SAXParseException}

trait SchemaType {
  private[utils] val schema: Schema
}

object SchemaType {

  case object CC328A extends SchemaType {
    private[utils] val schema = schemaFor("xsds/CC328A-v10-0.xsd")
  }

  case object CC316A extends SchemaType {
    private[utils] val schema = schemaFor("xsds/CC316A-v10-0.xsd")
  }

  case object CC304A extends SchemaType {
    private[utils] val schema = schemaFor("xsds/CC304A-v10-0.xsd")
  }

  case object CC305A extends SchemaType {
    private[utils] val schema = schemaFor("xsds/CC305A-v10-0.xsd")
  }

  private def schemaFor(xsdPath: String) = {
    val schemaLang = XMLConstants.W3C_XML_SCHEMA_NS_URI
    val resource = SchemaType.getClass.getClassLoader
      .getResource(xsdPath)

    SchemaFactory.newInstance(schemaLang).newSchema(resource)
  }
}

trait ValidationResult {
  def isValid: Boolean

  def allErrors: Seq[SAXParseException]
}

class SchemaValidator {

  private[SchemaValidator] class ValidationResultImpl extends ErrorHandler with ValidationResult {

    var warnings = Vector.empty[SAXParseException]

    var errors = Vector.empty[SAXParseException]

    var fatalErrors = Vector.empty[SAXParseException]

    override def warning(ex: SAXParseException): Unit =
      warnings = warnings :+ ex

    override def error(ex: SAXParseException): Unit =
      errors = errors :+ ex

    override def fatalError(ex: SAXParseException): Unit =
      fatalErrors = fatalErrors :+ ex

    def isValid: Boolean = errors.isEmpty && fatalErrors.isEmpty

    def allErrors: Seq[SAXParseException] = errors ++ fatalErrors
  }

  def validateSchema(schemaType: SchemaType, xml: Node): ValidationResult = {
    val factory = SAXParserFactory.newInstance()

    factory.setNamespaceAware(true)
    factory.setSchema(schemaType.schema)

    val reader = factory.newSAXParser().getXMLReader

    val validationResult = new ValidationResultImpl
    reader.setErrorHandler(validationResult)

    reader.parse(new InputSource(new ByteArrayInputStream(xml.toString.getBytes(StandardCharsets.UTF_8))))

    validationResult
  }
}

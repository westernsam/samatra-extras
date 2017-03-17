package com.springer.samatra.extras.responses

import java.io.{FileReader, InputStream, InputStreamReader, Reader}
import java.util
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Function
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.samskivert.mustache.Mustache.{Compiler, Lambda}
import com.samskivert.mustache.{Mustache, Template}
import com.springer.samatra.routing.Routings.HttpResp
import com.springer.samatra.routing.StandardResponses.{Halt, Html}
import com.springer.samatra.extras.PrintStackTrace

import scala.collection.JavaConverters._

trait TemplateRenderer {
  def rendered(viewName: String, model: Map[String, Any]): Either[ViewRenderingError, String]
  def rendered(reader: Reader, model: Map[String, Any]): Either[ViewRenderingError, String]
}

case class ViewRenderingError(exception: Throwable) extends PrintStackTrace

case class NonLeaveFormattingError(branch: java.util.Map[_, _]) extends RuntimeException(s"Only leaves can be formatted, but we got '$branch'")

case class TemplateResponse(templateName: String, model: Map[String, Any], onError: Throwable => HttpResp = err => Halt(500, Some(err)))(implicit val renderer: TemplateRenderer) extends HttpResp {
  override def process(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
    val httpResp = {
      for {
        rendered <- renderer.rendered(templateName, model)
      } yield Html(rendered)
    } match {
      case Right(result) => result
      case Left(err) => onError(err.exception)
    }

    httpResp.process(req, resp)
  }
}

class MustacheRenderer(globals: Map[String, Any], templateReader: String => Reader, enableCache: Boolean) extends TemplateRenderer {

  private val templates = new ConcurrentHashMap[String, Template]()
  private val mustacheCompiler: Compiler = Mustache.compiler()
    .withFormatter {
      case m: java.util.Map[_, _] => throw NonLeaveFormattingError(m)
      case otherwise: Any => String.valueOf(otherwise)
    }
    .withLoader(
      (viewName: String) => templateReader(viewName)
    )

  private val templateLoader: Function[String, Template] = (viewName: String) => mustacheCompiler.compile(templateReader(viewName))

  /**
    * render the given template using the model provided.
    *
    * @param viewName template name
    * @param model must be a map containing the following scala types (as deeply nested as you like):
    *
          map: Map[String, _]
          iterable: Iterable[_]
          array: Array[_]
          option: Option[_]
          product: Product //Case class
          str:CharSequence
          bool:Boolean
          number:Number
          lambda:Lambda
    *
    */
  override def rendered(viewName: String, model: Map[String, Any]): Either[ViewRenderingError, String] = {
    try {
      Right(templateFor(viewName).execute(MustacheRenderer.toJavaModel(globals ++ model)))
    } catch {
      case failure: Throwable => Left(ViewRenderingError(failure))
    }
  }

  /**
    * render the template supplied by the reader using the model provided.
    *
    * @param reader a template supplied via a reader
    * @param model must be a map containing the following scala types (as deeply nested as you like):
    *
          map: Map[String, _]
          iterable: Iterable[_]
          array: Array[_]
          option: Option[_]
          product: Product //Case class
          str:CharSequence
          bool:Boolean
          number:Number
          lambda:Lambda
    *
    */
  override def rendered(reader: Reader, model: Map[String, Any]): Either[ViewRenderingError, String] = {
    try {
      Right(mustacheCompiler.compile(reader).execute(MustacheRenderer.toJavaModel(globals ++ model)))
    } catch {
      case failure: Throwable => Left(ViewRenderingError(failure))
    }
  }

  private def templateFor(viewName: String): Template = {
    if (enableCache) templates.computeIfAbsent(viewName, templateLoader)
    else templateLoader(viewName)
  }
}

object MustacheRenderer {

  class FileTemplateLoader(dir: String) extends (String => Reader) {
    override def apply(viewName: String): Reader = new FileReader(s"$dir/$viewName.mustache")
  }

  class ClasspathTemplateLoader(templatePath: String = s"/templates") extends (String => Reader) {
    override def apply(viewName: String): InputStreamReader = {
      val asStream: InputStream = getClass.getResourceAsStream(s"$templatePath/$viewName.mustache")
      if (asStream == null) throw new NullPointerException(s"Expected a stream from $templatePath/$viewName.mustache but got null")
      new InputStreamReader(asStream)
    }
  }

  private def toJava(obj: Any): Any = obj match {
    case map: Map[String, Any]@unchecked => map.mapValues(toJava).asJava
    case iterable: Iterable[_]@unchecked => iterable.map(toJava).asJava
    case array: Array[_]@unchecked => array.map(toJava)
    case option: Option[Any]@unchecked => option.map(toJava).toList.asJava
    case product: Product => toJavaModel(product)
    case str:CharSequence => str
    case bool:Boolean => bool
    case number:Number => number
    case lambda:Lambda => lambda
    case _ => throw new IllegalArgumentException(s"Don't know how to java-ise $obj of type ${obj.getClass.getSimpleName}")
  }

  def toJavaModel(model: Map[String, Any]): util.Map[String, Any] = model.mapValues(toJava).asJava

  def toJavaModel(caseClass: Product): util.Map[String, Any] = {

    def caseClassMap(caseClass: Product) = {
      publicFields(caseClass).map { f =>
        f.setAccessible(true)
        f.getName -> toJava(f.get(caseClass))
      }.toMap.asJava
    }

    def publicFields(obj: Product) = obj.getClass.getDeclaredFields.filterNot(_.getName == "$outer")
    def isCaseClass(obj: Product) = publicFields(obj).length == obj.productArity

    if (isCaseClass(caseClass))
      caseClassMap(caseClass)
    else
      throw new IllegalArgumentException(s"Product is not a case class $caseClass")
  }
}

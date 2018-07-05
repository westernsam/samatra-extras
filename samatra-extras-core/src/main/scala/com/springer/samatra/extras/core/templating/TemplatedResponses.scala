package com.springer.samatra.extras.core.templating

import java.io.Reader

import com.springer.samatra.extras.core.PrintStackTrace
import com.springer.samatra.extras.core.templating.viewmodel.ViewModelBuilder
import com.springer.samatra.routing.Routings.HttpResp
import com.springer.samatra.routing.StandardResponses.{Halt, Html}
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

trait TemplateRenderer {
  def rendered(viewName: String, model: Map[String, Any]): Either[ViewRenderingError, String]
  def rendered(reader: Reader, model: Map[String, Any]): Either[ViewRenderingError, String]
}

case class ViewRenderingError(exception: Throwable) extends PrintStackTrace

case class NonLeafFormattingError(branch: java.util.Map[_, _]) extends RuntimeException(s"Only leaves can be formatted, but we got '$branch'")

sealed abstract class DocumentRendererError
object DocumentRendererError {
  final case class ViewModelBuilding(error: Exception) extends DocumentRendererError
  final case class ViewRendering(error: ViewRenderingError) extends DocumentRendererError
}

case class ViewModelTemplateResponse[C, E](templateName: String, viewModel: ViewModelBuilder[C], context: C, onError: Throwable => HttpResp = err => Halt(500, Some(err)))(implicit val renderer: TemplateRenderer) extends HttpResp {
  override def process(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
    val httpResp = {
      for {
        model <- viewModel.model(context).left.map(DocumentRendererError.ViewModelBuilding)
        rendered <- renderer.rendered(templateName, model).left.map(DocumentRendererError.ViewRendering)
      } yield Html(rendered)
    } match {
      case Right(result) => result
      case Left(DocumentRendererError.ViewModelBuilding(ex)) => onError(ex)
      case Left(DocumentRendererError.ViewRendering(ViewRenderingError(ex))) => onError(ex)
    }

    httpResp.process(req, resp)
  }
}

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
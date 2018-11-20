package com.springer.samatra.extras.formbuilders

import java.util
import java.util.UUID
import java.util.concurrent.{CountDownLatch, TimeUnit}

import com.springer.samatra.routing.Request
import com.springer.samatra.routing.Routings._
import com.springer.samatra.routing.StandardResponses.{AddCookie, Halt, WithCookies}
import javax.servlet.http._
import scalatags.Text
import scalatags.Text.all._

import scala.collection.mutable
import scala.reflect.{ClassTag, classTag}
import Implicits.RequestOps
import com.springer.samatra.testing.servlet.{InMemHttpServletRequest, InMemHttpServletResponse}

object FormBuilders {

  implicit class ScalaTagsToHtml(val f: Frag) extends HttpResp {
    override def process(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
      resp.setContentType("text/html; charset=utf-8")
      resp.getWriter.write(f.render)
    }
  }

  abstract class FormBuilderPageController extends Controller {

    def bind(page: Page): Unit = {

      get(page.path)(implicit req => {
        val csrfToken = UUID.randomUUID().toString
        req.setAttribute("csrfToken", csrfToken)
        WithCookies(AddCookie("csrfToken", csrfToken, httpOnly = true)) {
          page.render
        }
      })

      page.onRoute { route =>
        routes.append(route)
      }

      val latch = new CountDownLatch(1)
      val request = new InMemHttpServletRequest("http", page.path, "GET", Map.empty, None, Seq.empty, asyncListeners = new util.ArrayList(), countDown = latch)
      val resp = page.render(Request(request))

      resp.process(request, new InMemHttpServletResponse((_, _) => (), (_, _) => (), _ => (), _ => ()))
      latch.await(1, TimeUnit.SECONDS)
    }
  }

  trait Page {

    var onRoute : Route => Unit = _
    def onRoute(r: Route => Unit): Unit = {
      onRoute = r
    }

    def render(implicit req: Request): HttpResp
    def path: String

    protected def get(path: String)(body: Request => HttpResp): Unit = {
      onRoute(PathParamsRoute(GET, path, body))
    }
    protected def post(path: String)(body: Request => HttpResp): Unit = {
      onRoute(PathParamsRoute(POST, path, body))
    }

    val refs: mutable.Set[String] = mutable.Set[String]()
    protected def onHref(name:String, p: Page)(implicit r: Request): Frag = {
      val contextualizedPath = r.contextualize(p.path)

      if (refs.add(contextualizedPath)) {
        get(p.path)(implicit r => p.render)
      }

      a(href:=contextualizedPath, name)
    }

    protected def formOf[A <: Product: ClassTag](current: Option[A] = None, onPost: Request => HttpResp)(implicit r: Request): Frag ={
      val clazz = classTag[A].runtimeClass

      val inputFields = clazz.getDeclaredFields.map { f=>
        f.setAccessible(true)
        val fn = f.getName
        input(`type` := "text", name := fn, value := current.map(f.get(_).toString).getOrElse(""))
      }

      onForm(onPost, "/" + classTag[A].runtimeClass.getSimpleName,
        inputFields :+ input(`type` := "submit", name := "submit", value := "Submit") :_*
      )
    }

    val forms: mutable.Set[String] = mutable.Set[String]()
    protected def onForm(onPost: Request => HttpResp, path: String, xs: Text.all.Modifier*)(implicit r: Request): Frag = {

      if (forms.add(path)) {
        post(path) { req =>
          if (!req.cookie("csrfToken").contains(req.queryStringParamValue("csrf-token"))) {
            Halt(401, Some(new IllegalStateException("Failed csrf token")))
          } else {
            onPost(req)
          }
        }
      }

      val csrfToken: Text.TypedTag[String] = input(`type` := "hidden", name := "csrf-token", value := Option(r.underlying.getAttribute("csrfToken")).map(_.toString).getOrElse("missing"))
      form(Seq(action := r.contextualize(path), method := "POST") ++ Seq(csrfToken +: xs: _*): _*)
    }
  }
}

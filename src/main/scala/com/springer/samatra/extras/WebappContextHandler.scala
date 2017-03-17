package com.springer.samatra.extras

import java.io.OutputStreamWriter
import java.nio.ByteBuffer
import java.util
import javassist.{ClassPool, CtClass}
import javax.servlet.{DispatcherType, Filter}

import com.springer.samatra.routing.Routings.{AggregateRoutes, HttpResp, PathParamsRoute, RegexRoute, Route, Routes}
import com.springer.samatra.routing.{Request, Routings}
import org.eclipse.jetty.servlet.{FilterHolder, ServletContextHandler, ServletHolder}

import scala.collection.mutable
import scala.util.control.NonFatal
import scala.util.{Success, Try}
trait RouteAndContext {
  def routesWithContext: Seq[(String, Routes)]
}

class WebappContextHandler extends ServletContextHandler with RouteAndContext {
  val routesWithContext: mutable.ArrayBuffer[(String, Routes)] = new mutable.ArrayBuffer[(String, Routes)]()

  def addFilter(filter: Filter, path: String = "/*"): this.type = {
    super.addFilter(new FilterHolder(filter), path, util.EnumSet.allOf(classOf[DispatcherType]))
    this
  }

  def addRoutes(path: String, routes: Routes*): this.type = {
    routes.foreach(r => routesWithContext.append(path.replaceAll("/\\*$", "") -> r))
    super.addServlet(new ServletHolder(Routes(routes: _*)), path)
    this
  }
}


trait RoutePrinting {
  self: RouteAndContext =>

  case class RouteWithLineNumber(r: Route, i: Option[Int])

  private val pool = ClassPool.getDefault

  def printRoutesTo(w: Appendable = new OutputStreamWriter(System.out)): this.type = {

    val routesWithLineNumbers: Seq[(String, RouteWithLineNumber)] = hackOutLineNumbers(routesWithContext)
    for {
      (context, r) <- routesWithLineNumbers
    } printRoute(context, r, w)

    this
  }

  def printRoute(context: String = "", r: RouteWithLineNumber, out: Appendable): Unit = r match {
    case RouteWithLineNumber(RegexRoute(method, pattern, resp), ln) if method != Routings.HEAD => out.append(printRoute(method, context + pattern.toString(), resp, ln))
    case RouteWithLineNumber(PathParamsRoute(method, pattern, resp), ln) if method != Routings.HEAD => out.append(printRoute(method, context + pattern, resp, ln))
    case _ => //noop
  }

  private def printRoute(method: Routings.HttpMethod, pattern: String, resp: (Request) => HttpResp, ln: Option[Int]): String = {
    val clazz: Class[_ <: (Request) => HttpResp] = resp.getClass
    val enclosingClassName = s"${clazz.getName.split("\\$\\$")(0).split("\\.").reverse.head}"
    s"${method.toString.padTo(4, ' ')} ${pattern.toString.padTo(32, ' ')} -> ${clazz.getPackage.getName} ($enclosingClassName.scala:${ln.getOrElse("?")})\n"
  }

  def hackOutLineNumbers(allRoutes: Seq[(String, Routes)]): Seq[(String, RouteWithLineNumber)] = {

    /*
    *   1. look up the name of the path in the const pool - get the index
        2. look for ldc of that index in the code of the init method (18, then byte val of 1), take index of ldc (18)
        3. look up index from (2) in line number table of init

        ldc	  12	0001 0010	1: index	→                 value	push a constant #index from a constant pool (String, int or float) onto the stack
        ldc_w	13	0001 0011	2: indexbyte1, indexbyte2	→ value	push a constant #index from a constant pool (String, int or float) onto the stack (wide index is constructed as indexbyte1 << 8 + indexbyte2)

    * */

    def lineNumber(pattern: String, clazz: Class[_]): Option[Int] = try {
      val ctClazz: CtClass = pool.get(clazz.getName)

      val constructor = ctClazz.getConstructors()(0).toMethod("waht", ctClazz).getMethodInfo
      val constPool = constructor.getConstPool

      val poolId = (for (i <- 1 until constPool.getSize) yield i).find { i =>
        Try {
          constPool.getLdcValue(i)
        } match {
          case Success(value) if value == pattern => true
          case _ => false
        }
      }

      if (poolId.isEmpty) None
      else {
        val cpId: Int = poolId.get

        val code = constructor.getCodeAttribute.getCode

        val foundIt = code.zipWithIndex.find { case (c, idx) =>
          /*ldc code*/ c == 18 && code(idx + 1) == cpId.toByte ||
          /*ldc_w code*/ c == 19 && {
          val bytes = ByteBuffer.allocate(4).putInt(cpId).array()
          bytes(2) == code(idx + 1) && bytes(3) == code(idx + 2)
        }
        }

        foundIt.map(ln => constructor.getLineNumber(ln._2))
      }
    } catch {
      case NonFatal(e) => None
    }

    val buffer: Seq[(String, RouteWithLineNumber)] = {
      for {
        (cxt, rss: Routes) <- allRoutes
        realRss <- {
          rss match {
            case routes: AggregateRoutes =>
              routes.controllers
            case _ =>
              Seq(rss)
          }
        }
        rs: Route <- realRss.routes
        if rs.method != Routings.HEAD
      } yield rs match {
        case r@RegexRoute(method, pattern, _) => cxt -> RouteWithLineNumber(r, lineNumber(pattern.pattern.toString, realRss.getClass))
        case r@PathParamsRoute(method, pattern, _) => cxt -> RouteWithLineNumber(r, lineNumber(pattern, realRss.getClass))
      }
    }

    buffer
  }
}
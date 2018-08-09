package com.springer.samatra.extras.routeprinting

import java.io.OutputStreamWriter
import java.nio.ByteBuffer

import com.springer.samatra.extras.core.jetty.RouteAndContext
import com.springer.samatra.extras.routeprinting.RoutePrinting.{RouteWithLineNumber, ServletRouteWithLineNumber, lineNumber}
import com.springer.samatra.routing.Routings._
import com.springer.samatra.routing.{Request, Routings}
import javassist.{ClassPool, CtClass}

import scala.util.control.NonFatal
import scala.util.{Success, Try}


trait RoutePrinting {
  self: RouteAndContext =>

  def routesWithLineNumbers: Seq[(String, RouteWithLineNumber)] = RoutePrinting.hackOutLineNumbers(routesWithContext, { (routes, route) =>
    route match {
      case RegexRoute(_, pattern, _) if routes.isInstanceOf[Controller] => Some(ServletRouteWithLineNumber(route, lineNumber(pattern.pattern.toString, routes.getClass)))
      case PathParamsRoute(_, pattern, _) if routes.isInstanceOf[Controller] => Some(ServletRouteWithLineNumber(route, lineNumber(pattern, routes.getClass)))
      case _ => None
    }
  })

  def printRoutesTo(w: Appendable = new OutputStreamWriter(System.out)): this.type = {
    for {
      (servletPath, r) <- routesWithLineNumbers
    } r.printRoute(getContextPath, servletPath, w)
    this
  }
}

object RoutePrinting {

  trait RouteWithLineNumber {
    def printRoute(context: String, servlet: String = "", out: Appendable): Unit
  }

  case class ServletRouteWithLineNumber(r: Route, i: Option[Int]) extends RouteWithLineNumber {

    def printRoute(contextPath: String, servletPath: String = "", out: Appendable): Unit = r match {
      case RegexRoute(method, pattern, resp) if method != Routings.HEAD => out.append(printRoute(method, contextPath + servletPath + pattern.toString(), resp, i))
      case PathParamsRoute(method, pattern, resp) if method != Routings.HEAD => out.append(printRoute(method, contextPath + servletPath + pattern, resp, i))
      case _ => //noop
    }

    private def printRoute(method: Routings.HttpMethod, pattern: String, resp: Request => HttpResp, ln: Option[Int]): String = {
      val clazz: Class[_ <: Request => HttpResp] = resp.getClass
      val enclosingClassName = s"${clazz.getName.split("\\$\\$")(0).split("\\.").reverse.head}"
      s"${method.toString.padTo(6, ' ')} ${pattern.toString.padTo(32, ' ')} -> ${clazz.getPackage.getName} ($enclosingClassName.scala:${ln.getOrElse("?")})\n"
    }
  }

  /*
  *   1. look up the name of the path in the const pool - get the index
      2. look for ldc of that index in the code of the init method (18, then byte val of 1), take index of ldc (18)
      3. look up index from (2) in line number table of init

      ldc	  12	0001 0010	1: index	→                 value	push a constant #index from a constant pool (String, int or float) onto the stack
      ldc_w	13	0001 0011	2: indexbyte1, indexbyte2	→ value	push a constant #index from a constant pool (String, int or float) onto the stack (wide index is constructed as indexbyte1 << 8 + indexbyte2)

  * */
  private val pool = ClassPool.getDefault
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

  def hackOutLineNumbers(allRoutes: Seq[(String, Routes)], routeWithLineNumber: (Routes, Route) => Option[RouteWithLineNumber]): Seq[(String, RouteWithLineNumber)] = {
    for {
      (cxt, rss: Routes) <- allRoutes
      realRss: Routes <- {
        rss match {
          case routes: AggregateRoutes =>
            routes.controllers
          case _ =>
            Seq(rss)
        }
      }
      rs: Route <- realRss.routes
      if rs.method != Routings.HEAD
      res <- routeWithLineNumber(realRss, rs)
    } yield cxt -> res
  }
}
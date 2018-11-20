package com.springer.samatra.extras.formbuilders

import com.springer.samatra.routing.Request

import scala.reflect.{ClassTag, classTag}

object Implicits {

  implicit class RequestOps(r: Request) {

    def contextualize(path: String): String = {
      val flatten = Seq(Option(r.underlying.getContextPath),
        Option(r.underlying.getServletPath),
        Option(r.underlying.getPathInfo)).flatten

      flatten
        .mkString.split("/").reverse.drop(1).reverse.mkString("/") + path
    }

    def extract[A <: Product : ClassTag]: A = {
      val clazz = classTag[A].runtimeClass

      val fields = clazz.getDeclaredFields.map { f =>
        f.setAccessible(true)
        f.getName -> f.getType
      }

      clazz.getConstructor(fields.map(_._2): _*).newInstance(fields.map(_._1).map(r.queryStringParamValue): _*).asInstanceOf[A]
    }
  }
}
package com.springer.samatra.extras.responses

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.springer.samatra.routing.Routings.HttpResp

import scala.language.implicitConversions
import scala.xml.Elem

object XmlResponses {

  implicit def fromXmlResponse(xml: Elem): HttpResp = new XmlResp(xml)

  case class XmlResp(xml:String) extends HttpResp {
    def this(elem:Elem) = this(elem.buildString(stripComments = true))
    override def process(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
      resp.setContentType("application/xml")
      resp.setStatus(200)
      resp.getOutputStream.write(xml.getBytes)
    }
  }
}

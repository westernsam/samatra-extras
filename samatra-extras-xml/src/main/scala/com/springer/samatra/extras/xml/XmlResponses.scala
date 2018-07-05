package com.springer.samatra.extras.xml

import com.springer.samatra.routing.Routings.HttpResp
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import scala.language.implicitConversions
import scala.xml.Elem

object XmlResponses {

  implicit def fromXmlResponse(xml: Elem): HttpResp = new XmlResp(xml)

  case class XmlResp(xml:String) extends HttpResp {
    def this(elem:Elem) = this(elem.buildString(stripComments = true))
    override def process(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
      resp.setContentType("application/xml; charset=utf-8")
      resp.setStatus(200)
      resp.getOutputStream.write(xml.getBytes)
    }
  }
}

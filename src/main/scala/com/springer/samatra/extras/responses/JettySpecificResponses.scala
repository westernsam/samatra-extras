package com.springer.samatra.extras.responses

import java.nio.ByteBuffer
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.springer.samatra.routing.Routings.HttpResp
import org.eclipse.jetty.server.HttpOutput

import scala.language.implicitConversions

object JettySpecificResponses {

  implicit def fromByteBuffer(b: ByteBuffer): HttpResp = (req: HttpServletRequest, resp: HttpServletResponse) => resp.getOutputStream match {
    case httpOut: HttpOutput => httpOut.sendContent(b)
    case notHttpOut@_ => notHttpOut.write(b.array())
  }
}

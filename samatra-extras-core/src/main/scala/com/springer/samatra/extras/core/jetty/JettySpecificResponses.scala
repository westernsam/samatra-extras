package com.springer.samatra.extras.core.jetty

import com.springer.samatra.routing.Routings.HttpResp
import org.eclipse.jetty.server.HttpOutput

import java.nio.ByteBuffer
import javax.servlet.ServletOutputStream
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import scala.language.implicitConversions

object JettySpecificResponses {

  implicit def fromByteBuffer(b: ByteBuffer): HttpResp = (req: HttpServletRequest, resp: HttpServletResponse) => {
    val stream: ServletOutputStream = resp.getOutputStream
    stream match {
      case httpOut: HttpOutput => httpOut.sendContent(b)
      case notHttpOut@_ => notHttpOut.write(b.array())
    }
  }
}

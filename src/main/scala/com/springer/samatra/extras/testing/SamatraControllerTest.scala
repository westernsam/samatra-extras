package com.springer.samatra.extras.testing

import java.io.BufferedReader
import java.security.Principal
import java.util
import java.util.Locale
import javax.servlet._
import javax.servlet.http._

import com.springer.samatra.routing.FutureResponses.FutureHttpResp
import com.springer.samatra.routing.Request
import com.springer.samatra.routing.Routings.{HttpResp, PathParamsRoute, RegexRoute, Routes}

import scala.concurrent.Future
import scala.language.implicitConversions

trait SamatraControllerTest {

  def httpServletRequest(path: String, method: String): HttpServletRequest = new HttpServletRequest {
    override def getPathInfo: String = ???
    override def getUserPrincipal: Principal = ???
    override def getServletPath: String = ""
    override def getDateHeader(name: String): Long = ???
    override def getIntHeader(name: String): Int = ???
    override def getMethod: String = method
    override def getContextPath: String = ???
    override def isRequestedSessionIdFromUrl: Boolean = ???
    override def getPathTranslated: String = ???
    override def getRequestedSessionId: String = ???
    override def isRequestedSessionIdFromURL: Boolean = ???
    override def logout(): Unit = ???
    override def changeSessionId(): String = ???
    override def getRequestURL: StringBuffer = ???
    override def upgrade[T <: HttpUpgradeHandler](handlerClass: Class[T]): T = ???
    override def getRequestURI: String = path
    override def isRequestedSessionIdValid: Boolean = ???
    override def getAuthType: String = ???
    override def authenticate(response: HttpServletResponse): Boolean = ???
    override def login(username: String, password: String): Unit = ???
    override def getHeader(name: String): String = ???
    override def getCookies: Array[Cookie] = ???
    override def getParts: util.Collection[Part] = ???
    override def getHeaders(name: String): util.Enumeration[String] = ???
    override def getQueryString: String = ???
    override def getPart(name: String): Part = ???
    override def isUserInRole(role: String): Boolean = ???
    override def getRemoteUser: String = ???
    override def getHeaderNames: util.Enumeration[String] = ???
    override def isRequestedSessionIdFromCookie: Boolean = ???
    override def getSession(create: Boolean): HttpSession = ???
    override def getSession: HttpSession = ???
    override def getRemoteAddr: String = ???
    override def getParameterMap: util.Map[String, Array[String]] = ???
    override def getServerName: String = ???
    override def getRemotePort: Int = ???
    override def getParameter(name: String): String = ???
    override def getRequestDispatcher(path: String): RequestDispatcher = ???
    override def getAsyncContext: AsyncContext = ???
    override def isAsyncSupported: Boolean = ???
    override def getContentLength: Int = ???
    override def getInputStream: ServletInputStream = ???
    override def isAsyncStarted: Boolean = ???
    override def startAsync(): AsyncContext = ???
    override def startAsync(servletRequest: ServletRequest, servletResponse: ServletResponse): AsyncContext = ???
    override def setCharacterEncoding(env: String): Unit = ???
    override def getCharacterEncoding: String = ???
    override def getServerPort: Int = ???
    override def getAttributeNames: util.Enumeration[String] = ???
    override def getContentLengthLong: Long = ???
    override def getParameterNames: util.Enumeration[String] = ???
    override def getContentType: String = ???
    override def getLocalPort: Int = ???
    override def getServletContext: ServletContext = null
    override def getRemoteHost: String = ???
    override def getLocalAddr: String = ???
    override def getRealPath(path: String): String = ???
    override def setAttribute(name: String, o: scala.Any): Unit = ???
    override def getAttribute(name: String): AnyRef = ???
    override def getLocales: util.Enumeration[Locale] = ???
    override def removeAttribute(name: String): Unit = ???
    override def getParameterValues(name: String): Array[String] = ???
    override def getScheme: String = ???
    override def getReader: BufferedReader = ???
    override def isSecure: Boolean = ???
    override def getProtocol: String = ???
    override def getLocalName: String = ???
    override def getDispatcherType: DispatcherType = ???
    override def getLocale: Locale = ???
  }

  def futureFrom(resp: HttpResp): Future[HttpResp] = resp match {
    case FutureHttpResp(fut, _, _, _, _) => fut.asInstanceOf[Future[HttpResp]]
    case _ => Future.successful(resp)
  }

  def get(r: Routes)(path: String): Future[HttpResp] = runRequest(r, httpServletRequest(path, "GET"))
  def head(r: Routes)(path: String): Future[HttpResp] = runRequest(r, httpServletRequest(path, "HEAD"))
  def post(r: Routes, body: Array[Byte])(path: String): Future[HttpResp] = runRequest(r, httpServletRequest(path, "POST"))
  def put(r: Routes)(path: String): Future[HttpResp] = runRequest(r, httpServletRequest(path, "PUT"))
  def delete(r: Routes)(path: String): Future[HttpResp] = runRequest(r, httpServletRequest(path, "DELETE"))

  private def runRequest(r: Routes, httpReq: HttpServletRequest) = {
    val request = Request(httpReq)
    val route = r.matching(httpReq, null).right.get
    val requestToResp: (Request) => HttpResp = route match {
      case PathParamsRoute(_, _, resp) => resp
      case RegexRoute(_, _, resp) => resp
    }
    val matches = route.matches(request)
    futureFrom(requestToResp(request.copy(params = matches.get)))
  }
}

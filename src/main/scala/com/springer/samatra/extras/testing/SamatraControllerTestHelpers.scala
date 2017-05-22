package com.springer.samatra.extras.testing

import java.io._
import java.net.URLEncoder
import java.security.Principal
import java.text.SimpleDateFormat
import java.util
import java.util.{Calendar, Locale, TimeZone}
import java.util.concurrent.{ConcurrentHashMap, CopyOnWriteArrayList}
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger, AtomicReference}
import java.util.function.Function
import javax.servlet._
import javax.servlet.http._

import com.samskivert.mustache.Template
import com.springer.samatra.routing.FutureResponses.FutureHttpResp
import com.springer.samatra.routing.Request
import com.springer.samatra.routing.Routings._
import com.springer.samatra.routing.StandardResponses.{Halt, WithHeaders}

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.language.implicitConversions

object SamatraControllerTestHelpers {
  val dateFormat: SimpleDateFormat = new SimpleDateFormat("EEE, dd MM yyyy hh:mm:ss 'GMT'")

  implicit class SamatraHttpRespHelpers(resp: HttpResp) {
    private val committed = new AtomicBoolean(false)
    private val bytes = new ByteArrayOutputStream()
    private val writer = new PrintWriter(new OutputStreamWriter(bytes))
    private val stream = new ServletOutputStream {
      override def isReady: Boolean = true
      override def setWriteListener(writeListener: WriteListener): Unit = throw new UnsupportedOperationException
      override def write(b: Int): Unit = bytes.write(b)
    }

    private val status = new AtomicInteger(200)
    private val respHeaders = new ConcurrentHashMap[String, CopyOnWriteArrayList[String]]()
    private val respCookies = new CopyOnWriteArrayList[Cookie]()
    private val characterEncoding = new AtomicReference[String]("UTF-8")
    private val contentType = new AtomicReference[String]()

    resp.process(null, new HttpServletResponse {
      override def sendError(sc: Int, msg: String): Unit = {
        setStatus(sc)
        getWriter.write(msg)
      }
      override def sendError(sc: Int): Unit = setStatus(sc)
      override def getStatus: Int = status.get()

      override def addCookie(cookie: Cookie): Unit = respCookies.add(cookie)

      override def getHeader(name: String): String = respHeaders.get(name).asScala.head

      override def setHeader(name: String, value: String): Unit = respHeaders.computeIfAbsent(name, _ => new CopyOnWriteArrayList[String]()).add(value)


      override def setIntHeader(name: String, value: Int): Unit = setHeader(name, value.toString)
      override def addDateHeader(name: String, date: Long): Unit = addHeader(name, formatDate(date))
      override def setDateHeader(name: String, date: Long): Unit = setHeader(name, formatDate(date))

      override def encodeURL(url: String): String = URLEncoder.encode(url, "UTF-8")
      override def encodeUrl(url: String): String = URLEncoder.encode(url, "UTF-8")

      override def addHeader(name: String, value: String): Unit = respHeaders.computeIfAbsent(name, _ => new CopyOnWriteArrayList[String]()).add(value)
      override def getHeaders(name: String): util.Collection[String] = respHeaders.getOrDefault(name, new CopyOnWriteArrayList[String]())

      override def encodeRedirectUrl(url: String): String = encodeUrl(url)
      override def encodeRedirectURL(url: String): String = encodeURL(url)
      override def sendRedirect(location: String): Unit = {
        status.set(302)
        addHeader("Location", location)
      }
      override def setStatus(sc: Int): Unit = status.set(sc)
      override def setStatus(sc: Int, sm: String): Unit = {
        status.set(sc)
        getWriter.write(sm)
      }
      override def getHeaderNames: util.Collection[String] = respHeaders.keySet()
      override def containsHeader(name: String): Boolean = respHeaders.containsKey(name)
      override def addIntHeader(name: String, value: Int): Unit = addHeader(name, value.toString)
      override def getBufferSize: Int = -1
      override def resetBuffer(): Unit = ()
      override def setContentType(`type`: String): Unit = contentType.set(`type`)
      override def setBufferSize(size: Int): Unit = ()
      override def isCommitted: Boolean = committed.get()
      override def setCharacterEncoding(charset: String): Unit = characterEncoding.set(charset)
      override def setContentLength(len: Int): Unit = setHeader("Content-Length", len.toString)
      override def setContentLengthLong(len: Long): Unit = setHeader("Content-Length", len.toString)

      override def getCharacterEncoding: String = characterEncoding.get
      override def flushBuffer(): Unit = ()
      override def getContentType: String = Option(contentType.get()).getOrElse("")
      override def reset(): Unit = ()

      override def getWriter: PrintWriter = {
        if (committed.getAndSet(true)) throw new IllegalStateException("Connection already commited")
        writer
      }

      override def getOutputStream: ServletOutputStream = {
        if (committed.getAndSet(true)) throw new IllegalStateException("Connection already commited")
        stream
      }

      override def getLocale: Locale = Locale.getDefault
      override def setLocale(loc: Locale): Unit = ()
    })

    writer.flush()
    writer.close()

    stream.flush()
    stream.close()

    private def formatDate(date: Long) = {
      val cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"))
      cal.setTimeInMillis(date)
      dateFormat.format(cal.getTime)
    }

    def outputAsString: String = new String(bytes.toByteArray)
    def outputAsBytes: Array[Byte] = bytes.toByteArray
    def outputAsStream: InputStream = new ByteArrayInputStream(outputAsBytes)

    def statusCode: Int = status.get()

    def headers: Map[String, Seq[String]] = {
      val contentTypeHeaders = {
        (Option(contentType.get), Option(characterEncoding.get())) match {
          case (Some(t), Some(e)) => Seq(t, e)
          case (Some(t), None) => Seq(t)
          case (None, Some(e)) => Seq(e)
          case (None, None) => Seq()
        }
      }
      val hds = respHeaders.asScala.mapValues(_.asScala.toSeq).toMap
      if (contentTypeHeaders.isEmpty) hds else hds.updated("Content-Type", contentTypeHeaders)
    }

    def cookies: Seq[Cookie] = respCookies.asScala
  }

  /*
    *
    *
    *
      getParameter
      getParameterMap

      getAttribute
      setAttribute
      removeAttribute

      getQueryString
      getRequestURL

      getCookies
  [x] getInputStream
    * */
  def httpServletRequest(path: String, method: String, headers: Map[String, Seq[String]], body: Option[Array[Byte]]): HttpServletRequest = {
    val bytes =  body match {
      case Some(b) => new ByteArrayInputStream(body.get)
      case None => new ByteArrayInputStream(new Array[Byte](0))
    }

    new HttpServletRequest {

      override def getPathInfo: String = ???
      override def getUserPrincipal: Principal = ???
      override def getServletPath: String = ""
      override def getDateHeader(name: String): Long = ???
      override def getIntHeader(name: String): Int = ???
      override def getMethod: String = method
      override def getContextPath: String = "/"
      override def isRequestedSessionIdFromUrl: Boolean = ???
      override def getPathTranslated: String = ???
      override def getRequestedSessionId: String = ""
      override def isRequestedSessionIdFromURL: Boolean = ???
      override def logout(): Unit = ()
      override def changeSessionId(): String = ???
      override def getRequestURL: StringBuffer = new StringBuffer(path)
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
      override def getInputStream: ServletInputStream = new ServletInputStream {
        override def isReady: Boolean = true
        override def isFinished: Boolean = bytes.available() > -1
        override def setReadListener(readListener: ReadListener): Unit = ()
        override def read(): Int = bytes.read()
      }
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
      override def getScheme: String = "http"
      override def getReader: BufferedReader = ???
      override def isSecure: Boolean = false
      override def getProtocol: String = ???
      override def getLocalName: String = ???
      override def getDispatcherType: DispatcherType = ???
      override def getLocale: Locale = Locale.getDefault
    }
  }

  def get(r: Routes)(path: String, headers: Map[String, Seq[String]] = Map.empty): Future[HttpResp] = runRequest(r, httpServletRequest(path, "GET", headers, None))
  def head(r: Routes)(path: String, headers: Map[String, Seq[String]] = Map.empty): Future[HttpResp] = runRequest(r, httpServletRequest(path, "HEAD", headers, None))
  def post(r: Routes)(path: String, headers: Map[String, Seq[String]] = Map.empty, body: Array[Byte]): Future[HttpResp] = runRequest(r, httpServletRequest(path, "POST", headers, Some(body)))
  def put(r: Routes)(path: String, headers: Map[String, Seq[String]] = Map.empty, body: Array[Byte]): Future[HttpResp] = runRequest(r, httpServletRequest(path, "PUT", headers, Some(body)))
  def delete(r: Routes)(path: String, headers: Map[String, Seq[String]] = Map.empty): Future[HttpResp] = runRequest(r, httpServletRequest(path, "DELETE", headers, None))

  def futureFrom(resp: HttpResp): Future[HttpResp] = resp match {
    case FutureHttpResp(fut, _, _, _, _) => fut.asInstanceOf[Future[HttpResp]]
    case _ => Future.successful(resp)
  }

  private def runRequest(r: Routes, httpReq: HttpServletRequest) = {
    val request = Request(httpReq)

    futureFrom(r.matching(httpReq, null) match {
      case Right(route) =>
        val requestToResp: (Request) => HttpResp = route match {
          case PathParamsRoute(_, _, resp) => resp
          case RegexRoute(_, _, resp) => resp
        }
        val matches = route.matches(request)
        requestToResp(request.copy(params = matches.get))

      case Left(matchingRoutes: Seq[Route]) => matchingRoutes match {
        case Nil => Halt(404)
        case _ =>
          WithHeaders("Allow" -> Set(matchingRoutes.map(_.method): _*).mkString(", ")) {
            Halt(405)
          }
      }
    })
  }
}

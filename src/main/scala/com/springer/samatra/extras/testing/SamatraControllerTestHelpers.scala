package com.springer.samatra.extras.testing

import java.io._
import java.net.URLDecoder.decode
import java.net.{URLDecoder, URLEncoder}
import java.security.Principal
import java.text.SimpleDateFormat
import java.util
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger, AtomicReference}
import java.util.concurrent.{ConcurrentHashMap, CopyOnWriteArrayList}
import java.util.{Calendar, Collections, Locale, TimeZone}
import javax.servlet._
import javax.servlet.http._

import com.springer.samatra.extras.http.URI
import com.springer.samatra.routing.FutureResponses.FutureHttpResp
import com.springer.samatra.routing.Request
import com.springer.samatra.routing.Routings._
import com.springer.samatra.routing.StandardResponses.{Halt, WithHeaders}

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.language.implicitConversions

object SamatraControllerTestHelpers {
  private val zone = TimeZone.getTimeZone("GMT")
  private val dateFormat: SimpleDateFormat = new SimpleDateFormat("EEE, dd MM yyyy hh:mm:ss z")
  dateFormat.setTimeZone(zone)

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
      val cal = Calendar.getInstance(zone)
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

  def httpServletRequest(path: String, method: String, headers: Map[String, Seq[String]], body: Option[Array[Byte]], cookies: Seq[Cookie]): HttpServletRequest = {
    val committed = new AtomicBoolean(false)

    val bytes = body match {
      case Some(b) => new ByteArrayInputStream(body.get)
      case None => new ByteArrayInputStream(new Array[Byte](0))
    }
    val uri = URI.parse(path)
    val attributes = new ConcurrentHashMap[String, AnyRef]()

    new HttpServletRequest {

      override def getPathInfo: String = path
      override def getUserPrincipal: Principal = null
      override def getServletPath: String = ""
      override def getDateHeader(name: String): Long = if (headers.contains(name)) dateFormat.parse(getHeader(name)).getTime else -1
      override def getIntHeader(name: String): Int = if (headers.contains(name)) getHeader(name).toInt else -1
      override def getMethod: String = method
      override def getContextPath: String = "/"
      override def isRequestedSessionIdFromUrl: Boolean = false
      override def getPathTranslated: String = path
      override def getRequestedSessionId: String = ""
      override def isRequestedSessionIdFromURL: Boolean = false
      override def logout(): Unit = ()
      override def changeSessionId(): String = ""
      override def getRequestURL: StringBuffer = new StringBuffer(uri.format)
      override def upgrade[T <: HttpUpgradeHandler](handlerClass: Class[T]): T = throw new UnsupportedOperationException
      override def getRequestURI: String = path
      override def isRequestedSessionIdValid: Boolean = true
      override def getAuthType: String = null
      override def authenticate(response: HttpServletResponse): Boolean = true
      override def login(username: String, password: String): Unit = ()
      override def getHeader(name: String): String = headers.get(name).map(_.head).orNull
      override def getHeaders(name: String): util.Enumeration[String] = Collections.enumeration(headers(name).asJava)
      override def getQueryString: String = uri.queryString.orNull
      override def isUserInRole(role: String): Boolean = true
      override def getRemoteUser: String = ""
      override def getHeaderNames: util.Enumeration[String] = Collections.enumeration(headers.asJava.keySet())
      override def isRequestedSessionIdFromCookie: Boolean = false
      override def getSession(create: Boolean): HttpSession = null
      override def getSession: HttpSession = null
      override def getRemoteAddr: String = "SamatraTestHelper"
      override def getServerName: String = "SamatraTestHelper"
      override def getRemotePort: Int = -1
      override def getRequestDispatcher(path: String): RequestDispatcher = throw new UnsupportedOperationException
      override def getAsyncContext: AsyncContext = throw new UnsupportedOperationException
      override def isAsyncSupported: Boolean = false
      override def getInputStream: ServletInputStream = {
        if (committed.getAndSet(true)) throw new IllegalStateException("Request body already read")
        new ServletInputStream {
          override def isReady: Boolean = true
          override def isFinished: Boolean = bytes.available() > -1
          override def setReadListener(readListener: ReadListener): Unit = ()
          override def read(): Int = bytes.read()
        }
      }
      override def getReader: BufferedReader = {
        if (committed.getAndSet(true)) throw new IllegalStateException("Request body already read")
        new BufferedReader(new InputStreamReader(bytes))
      }

      override def isAsyncStarted: Boolean = false
      override def startAsync(): AsyncContext = throw new UnsupportedOperationException
      override def startAsync(servletRequest: ServletRequest, servletResponse: ServletResponse): AsyncContext = throw new UnsupportedOperationException


      override def setCharacterEncoding(env: String): Unit = ()
      override def getCharacterEncoding: String = headers.get("Content-Type").map(_.last).map(_.split("=").toList.last).orNull //Content-Type:text/html; charset=utf-8

      override def getServerPort: Int = -1

      override def setAttribute(name: String, o: AnyRef): Unit = attributes.put(name, o)
      override def getAttribute(name: String): AnyRef = attributes.get(name)
      override def getAttributeNames: util.Enumeration[String] = Collections.enumeration(attributes.keySet())
      override def removeAttribute(name: String): Unit = attributes.remove(name)

      override def getCookies: Array[Cookie] = cookies.toArray

      override def getParameterValues(name: String): Array[String] = getParameterMap.values().asScala.map(_.head).toArray
      override def getParameterNames: util.Enumeration[String] = Collections.enumeration(getParameterMap.keySet())
      override def getParameter(name: String): String = Option(getParameterMap.get(name)).map(_.head).orNull

      override def getParameterMap: util.Map[String, Array[String]] = {
        def parse(qs: String): Map[String, Array[String]] = {
          val tuples: Array[(String, String)] = for {
            p <- qs.split("&")
            keyAndValue = p.split("=", 2)
          } yield decode(keyAndValue(0), "UTF-8") -> decode(keyAndValue(1), "UTF-8")

          tuples.groupBy { case (k, _) => k }.mapValues(_.map(_._2))
        }

        uri.queryString.map(parse).getOrElse(Map.empty).asJava //todo params from body
      }
      override def getParts: util.Collection[Part] = ???
      override def getPart(name: String): Part = ???

      override def getContentLength: Int = body.map(_.length).getOrElse(0)
      override def getContentLengthLong: Long = bytes.available()
      override def getContentType: String = headers.get("Content-Type").map(_.head).orNull

      override def getLocalPort: Int = -1
      override def getServletContext: ServletContext = null
      override def getRemoteHost: String = ""
      override def getLocalAddr: String = ""
      override def getRealPath(path: String): String = path

      override def getScheme: String = "http"

      override def isSecure: Boolean = false
      override def getProtocol: String = "http"
      override def getLocalName: String = ""
      override def getDispatcherType: DispatcherType = DispatcherType.REQUEST
      override def getLocale: Locale = Locale.getDefault
      override def getLocales: util.Enumeration[Locale] = Collections.enumeration(util.Arrays.asList(getLocale))
    }
  }

  def get(r: Routes)(path: String, headers: Map[String, Seq[String]] = Map.empty, cookies: Seq[Cookie] = Seq.empty): Future[HttpResp] = runRequest(r, httpServletRequest(path, "GET", headers, None, cookies))
  def head(r: Routes)(path: String, headers: Map[String, Seq[String]] = Map.empty, cookies: Seq[Cookie] = Seq.empty): Future[HttpResp] = runRequest(r, httpServletRequest(path, "HEAD", headers, None, cookies))
  def post(r: Routes)(path: String, headers: Map[String, Seq[String]] = Map.empty, body: Array[Byte], cookies: Seq[Cookie] = Seq.empty): Future[HttpResp] = runRequest(r, httpServletRequest(path, "POST", headers, Some(body), cookies))
  def put(r: Routes)(path: String, headers: Map[String, Seq[String]] = Map.empty, body: Array[Byte], cookies: Seq[Cookie] = Seq.empty): Future[HttpResp] = runRequest(r, httpServletRequest(path, "PUT", headers, Some(body), cookies))
  def delete(r: Routes)(path: String, headers: Map[String, Seq[String]] = Map.empty, cookies: Seq[Cookie] = Seq.empty): Future[HttpResp] = runRequest(r, httpServletRequest(path, "DELETE", headers, None, cookies))

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

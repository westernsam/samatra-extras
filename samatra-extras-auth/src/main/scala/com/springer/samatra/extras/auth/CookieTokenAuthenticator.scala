package com.springer.samatra.extras.auth

import java.io.IOException

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import javax.servlet.{ServletRequest, ServletResponse}
import org.eclipse.jetty.security.authentication.LoginAuthenticator
import org.eclipse.jetty.security.{AbstractLoginService, ConstraintSecurityHandler, ServerAuthException, UserAuthentication}
import org.eclipse.jetty.server.Authentication

object CookieTokenAuthenticator {
  def apply(realm: String, application: String, tokenValidator: TokenValidator, onFail: (HttpServletRequest, HttpServletResponse) => Unit, cookieName: String = "auth"): ConstraintSecurityHandler = new ConstraintSecurityHandler {
    setAuthenticator(new CookieTokenAuthenticator(tokenValidator, onFail, cookieName))
    setRealmName(realm)
    setLoginService(new AbstractLoginService {
      override def loadRoleInfo(user: AbstractLoginService.UserPrincipal): Array[String] = {
        user match {
          case j: TokenAuthenticatedUser => j.roles()
          case _ => Array.empty
        }
      }
      override def loadUserInfo(username: String): AbstractLoginService.UserPrincipal = TokenAuthenticatedUser(username, application)
    })
  }
}

class CookieTokenAuthenticator(ec: TokenValidator, onFail: (HttpServletRequest, HttpServletResponse) => Unit, cookieName: String) extends LoginAuthenticator {
  override def getAuthMethod: String = "__JWT"
  override def validateRequest(req: ServletRequest, resp: ServletResponse, mandatory: Boolean): Authentication = {
    val request = req.asInstanceOf[HttpServletRequest]
    val response = resp.asInstanceOf[HttpServletResponse]
    try {

      val either: Either[String, UserAuthentication] = for {
        c <- Option(request.getCookies).getOrElse(Array.empty).find(_.getName == cookieName).toRight("No cookie")
        auth <- ec.validate(c.getValue)
      } yield {
        new UserAuthentication("__jwt", login(auth.name, auth, request))
      }

      either.getOrElse {
        onFail(request, response)
        Authentication.SEND_FAILURE
      }
    } catch {
      case e: IOException =>
        throw new ServerAuthException(e)
    }
  }
  override def secureResponse(request: ServletRequest, response: ServletResponse, mandatory: Boolean, validatedUser: Authentication.User): Boolean = true

}

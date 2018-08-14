package com.springer.samatra.extras.auth

import java.io.IOException
import java.security.{KeyFactory, Security}
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import javax.servlet.{ServletRequest, ServletResponse}
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.eclipse.jetty.security.authentication.LoginAuthenticator
import org.eclipse.jetty.security.{AbstractLoginService, ConstraintSecurityHandler, ServerAuthException, UserAuthentication}
import org.eclipse.jetty.server.Authentication
import pdi.jwt.{Jwt, JwtAlgorithm}

object JwtAuthenticator {
  def apply(realm: String, application: String, tokenValidator: TokenValidator, onFail: (HttpServletRequest, HttpServletResponse) => Unit, cookieName: String = "auth", publicKeyString: String): ConstraintSecurityHandler = new ConstraintSecurityHandler {
    setAuthenticator(new JwtAuthenticator(publicKeyString, tokenValidator, onFail, cookieName))
    setRealmName(realm)
    setLoginService(new AbstractLoginService {
      override def loadRoleInfo(user: AbstractLoginService.UserPrincipal): Array[String] = {
        user match {
          case j: JwtUser => j.roles()
          case _ => Array.empty
        }
      }
      override def loadUserInfo(username: String): AbstractLoginService.UserPrincipal = JwtUser(username, application)
    })
  }
}

class JwtAuthenticator(publicKeyString: String, ec: TokenValidator, onFail: (HttpServletRequest, HttpServletResponse) => Unit, cookieName: String) extends LoginAuthenticator {
  Security.addProvider(new BouncyCastleProvider)
  private val factory = KeyFactory.getInstance("ECDSA")
  private val publicKey = factory.generatePublic(new X509EncodedKeySpec(Base64.getUrlDecoder.decode(publicKeyString)))

  override def getAuthMethod: String = "__JWT"
  override def validateRequest(req: ServletRequest, resp: ServletResponse, mandatory: Boolean): Authentication = {
    val request = req.asInstanceOf[HttpServletRequest]
    val response = resp.asInstanceOf[HttpServletResponse]
    try {

      val either: Either[String, UserAuthentication] = for {
        c <- Option(request.getCookies).getOrElse(Array.empty).find(_.getName == cookieName).toRight("No cookie")
        msg <- Jwt.decode(c.getValue, publicKey, JwtAlgorithm.allECDSA).toEither.left.map(_.getMessage)
        auth <- ec.validate(msg)
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

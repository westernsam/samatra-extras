package com.springer.samatra.extras.auth

import java.util.concurrent.atomic.AtomicReference

import org.eclipse.jetty.security.AbstractLoginService
import org.eclipse.jetty.util.security.Credential

case class TokenAuthenticatedUser(username: String, application: String, cred: JwtCredentials = new JwtCredentials()) extends AbstractLoginService.UserPrincipal(username, cred) {
  def roles(): Array[String] = cred.auth.get().appRoles(application)
}

class JwtCredentials() extends Credential {
  val auth: AtomicReference[AuthToken] = new AtomicReference[AuthToken]()

  override def check(credentials: Any): Boolean = {
    credentials match {
      case a: AuthToken => auth.set(a); true
      case _ => false
    }
  }
}




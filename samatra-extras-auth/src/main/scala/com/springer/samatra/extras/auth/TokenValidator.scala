package com.springer.samatra.extras.auth

trait AuthToken {
  def appRoles(application: String):Array[String]
  def name: String
}

trait TokenValidator {
  def validate(token: String): Either[String, AuthToken]
}

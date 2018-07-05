package com.springer.samatra.extras.core.http

case class URI(scheme: Option[String],
               authority: Option[String],
               path: String,
               queryString: Option[String] = None,
               fragment: Option[String] = None) {

  def toRelative: URI = copy(scheme = None, authority = None)

  def removeParam(paramName: String): URI = copy(queryString =
    queryString
      .map(_.split("&"))
      .map(_.filterNot(_.startsWith(s"$paramName")))
      .map(_.mkString("&"))
      .filter(!_.isEmpty)
  )

  def addParam(param: (String, String)): URI = copy(queryString =
    queryString
      .map(_ + "&")
      .orElse(Some(""))
      .map(_ + s"${param._1}=${param._2}"))

  def withPath(path: String): URI = copy(path = path)

  lazy val format: String = {
    val relative = s"$path${queryString.map(q => s"?$q").getOrElse("")}${fragment.map(f => s"#$f").getOrElse("")}"

    val schemeStr = scheme.map(_ + ":").getOrElse("")
    val authorityStr = authority.map(h => s"//$h").getOrElse("")

    s"$schemeStr$authorityStr$relative"
  }
}

object URI {

  def parse(uri: String): URI = uri match {
    case regex(_, scheme, _, authority, path, _, queryString, _, fragment) =>
      URI(Option(scheme), Option(authority), path, Option(queryString), Option(fragment))
  }

  private lazy val regex = """^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\?([^#]*))?(#(.*))?""".r
}

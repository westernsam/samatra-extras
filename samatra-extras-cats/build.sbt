val jettyVersion = "9.4.46.v20220331"

libraryDependencies ++=
  Seq(
    "org.typelevel" %% "cats-core" % "2.7.0",
    "org.typelevel" %% "cats-effect" % "3.3.11",
    "org.eclipse.jetty" % "jetty-webapp" % jettyVersion,
    "org.eclipse.jetty" % "jetty-server" % jettyVersion,
    "org.scalatest" %% "scalatest" % "3.2.11" % "test"
  )


name := "samatra-extras"

organization := "com.springer"

version := Option(System.getenv("GO_PIPELINE_LABEL")).getOrElse("LOCAL")

scalaVersion := "2.12.6"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-Xfatal-warnings", "-Xlint")

val jettyVersion = "9.4.10.v20180503"

resolvers += ("Local Ivy Repository" at "file:///" + Path.userHome.absolutePath + "/.ivy2/cache")
resolvers += "jitpack" at "https://jitpack.io"

libraryDependencies ++=
  Seq(
    "com.github.springernature.samatra" %% "samatra" % "v1.5.0",
    "com.github.springernature.samatra" %% "samatra-websockets" % "v1.5.0",

    "org.eclipse.jetty" % "jetty-webapp" % jettyVersion,
    "org.eclipse.jetty" % "jetty-server" % jettyVersion,
    "org.eclipse.jetty" % "jetty-http" % jettyVersion,
    "org.eclipse.jetty" % "jetty-io" % jettyVersion,
    "org.eclipse.jetty" % "jetty-security" % jettyVersion,
    "org.eclipse.jetty" % "jetty-servlet" % jettyVersion,
    "org.eclipse.jetty" % "jetty-servlets" % jettyVersion,
    "org.eclipse.jetty" % "jetty-util" % jettyVersion,
    "org.eclipse.jetty" % "jetty-jmx" % jettyVersion,

    "org.eclipse.jetty.websocket" % "javax-websocket-server-impl" % jettyVersion,

    "org.slf4j" % "slf4j-api" % "1.7.25",
    "com.samskivert" % "jmustache" % "1.14",
    "com.timgroup" % "java-statsd-client" % "3.1.0",
    "org.javassist" % "javassist" % "3.22.0-GA",
    "org.scala-lang.modules" %% "scala-xml" % "1.1.0",
    "org.asynchttpclient" % "async-http-client" % "2.4.7",

    "org.scalatest" %% "scalatest" % "3.0.5" % "test"
  )

val jettyVersion = "9.4.10.v20180503"

name := "samatra-extras"

lazy val commonSettings = Seq(
  organization := "com.springer",
  version := Option(System.getenv("GO_PIPELINE_LABEL")).getOrElse("LOCAL"),
  scalaVersion := "2.12.3",
  scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-Xfatal-warnings", "-Xlint"),
  resolvers += ("Local Ivy Repository" at "file:///" + Path.userHome.absolutePath + "/.ivy2/cache"),
  resolvers += "jitpack" at "https://jitpack.io",
  libraryDependencies ++=
    Seq(
      "com.github.springernature.samatra" %% "samatra" % "v1.5.0",

      "org.eclipse.jetty" % "jetty-webapp" % jettyVersion,
      "org.eclipse.jetty" % "jetty-server" % jettyVersion,
      "org.eclipse.jetty" % "jetty-http" % jettyVersion,
      "org.eclipse.jetty" % "jetty-io" % jettyVersion,
      "org.eclipse.jetty" % "jetty-security" % jettyVersion,
      "org.eclipse.jetty" % "jetty-servlet" % jettyVersion,
      "org.eclipse.jetty" % "jetty-servlets" % jettyVersion,
      "org.eclipse.jetty" % "jetty-util" % jettyVersion,
      "org.eclipse.jetty" % "jetty-jmx" % jettyVersion,

      "org.slf4j" % "slf4j-api" % "1.7.25",
      "org.asynchttpclient" % "async-http-client" % "2.4.7"
    )
)

lazy val `samatra-extras-core` = project.in(file("samatra-extras-core"))
  .settings(commonSettings: _*)

lazy val `samatra-extras-mustache` = project.in(file("samatra-extras-mustache"))
  .settings(commonSettings: _*)
  .dependsOn(`samatra-extras-core`)

lazy val `samatra-extras-xml` = project.in(file("samatra-extras-xml"))
  .settings(commonSettings: _*)
  .dependsOn(`samatra-extras-core`)

lazy val `samatra-extras-routeprinting` = project.in(file("samatra-extras-routeprinting"))
  .settings(commonSettings: _*)
  .dependsOn(`samatra-extras-core`)

lazy val `samatra-extras-statsd` = project.in(file("samatra-extras-statsd"))
  .settings(commonSettings: _*)
  .dependsOn(`samatra-extras-core`)

lazy val `samatra-extras-websockets` = project.in(file("samatra-extras-websockets"))
  .settings(commonSettings: _*)
  .dependsOn(`samatra-extras-core`, `samatra-extras-routeprinting`)

val `samatra-extras`: sbt.Project = project.in(file("."))
  .settings(commonSettings: _*)
  .aggregate(`samatra-extras-core`, `samatra-extras-mustache`, `samatra-extras-xml`, `samatra-extras-statsd`, `samatra-extras-routeprinting`, `samatra-extras-websockets`)


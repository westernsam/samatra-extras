val jettyVersion = "9.4.10.v20180503"

libraryDependencies ++= Seq(
  "com.github.springernature.samatra" %% "samatra-websockets" % "v1.5.0",
  "org.eclipse.jetty.websocket" % "javax-websocket-server-impl" % jettyVersion,
  "org.scalatest" %% "scalatest" % "3.0.5" % "test"
)
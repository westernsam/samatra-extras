val jettyVersion = "9.4.12.v20180830"

libraryDependencies ++= Seq(
  "com.github.springernature.samatra" %% "samatra-websockets" % "v1.5.2",
  "org.eclipse.jetty.websocket" % "javax-websocket-server-impl" % jettyVersion,
  "org.scalatest" %% "scalatest" % "3.0.5" % "test"
)
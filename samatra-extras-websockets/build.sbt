val jettyVersion = "9.4.35.v20201120"

libraryDependencies ++= Seq(
  "com.github.westernsam.samatra" %% "samatra-websockets" % "v1.1",
  "org.eclipse.jetty.websocket" % "javax-websocket-server-impl" % jettyVersion,
  "org.scalatest" %% "scalatest" % "3.2.3" % "test"
)
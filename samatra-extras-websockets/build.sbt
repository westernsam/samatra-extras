val jettyVersion = "9.4.46.v20220331"

libraryDependencies ++= Seq(
  "com.github.westernsam.samatra" %% "samatra-websockets" % "v1.1",
  "org.eclipse.jetty.websocket" % "javax-websocket-server-impl" % jettyVersion,
  "org.scalatest" %% "scalatest" % "3.2.11" % "test"
)
samatra-extras [![](https://jitpack.io/v/westernsam/samatra-extras_2.12.svg)](https://jitpack.io/#westernsam/samatra-extras_2.12)
------------

[Samatra](https://github.com/westernsam/samatra) with batteries included. Documentation [here](https://github.com/westernsam/samatra-extras/wiki)

The aim of Samatra is to have as few dependencies as possible - if fact it only depends on the servlet-api.jar 
Samatra-extras adds more dependencies but offers more out of the box. Specifically it depends on

- Samatra v1.1
- Jetty 9.4.35.v20201120
- Async Http Client 2.12.1
- SLF4J 1.7.30

This provides helpers for:

- Jetty web server configuration
- Gzipping
- Logging
- AsyncHttpClient to scala Future

Other samatra extra libraries exist which add further dependencies:

- samatra-extras-xml - XML responses 
    - ```"org.scala-lang.modules" %% "scala-xml" % "1.3.0"```
- samatra-extras-mustache - Mustache responses
    - ```"com.samskivert" % "jmustache" % "1.15"```
- samatra-extras-statsd - Statsd metrics - web responses and jvm
    - ```"com.timgroup" % "java-statsd-client" % "3.1.0"```
- samatra-extras-routeprinting - route printing on start-up (integrates nicely with Intellij)
    - ```"org.javassist" % "javassist" % "3.27.0-GA"```
- samatra-extras-websockets - 
    - ```"com.github.westernsam.samatra" %% "samatra-websockets" % "v1.1"```
    - ```"org.eclipse.jetty.websocket" % "javax-websocket-server-impl" % 9.4.35.v20201120```

## Supported platforms
- Scala 2.13.4
- Jetty 9.4.35.v20201120

## How to install
- sbt: 
```
resolvers += "jitpack" at "https://jitpack.io",
libraryDependencies += "com.github.westernsam" %% "samatra-extras" % "v1.1"	
```

You may also be interested in [samatra.g8](https://github.com/westernsam/samatra.g8) which is a giter8 / sbt template for generating new samatra projects.
 
## Licensing
The MIT License (MIT)  http://opensource.org/licenses/MIT

Copyright © 2016, 2017 Springer Nature

## Maintenance
Submit issues and PR's to this github.

## How to use
The easiest way to generate a new stub Samatra-extras project is to use the [samatra.g8](https://github.com/westernsam/samatra.g8) template.

enablePlugins(JavaServerAppPackaging)

name := "goticks"

version := "1.0"

scalaVersion := "2.12.6"

organization := "com.goticks" 

libraryDependencies ++= {
  val akkaVersion = "2.5.16"
  Seq(
    "com.typesafe.akka" %% "akka-actor"           % akkaVersion,
    "com.typesafe.akka" %% "akka-stream"          % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j"           % akkaVersion,
    "com.typesafe.akka" %% "akka-http-core"       % "10.1.5",
    "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.5",
    "ch.qos.logback"    %  "logback-classic"      % "1.2.3",

    "com.typesafe.akka" %% "akka-testkit"         % akkaVersion   % Test,
    "org.scalacheck"    %% "scalacheck"           % "1.14.0"      % Test,
    "org.scalatest"     %% "scalatest"            % "3.0.5"       % Test
  )
}

// Assembly settings
mainClass in assembly := Some("com.goticks.Main")

assemblyJarName in assembly := "goticks.jar"
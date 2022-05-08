ThisBuild / scalaVersion := "2.13.8"

lazy val root = (project in file(".")).settings(
  name := "server",
  libraryDependencies += "org.scalikejdbc" %% "scalikejdbc" % "3.5.0",
  libraryDependencies += "com.h2database" % "h2" % "1.4.200",
  libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3",
  libraryDependencies += "mysql" % "mysql-connector-java" % "8.0.28",
  libraryDependencies += "com.typesafe.akka" %% "akka-actor-typed" % "2.6.8",
  libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.2.9",
  libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.6.8",
  libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % "10.2.9"
)

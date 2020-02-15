name := "mr-forge-sync"
organization := "net.gaillourdet"
maintainer := "jm@gaillourdet.net"
version := "0.1-SNAPSHOT"

scalaVersion := "2.13.1"

enablePlugins(JavaAppPackaging)

libraryDependencies ++= Seq(
  "com.github.scopt" %% "scopt" % "4.0.0-RC2",
  "org.gitlab4j" % "gitlab4j-api" % "4.12.0",
  "org.scala-lang.modules" %% "scala-java8-compat" % "0.9.0",
  "org.scalatest" %% "scalatest" % "3.0.8" % Test // available for 2.13
)

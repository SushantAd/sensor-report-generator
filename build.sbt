name := "sensor-report-generator"

version := "0.1"

scalaVersion := "2.13.10"

libraryDependencies ++= Seq(
  "co.fs2" %% "fs2-core" % "3.4.0",
  "co.fs2" %% "fs2-io" % "3.4.0",
  "co.fs2" %% "fs2-reactive-streams" % "3.4.0",
  "co.fs2" %% "fs2-scodec" % "3.4.0",
//Test dependencies
  "org.scalatest" %% "scalatest" % "3.2.14" % Test,
  "org.mockito" %% "mockito-scala" % "1.17.0" % Test
)
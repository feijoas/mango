import sbt._
import Keys._

object Dependencies {
  // this version of guava is used if no cmd-arg is given
  val guavaDefaulVersion = "20.0"

  // set guava version by cmd-arg or default
  val guavaVersion = {
    val arg = System.getenv("GUAVA_VERSION")
    if(arg != null) arg
    else guavaDefaulVersion
  }

  // compile dependencies
  val guava = "com.google.guava" % "guava" % guavaVersion
  val findbugs = "com.google.code.findbugs" % "jsr305" % "1.3.+"

  // test dependencies
  val guavaTestlib = "com.google.guava" % "guava-testlib" % guavaDefaulVersion % "test"
  val junit = "junit" % "junit" % "4.12" % "test"
  val scalatest = "org.scalatest" %% "scalatest" % "3.0.0" % "test"
  val scalacheck = "org.scalacheck" %% "scalacheck" % "1.13.4" % "test"
  val scalamock = "org.scalamock" %% "scalamock-scalatest-support" % "3.2.2" % "test"
  val mockito = "org.mockito" % "mockito-core" % "2.2.11" % "test"

  val deps = Seq(guava, guavaTestlib, junit, scalatest, scalacheck, scalamock, mockito)
}
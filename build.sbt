/**
  * Organization:
  */
organization     := "org.feijoas"

/**
  * Library Meta:
  */
name     := "mango"
version  := "0.14"
licenses := Seq(("Apache License, Version 2.0", url("http://www.apache.org/licenses/LICENSE-2.0")))

/**
  * Scala:
  */
scalaVersion       := "2.12.0"
crossScalaVersions := Seq("2.12.0")

/**
  * Library Dependencies:
  */

// Versions:
val GuavaVersion         = "20.0"

// compile dependencies
val guava = "com.google.guava" % "guava" % GuavaVersion
val findbugs = "com.google.code.findbugs" % "jsr305" % "3.0.1"

// test dependencies
val guavaTestlib = "com.google.guava" % "guava-testlib" % GuavaVersion % "test"
val junit = "junit" % "junit" % "4.12" % "test"
val scalatest = "org.scalatest" %% "scalatest" % "3.0.1" % "test"
val scalacheck = "org.scalacheck" %% "scalacheck" % "1.13.4" % "test"
val scalamock = "org.scalamock" %% "scalamock-scalatest-support" % "3.4.2" % Test
val mockito = "org.mockito" % "mockito-core" % "2.3.11" % "test"

libraryDependencies ++= Seq(guava, findbugs, guavaTestlib, junit, scalatest, scalacheck, scalamock, mockito)

/**
  * Tests:
  */
parallelExecution in Test := false

/**
  * Scoverage:
  */
coverageEnabled in Test := true

/**
  * Publishing to Sonatype:
  */
publishMavenStyle := true

publishArtifact in Test := false

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

pomExtra := {
  <url>mango.feijoas.org</url>
  <licenses>
    <license>
      <name>The Apache Software License, Version 2.0</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>https://github.com/feijoas/mango.git</url>
    <connection>https://github.com/feijoas/mango.git</connection>
  </scm>
  <developers>
    <developer>         
      <id>mschneiderwng</id>
      <name>Markus Schneider</name>
    </developer>
  </developers>
}
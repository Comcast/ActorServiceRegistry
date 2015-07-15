import sbt._
import Keys._

organization := "com.comcast"

name := "akka-service-registry"

version := "1.0"

scalaVersion := "2.11.6"

val akkaVersion = "2.3.11"

val slf4jOrg = "org.slf4j"

val slf4jVersion = "1.7.10"

net.virtualvoid.sbt.graph.Plugin.graphSettings

libraryDependencies += "junit" % "junit" % "4.5" % "test"

libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.1.5" % "test"

libraryDependencies += "org.scala-lang" % "scala-compiler" % scalaVersion.value % "test"

libraryDependencies +=  slf4jOrg % "slf4j-api" % slf4jVersion

libraryDependencies +=  slf4jOrg % "log4j-over-slf4j" % slf4jVersion

libraryDependencies +=  slf4jOrg % "jcl-over-slf4j" % slf4jVersion % "test"

libraryDependencies +=  slf4jOrg % "jul-to-slf4j" % slf4jVersion % "test"

libraryDependencies +=  slf4jOrg % "slf4j-simple" % slf4jVersion

libraryDependencies += "com.typesafe.akka" % "akka-actor_2.11" % akkaVersion

libraryDependencies += "com.typesafe.akka" % "akka-agent_2.11" % akkaVersion

libraryDependencies += "com.typesafe.akka" % "akka-remote_2.11" % akkaVersion

libraryDependencies += "com.typesafe.akka" % "akka-testkit_2.11" % akkaVersion % "test"

libraryDependencies += "com.typesafe.akka" % "akka-slf4j_2.11" % akkaVersion

libraryDependencies += "com.typesafe.akka" %% "akka-persistence-experimental" % akkaVersion excludeAll (ExclusionRule(organization = "io.dropwizard.metrics"))

resolvers += "dnvriend at bintray" at "http://dl.bintray.com/dnvriend/maven"

libraryDependencies += "com.github.dnvriend" %% "akka-persistence-inmemory" % "1.0.3" % "test"

libraryDependencies += "com.typesafe.akka" %% "akka-contrib" % akkaVersion excludeAll (ExclusionRule(organization = "io.dropwizard.metrics"))

libraryDependencies += "com.typesafe.akka" %% "akka-cluster" % akkaVersion excludeAll (ExclusionRule(organization = "io.dropwizard.metrics"))

libraryDependencies += "com.typesafe.akka" % "akka-testkit_2.11" % akkaVersion % "test"

libraryDependencies += "nl.grons" %% "metrics-scala" % "3.3.0_a2.3" excludeAll (ExclusionRule(organization = "com.typesafe.akka"))

libraryDependencies +=  "joda-time" % "joda-time" % "2.7"


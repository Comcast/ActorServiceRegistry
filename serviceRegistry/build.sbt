//
// Copyright  2015  Comcast Cable Communications Management, LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
import sbt._
import Keys._

organization := "com.comcast"

name := "actor-service-registry"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.6"

val akkaVersion = "2.3.11"

val slf4jOrg = "org.slf4j"

val slf4jVersion = "1.7.10"

net.virtualvoid.sbt.graph.Plugin.graphSettings

libraryDependencies += "junit" % "junit" % "4.5" % "test"

libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.1.5" % "test"

libraryDependencies += "org.scala-lang" % "scala-compiler" % scalaVersion.value % "test"

libraryDependencies +=  slf4jOrg % "slf4j-api" % slf4jVersion

libraryDependencies +=  slf4jOrg % "log4j-over-slf4j" % slf4jVersion % "test"

libraryDependencies +=  slf4jOrg % "jcl-over-slf4j" % slf4jVersion % "test"

libraryDependencies +=  slf4jOrg % "jul-to-slf4j" % slf4jVersion % "test"

libraryDependencies +=  slf4jOrg % "slf4j-simple" % slf4jVersion % "test"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % akkaVersion

libraryDependencies += "com.typesafe.akka" %% "akka-agent" % akkaVersion

libraryDependencies += "com.typesafe.akka" %% "akka-remote" % akkaVersion

libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test"

libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % akkaVersion

libraryDependencies += "com.typesafe.akka" %% "akka-persistence-experimental" % akkaVersion excludeAll (ExclusionRule(organization = "io.dropwizard.metrics"))

resolvers += "dnvriend at bintray" at "http://dl.bintray.com/dnvriend/maven"

libraryDependencies += "com.github.dnvriend" %% "akka-persistence-inmemory" % "1.0.3" % "test"

libraryDependencies += "com.typesafe.akka" %% "akka-contrib" % akkaVersion excludeAll (ExclusionRule(organization = "io.dropwizard.metrics"))

libraryDependencies += "com.typesafe.akka" %% "akka-cluster" % akkaVersion excludeAll (ExclusionRule(organization = "io.dropwizard.metrics"))

libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test"

libraryDependencies += "nl.grons" %% "metrics-scala" % "3.3.0_a2.3" excludeAll (ExclusionRule(organization = "com.typesafe.akka"))

libraryDependencies +=  "joda-time" % "joda-time" % "2.7"


artifact in (Compile, assembly) := {
  val art = (artifact in (Compile, assembly)).value
  art.copy(`classifier` = Some("assembly"))
}

addArtifact(artifact in (Compile, assembly), assembly)
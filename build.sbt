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

import bintray.AttrMap
import bintray._

organization := "com.comcast"

name := "actor-service-registry-aggregate"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.6"

test in publish := {}


val doNotPublishSettings = Seq(publish := {})

val MAJOR_MIN_VERSION="1.0"
val DEFAULT_VERSION = s"$MAJOR_MIN_VERSION-SNAPSHOT"
/*
Yes, version is tightly coupled with travis , for now
 */

val Version = sys.props.getOrElse("VERSION",s"${MAJOR_MIN_VERSION}.${sys.props.getOrElse("TRAVIS_BUILD_NUMBER",DEFAULT_VERSION)}")

/*

Most Travis stuff taken from:
http://szimano.org/automatic-deployments-to-jfrog-oss-and-bintrayjcentermaven-central-via-travis-ci-from-sbt/
 */
val rootPublishSettings =
  (if (version.toString.endsWith("-SNAPSHOT"))
    doNotPublishSettings
  else
    Seq(
      organization := "com.comcast.csv",
      version := Version,
      pomExtra := <scm>
        <url>git@github.com:Comcast/ActorServiceRegistry.git</url>
        <connection>git@github.com:Comcast/ActorServiceRegistry.git</connection>
      </scm>
        <developers>
          <developer>
            <id>Gumby</id>
            <name>Team Gumby</name>
            <url>http://www.does.org/~john</url>
          </developer>
        </developers>,
      publishArtifact in Test := false,
      homepage := Some(url("https://github.com/Comcast/ActorServiceRegistry")),
      publishMavenStyle := false,
      resolvers += Resolver.url("supler ivy resolver", url("http://dl.bintray.com/bobra200/maven"))(Resolver.ivyStylePatterns),
      licenses := ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.txt")) :: Nil // this is required! otherwise Bintray will reject the code
    )
    )



lazy val common = Project(id = "common",
                            base = file("common")).settings(rootPublishSettings)

lazy val serviceRegistry = Project(id = "serviceRegistry",
                            base = file("serviceRegistry"))
                            .dependsOn(common).settings(rootPublishSettings)

lazy val root = Project(id = "root",
                            base = file(".")).settings( publish := { } )
                            .aggregate(common, serviceRegistry)



/*
val buildSettings = Defaults.coreDefaultSettings ++ Seq(
  version := ver,
  scalaVersion := "2.11.5",
  scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-language:existentials", "-language:higherKinds"),
  parallelExecution := false
)
*/



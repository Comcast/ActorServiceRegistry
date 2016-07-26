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

resolvers += "Sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.7.4")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.13.0")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.0.4")

addSbtPlugin("me.lessis" % "bintray-sbt" % "0.3.0")

resolvers += Resolver.url(
"bintray-sbt-plugin-releases",
url("http://dl.bintray.com/content/sbt/sbt-plugin-releases"))(
Resolver.ivyStylePatterns)
 
resolvers += Classpaths.sbtPluginReleases
 
resolvers += Classpaths.typesafeReleases
 
addSbtPlugin("com.sksamuel.scoverage" %% "sbt-coveralls" % "0.0.5")
 
addSbtPlugin("com.typesafe.sbt" % "sbt-pgp" % "0.8.3")
 
// Add the following to have Git manage your build versions
resolvers += "jgit-repo" at "http://download.eclipse.org/jgit/maven"
 
addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.6.4")

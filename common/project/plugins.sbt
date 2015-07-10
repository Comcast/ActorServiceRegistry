import sbt._

import Defaults._

resolvers += "Sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.7.4")




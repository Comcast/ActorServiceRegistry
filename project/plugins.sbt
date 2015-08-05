import sbt._

resolvers += "Sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.7.4")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.13.0")

resolvers += "CSV Releases" at "http://nexus.cvs.ula.comcast.net:8081/nexus/content/repositories/releases/"

addSbtPlugin("com.comcast.tvx.infrastructure" % "gumby-common" % "1.2.7")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.0.4")

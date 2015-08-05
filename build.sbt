import sbt._
import Keys._
import gumby.build.{Common}

Common.commonSettings

organization := "com.comcast.csv"

name := "AkkaServiceRegistry"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.6"

lazy val common = Project(id = "common",
                            base = file("common"))

lazy val serviceRegistry = Project(id = "serviceRegistry",
                            base = file("serviceRegistry"))
                            .dependsOn(common)

lazy val root = Project(id = "root",
                            base = file("."))
                            .aggregate(common, serviceRegistry)



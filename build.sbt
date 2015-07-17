import sbt._
import Keys._

organization := "com.comcast"

name := "AkkaServiceRegistry"

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.6"

lazy val common = Project(id = "common",
                            base = file("common"))

lazy val serviceRegistry = Project(id = "serviceRegistry",
                            base = file("serviceRegistry"))
                            .dependsOn(common)

lazy val root = Project(id = "root",
                            base = file("."))
                            .aggregate(common, serviceRegistry)
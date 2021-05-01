import sbt.Keys._
import sbt._

object Settings {
  final val common = Seq[Setting[_]](
    version := (version in ThisBuild).value,
    organization := "org.kittrellcodeworks",
    scalaVersion := "2.12.11",
    scalacOptions := Seq("-Xlint", "-unchecked", "-deprecation", "-feature", "-Xfatal-warnings"),
    resolvers ++= Seq(
      Resolver.mavenLocal,
      Resolver.sonatypeRepo("releases"),
      Resolver.jcenterRepo
    )
  )
}

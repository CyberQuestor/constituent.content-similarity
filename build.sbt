import AssemblyKeys._

assemblySettings

name := "constituent.content-similarity"
scalaVersion := "2.11.8"

organization := "org.haystack"

libraryDependencies ++= Seq(
  "org.apache.predictionio" %% "apache-predictionio-core" % "0.13.0" % "provided",
  "org.apache.spark"        %% "spark-core"               % "2.1.1"             % "provided",
  "org.apache.spark"        %% "spark-mllib"              % "2.1.1"             % "provided",
  "org.scalanlp" %% "breeze" % "0.13.2",
  "org.scalanlp" %% "breeze-natives" % "0.13.2")

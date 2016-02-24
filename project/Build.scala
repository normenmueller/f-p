import sbt._
import Keys._

import com.typesafe.sbt.SbtMultiJvm
import com.typesafe.sbt.SbtMultiJvm.MultiJvmKeys.MultiJvm

object FunctionPassingBuild extends Build with Mappings with Formatter {

  val snapshotsRepo = Resolver.sonatypeRepo("snapshots")

  lazy val standardSettings = Seq(
    scalaVersion := "2.11.7",
    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding", "UTF-8",
      "-feature",
      "-unchecked"
    ), 
    resolvers ++= (if (version.value.endsWith("-SNAPSHOT")) List(snapshotsRepo) else Nil),
    parallelExecution in Global := false,
    unmanagedSourceDirectories in Compile <<= (scalaSource in Compile)(Seq(_)),
    unmanagedSourceDirectories in Test <<= (scalaSource in Test)(Seq(_))
  )

  lazy val `f-p` = Project(
    id = "f-p",
    base = file("."),
    settings = standardSettings,
    aggregate = Seq(core, samples)
  ) 

  val loggingDependencies = Seq(
    "com.typesafe.scala-logging" %% "scala-logging"   % "3.1.0",
    "ch.qos.logback"              % "logback-classic" % "1.1.5"
  )

  lazy val core = Project(
    id = "core",
    base = file("core"),
    settings = standardSettings ++ 
      documentationSettings ++ 
      formatSettings ++ 
      Seq(
        name := "f-p core",
        libraryDependencies ++= Seq(
          "org.scala-lang.modules"     %% "spores-core"     % "0.1.3",
          "org.scala-lang.modules"     %% "spores-pickling" % "0.1.3",
          "org.scala-lang.modules"     %% "scala-pickling"  % "0.10.1", 

          "io.netty"                    % "netty-all"       % "4.1.0.CR1",
          "org.javassist"               % "javassist"       % "3.20.0-GA",

          "org.scalatest"              %% "scalatest"       % "2.2.4" % "test"
        ) ++ loggingDependencies
      )
  )

  lazy val samples = Project(
    id = "samples",
    base = file("samples"),
    settings = standardSettings ++ 
      documentationSettings ++ 
      formatSettings ++ 
      SbtMultiJvm.multiJvmSettings ++ 
      Seq(
        name := "f-p samples",
        scalaSource in Compile := baseDirectory.value / "src"/ "single-jvm" / "main" / "scala",
        resourceDirectory in Compile := baseDirectory.value / "src"/ "single-jvm" / "main" / "resources",
        scalaSource in Test := baseDirectory.value / "src"/ "single-jvm" / "test" / "scala",
        resourceDirectory in Test := baseDirectory.value / "src"/ "single-jvm" / "test" / "resources",
        compile in MultiJvm <<= (compile in MultiJvm) triggeredBy (compile in Test),
        fork in run := true,
        parallelExecution in Test := false,
        libraryDependencies ++= loggingDependencies
      )
  ) dependsOn(core) configs(MultiJvm) 

}


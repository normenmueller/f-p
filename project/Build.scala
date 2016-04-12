import sbt._
import Keys._

import com.typesafe.sbt.SbtMultiJvm
import com.typesafe.sbt.SbtMultiJvm.MultiJvmKeys.MultiJvm

object Build extends Build with Mappings {

  val snapshotsRepo = Resolver.sonatypeRepo("snapshots")

  lazy val standardSettings = Seq(
    scalaVersion := "2.11.7",
    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding", "UTF-8",
      "-feature",
      "-unchecked",
      "-Xlog-implicits",
      "-language:experimental.macros"
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

  val macroSettings = Seq(
    libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-library" % _),
    libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-reflect" % _),
    libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-compiler" % _)
  )

  lazy val core = Project(
    id = "core",
    base = file("core"),
    settings = standardSettings ++
      documentationSettings ++
      macroSettings ++
      Seq(
        name := "f-p core",
        libraryDependencies ++= Seq(
          "org.scala-lang.modules"     %% "spores-core"     % "0.2.4-M2",
          "org.scala-lang.modules"     %% "spores-pickling" % "0.2.4-M2",
          "org.scala-lang.modules"     %% "scala-pickling"  % "0.11.0-M2",

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
      SbtMultiJvm.multiJvmSettings ++
      Seq(
        name := "f-p samples",
        /* Detect both multi-jvm and single-jvm examples */
        scalaSource in Compile := baseDirectory.value / "src",
        resourceDirectory in Compile :=
          baseDirectory.value / "src"/ "single-jvm" / "main" / "resources",
        /* Multi-jvm doesn't have tests so we don't care to add them here */
        scalaSource in Test :=
          baseDirectory.value / "src"/ "single-jvm" / "test" / "scala",
        resourceDirectory in Test :=
          baseDirectory.value / "src"/ "single-jvm" / "test" / "resources",
        compile in MultiJvm <<= (compile in MultiJvm) triggeredBy (compile in Test),
        fork in run := true,
        parallelExecution in Test := false,
        libraryDependencies ++= loggingDependencies
      )
  ) dependsOn(core) configs(MultiJvm)

}


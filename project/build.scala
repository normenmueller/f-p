import sbt._
import Keys._

import scalariform.formatter.preferences._
import com.typesafe.sbt.SbtScalariform.autoImport.scalariformPreferences
import com.typesafe.sbt.SbtScalariform

//import com.typesafe.sbteclipse.plugin.EclipsePlugin.EclipseKeys

import com.typesafe.sbt.SbtMultiJvm
import com.typesafe.sbt.SbtMultiJvm.MultiJvmKeys.MultiJvm

object build extends Build {
  type Sett = Def.Setting[_]

  lazy val standardSettings: Seq[Sett] = Seq[Sett](
    scalaVersion := "2.11.7",
    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding", "UTF-8",
      "-feature",
      "-unchecked"
    ), 
    resolvers ++= (if (version.value.endsWith("-SNAPSHOT")) List(Resolver.sonatypeRepo("snapshots")) else Nil),
    parallelExecution in Global := false,
    testOptions in Test += Tests.Argument(TestFrameworks.JUnit, "-q", "-v", "-s"),
    unmanagedSourceDirectories in Compile <<= (scalaSource in Compile)(Seq(_)),
    unmanagedSourceDirectories in Test <<= (scalaSource in Test)(Seq(_))
  )  

  //lazy val eclipseSettings: Seq[Sett] = Seq[Sett](
  //  EclipseKeys.eclipseOutput := Some(".target"),
  //  EclipseKeys.withSource := true
  //)

  // http://docs.scala-lang.org/style/
  lazy val formatSettings: Seq[Sett] = SbtScalariform.defaultScalariformSettings ++ Seq(
    scalariformPreferences := scalariformPreferences.value
      .setPreference(AlignParameters, false)
      .setPreference(AlignSingleLineCaseStatements, true)
      .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 40)
      .setPreference(CompactControlReadability, false)
      .setPreference(CompactStringConcatenation, false)
      .setPreference(DoubleIndentClassDeclaration, true)
      .setPreference(FormatXml, true)
      .setPreference(IndentLocalDefs, false)
      .setPreference(IndentPackageBlocks, true)
      .setPreference(IndentSpaces, 2)
      .setPreference(IndentWithTabs, false)
      .setPreference(MultilineScaladocCommentsStartOnFirstLine, true)
      .setPreference(PlaceScaladocAsterisksBeneathSecondAsterisk, true)
      .setPreference(PreserveSpaceBeforeArguments, true)
      .setPreference(DanglingCloseParenthesis, Preserve)
      .setPreference(RewriteArrowSymbols, false)
      .setPreference(SpaceBeforeColon, false)
      .setPreference(SpaceInsideBrackets, false)
      .setPreference(SpaceInsideParentheses, false)
      .setPreference(SpacesWithinPatternBinders, true)
  )

  lazy val `f-p` = Project(
    id = "f-p",
    base = file("."),
    settings = standardSettings,
    aggregate = Seq(core, samples)
  ) 

  lazy val core = Project(
    id = "core",
    base = file("core"),
    settings = standardSettings ++ formatSettings ++ SbtMultiJvm.multiJvmSettings ++ Seq[Sett](
      name := "f-p core",
      libraryDependencies ++= Seq(
        "org.scala-lang.modules"     %% "spores-core"     % "0.1.3",
        "org.scala-lang.modules"     %% "spores-pickling" % "0.1.3",
        "com.typesafe.akka"           % "akka-actor_2.11" % "2.3.12",
        "io.netty"                    % "netty-all"       % "4.0.33.Final",
        "com.typesafe.scala-logging" %% "scala-logging"   % "3.1.0",
        "ch.qos.logback"              % "logback-classic" % "1.1.3",
        "junit"                       % "junit-dep"       % "4.11" % "test",
        "com.novocode"                % "junit-interface" % "0.11" % "test"
      ),
      compile in MultiJvm <<= (compile in MultiJvm) triggeredBy (compile in Test)
    )
  ) configs (MultiJvm)

  lazy val samples = Project(
    id = "samples",
    base = file("samples"),
    settings = standardSettings ++ formatSettings ++ Seq[Sett](
      name := "f-p samples",
      libraryDependencies ++= Seq(
        "com.typesafe.scala-logging" %% "scala-logging"   % "3.1.0",
        "ch.qos.logback"              % "logback-classic" % "1.1.3"
      )
    )
  ) dependsOn(`core`)

}

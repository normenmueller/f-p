import com.typesafe.sbt.SbtScalariform
import scalariform.formatter.preferences._
import com.typesafe.sbt.SbtScalariform.autoImport.scalariformPreferences

trait Scalariform {

  /* Follow [[http://docs.scala-lang.org/style/ Scala style guide]] */
  lazy val formatSettings = SbtScalariform.defaultScalariformSettings ++ Seq(
    scalariformPreferences := scalariformPreferences.value
      .setPreference(AlignParameters, false)
      .setPreference(AlignSingleLineCaseStatements, false)
      .setPreference(CompactControlReadability, false)
      .setPreference(CompactStringConcatenation, false)
      .setPreference(DoubleIndentClassDeclaration, false)
      //.setPreference(DoubleIndentMethodDeclaration, true)
      .setPreference(FormatXml, true)
      .setPreference(IndentLocalDefs, false)
      .setPreference(IndentPackageBlocks, true)
      .setPreference(IndentSpaces, 2)
      .setPreference(IndentWithTabs, false)
      //.setPreference(NewLineAtEndOfFile, true)
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

}


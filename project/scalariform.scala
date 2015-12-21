import scalariform.formatter.preferences._
import com.typesafe.sbt.SbtScalariform.autoImport.scalariformPreferences
import com.typesafe.sbt.SbtScalariform

trait Formatting {

  // http://docs.scala-lang.org/style/
  lazy val formatSettings = SbtScalariform.defaultScalariformSettings ++ Seq(
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

}

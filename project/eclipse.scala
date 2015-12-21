import com.typesafe.sbteclipse.plugin.EclipsePlugin.EclipseKeys

trait Eclipse {

  lazy val eclipseSettings = Seq(
    EclipseKeys.eclipseOutput := Some(".target"),
    EclipseKeys.withSource := true
  )

}

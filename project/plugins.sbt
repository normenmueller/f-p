// https://github.com/sbt/sbt-multi-jvm
addSbtPlugin("com.typesafe.sbt" % "sbt-multi-jvm" % "0.3.11")

// https://github.com/daniel-trinh/sbt-scalariform 
//addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.6.0")

// Compiled from `https://github.com/normenmueller/sbt-scalariform`
// and published locally. Why? cf.
// [[https://github.com/daniel-trinh/sbt-scalariform/issues/15]]
addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.6.1")

// Eclipse integration
// Required to mix in trait `Eclipse` located at `eclipse.scala`.
// Currently disabled by default.
//addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "4.0.0")

// Allow support for running code (like tests) in different JVMs
addSbtPlugin("com.typesafe.sbt" % "sbt-multi-jvm" % "0.3.11")

// Add scalariform separately
resolvers += "Sonatype OSS Releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2"

addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.6.0")

// Add scalastyle plugin
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "0.8.0")

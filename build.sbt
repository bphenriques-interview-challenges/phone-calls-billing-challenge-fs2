ThisBuild / scalaVersion := "2.13.8"
ThisBuild / organization := "com.bphenriques"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organizationName := "bphenriques"

lazy val root = (project in file("."))
  .settings(
    name := "billing-fs2",
    maintainer := "4727729+bphenriques@users.noreply.github.com",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "2.7.0",
      "org.typelevel" %% "cats-effect" % "3.3.11",
      "co.fs2" %% "fs2-core" % "3.2.7",
      "co.fs2" %% "fs2-io" % "3.2.7",
      "org.gnieh" %% "fs2-data-csv" % "1.3.1",

      // Logging
      "org.typelevel" %% "log4cats-core" % "2.3.1",
      "org.typelevel" %% "log4cats-slf4j" % "2.3.1",
      "ch.qos.logback" % "logback-classic" % "1.2.11" % Runtime,

      // Testing
      "org.scalameta" %% "munit" % "0.7.29" % Test,
      "org.typelevel" %% "munit-cats-effect-3" % "1.0.7" % Test
    )
  )
  .enablePlugins(JavaAppPackaging)
  .settings(packagingSettings)

lazy val packagingSettings = Seq(
  Compile / mainClass := Some("com.bphenriques.billing.Main"),

  // Add documentation files.
  Universal / mappings ++= Seq(
    file("README.md") -> "README.md",
    file("LICENSE") -> "LICENSE"
  )
)

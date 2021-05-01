
lazy val core = (project in file("core"))
  .settings(Settings.common: _*)
  .settings(
    name := "repo-core",
    libraryDependencies ++= Seq(
      Deps.scalatest
    ),
    scalacOptions += "-language:higherKinds"
  )

lazy val mem = (project in file("mem"))
  .settings(Settings.common: _*)
  .settings(
    name := "repo-mem",
    libraryDependencies ++= Seq(
      Deps.scalatest,
      Deps.shapeless,
      Deps.slf4jSimple,
    ),
    scalacOptions += "-language:higherKinds"
  )
  .dependsOn(core % "compile->compile;test->test")

lazy val solr = (project in file("solr"))
  .settings(Settings.common: _*)
  .settings(
    name := "repo-solr",
    libraryDependencies ++= Seq(
      Deps.scalatest,
      Deps.slf4jSimple,
      Deps.solr,
      Deps.solrTestFramework,
    ),
    scalacOptions += "-language:higherKinds"
  )
  .dependsOn(core % "compile->compile;test->test")

lazy val mongo = (project in file("mongo"))
  .settings(Settings.common: _*)
  .settings(
    name := "repo-mongo",
    libraryDependencies ++= Seq(
      Deps.embeddedmongo,
      Deps.reactivemongo,
      Deps.scalatest,
      Deps.shapeless,
      Deps.slf4jSimple,
    )
  )
  .dependsOn(core % "compile->compile;test->test")

lazy val hibernate = (project in file("hibernate"))
  .settings(Settings.common: _*)
  .settings(
    name := "repo-hibernate",
    libraryDependencies ++= Seq(
      Deps.h2 % Test,
      Deps.hibernate,
      Deps.scalatest,
      Deps.slf4jSimple,
    )
  )
  .dependsOn(core % "compile->compile;test->test")

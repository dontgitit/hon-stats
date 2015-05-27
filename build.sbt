name := "hon"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  "org.postgresql" % "postgresql" % "9.3-1100-jdbc4",
  "com.typesafe.slick" %% "slick" % "2.1.0"
)

play.Project.playScalaSettings

lazy val root = project
  .in(file("."))
  .settings(
    name := "user-backend",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := "3.7.1",
  )

libraryDependencies ++= Seq(
  "com.greenfossil" %% "thorium" % "0.10.10" withSources(),
  "org.slf4j" % "slf4j-api" % "2.0.17",
  "ch.qos.logback" % "logback-classic" % "1.5.32" % Test,
  "org.scalameta" %% "munit" % "1.2.3" % Test,
  "org.typelevel" %% "cats-core" % "2.13.0",
  "org.mindrot" % "jbcrypt" % "0.4",
  "redis.clients" % "jedis" % "7.3.0"
)

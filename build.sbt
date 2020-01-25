name := "bank-transfer-api"
version := "1.0"

lazy val `bank-transfer-api` = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.11"

libraryDependencies ++= Seq(
  javaJdbc , cache , javaWs, javaJpa,
  "org.projectlombok" % "lombok" % "1.18.2",

  "com.h2database" % "h2" % "1.4.192",
  "org.hibernate" % "hibernate-core" % "5.2.16.Final",
  "com.google.code.gson" % "gson" % "2.8.5",

  "org.assertj" % "assertj-core" % "3.8.0" % "test",
  "org.mockito" % "mockito-all" % "1.10.19" % "test",
  "junit" % "junit" % "4.11" % "test"
)
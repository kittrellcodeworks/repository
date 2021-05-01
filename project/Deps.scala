import sbt._

object Deps {
  val embeddedmongo = "com.github.simplyscala" %% "scalatest-embedmongo" % "0.2.4" % Test
  val h2 = "com.h2database" % "h2" % "1.4.196"
  val hibernate = "org.hibernate" % "hibernate-core" % "5.4.30.Final"
  val reactivemongo = "org.reactivemongo" %% "reactivemongo" % "1.0.3"
  val scalatest = "org.scalatest" %% "scalatest" % "3.0.5" % Test
  val shapeless = "com.chuusai" %% "shapeless" % "2.3.3"
  val slf4jSimple = "org.slf4j" % "slf4j-simple" % "1.7.25" % Test
  val solr = "org.apache.solr" % "solr-core" % "7.2.1"
  val solrTestFramework = "org.apache.solr" % "solr-test-framework" % "7.2.1" % Test
}

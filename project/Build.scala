import sbt._
import Keys._
import play.Project._
 
object ApplicationBuild extends Build {
 val appName = "play-jpa-kundera"
 val appVersion = "1.0-SNAPSHOT"
 
 val appDependencies = Seq(
 "com.impetus.client" % "kundera-cassandra" % "2.5",
 javaCore
 )
 
val main = play.Project(appName, appVersion, appDependencies).settings(
 //Kundera Public repositories
 ebeanEnabled := false,
 resolvers += "Kundera" at "https://oss.sonatype.org/content/repositories/releases",
 resolvers += "Riptano" at "http://mvn.riptano.com/content/repositories/public",
 resolvers += "Kundera missing" at "http://kundera.googlecode.com/svn/maven2/maven-missing-resources",
 resolvers += "Local Maven Repository" at "file:///home/tep/.m2/mvnrepo"
 )
}

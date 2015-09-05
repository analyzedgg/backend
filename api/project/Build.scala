import sbt._
import io.gatling.sbt.GatlingPlugin

object DefaultBuild extends Build {
  lazy val root =
    Project("api", file("."))
      .configs( IntegrationTest )
      .settings( Defaults.itSettings : _*)
}
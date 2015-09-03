import sbt._

object DefaultBuild extends Build {
  lazy val root =
    Project("api", file("."))
      .configs( IntegrationTest )
      .settings( Defaults.itSettings : _*)
}
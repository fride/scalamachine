import sbt._
import Keys._

object BuildSettings {

  val org = "com.github.jrwest"
  val vsn = "0.0.0-SNAPSHOT"
  val scalaVsn = "2.9.1"

  val standardSettings = Defaults.defaultSettings ++ Seq(
    organization := org,
    version := vsn,
    scalaVersion := scalaVsn,
    shellPrompt <<= ShellPrompt.prompt,
    scalacOptions ++= Seq("-deprecation", "-unchecked")
  )

}

object Dependencies {
  lazy val scalaz7       = "org.scalaz"              %% "scalaz-core"    % "7.0-SNAPSHOT"   % "compile" withSources()
  lazy val liftweb       = "net.liftweb"             %% "lift-webkit"    % "2.5-SNAPSHOT"   % "compile" withSources()
  lazy val logback       = "ch.qos.logback"          % "logback-classic" % "1.0.0"          % "compile" withSources()
  lazy val specs2        = "org.specs2"              %% "specs2"         % "1.8.2"          % "test" withSources()
  lazy val scalacheck    = "org.scala-tools.testing" %% "scalacheck"     % "1.9"            % "test" withSources()
  lazy val mockito       = "org.mockito"             % "mockito-all"     % "1.9.0"          % "test" withSources()
  lazy val hamcrest      = "org.hamcrest"            % "hamcrest-all"    % "1.1"            % "test" withSources()
}

object ScalamachineBuild extends Build {
  import BuildSettings._
  import Dependencies._

  lazy val scalamachine = Project("scalamachine", file("."),
    settings = standardSettings,
    aggregate = Seq(core,lift)
  )

  lazy val core = Project("scalamachine-core", file("core"),
    settings = standardSettings ++
      Seq(
        name := "scalamachine-core",
        libraryDependencies ++= Seq(scalaz7,specs2,scalacheck,mockito,hamcrest)
      )
  )
  
  lazy val lift = Project("scalamachine-lift", file("lift"),
    dependencies = Seq(core), 
    settings = standardSettings ++
      Seq(
        name := "scalamachine-lift",
        libraryDependencies ++= Seq(liftweb)
      )
  )
  
  lazy val liftExample = Project("scalamachine-lift-example", file("examples/lift"),
    dependencies = Seq(lift),
    settings = standardSettings ++ 
      Seq(
        name := "scalamachine-lift-example",
        libraryDependencies ++= Seq(logback)
      )
  )

}

object ShellPrompt {
  val prompt = name(name => { state: State =>
    object devnull extends ProcessLogger {
      def info(s: => String) {}
      def error(s: => String) { }
      def buffer[T](f: => T): T = f
    }
    val current = """\*\s+(\w+)""".r
    def gitBranches = ("git branch --no-color" lines_! devnull mkString)
    "%s | %s> " format (
      name,
      current findFirstMatchIn gitBranches map (_.group(1)) getOrElse "-"
      )
  })
}


import Dependencies.V
import com.github.sbt.git.SbtGit.GitKeys._
import org.scalajs.linker.interface.ModuleSplitStyle

addCommandAlias("ci", "; lint; test; publishLocal")
addCommandAlias(
  "lint",
  "; scalafmtSbtCheck; scalafmtCheckAll; Compile/scalafix --check; Test/scalafix --check"
)
addCommandAlias("fix", "; Compile/scalafix; Test/scalafix; scalafmtSbt; scalafmtAll")
addCommandAlias("make-package", "; cli/Universal/packageBin")
addCommandAlias("make-stage", "; cli/Universal/stage")

inThisBuild(
  List(
    dynverSeparator := "-",
    dynverSonatypeSnapshots := false
  )
)

val sharedSettings = Seq(
  organization := "com.github.eikek",
  scalaVersion := V.scala3,
  scalacOptions ++=
    Seq(
      "-feature",
      "-deprecation",
      "-unchecked",
      "-encoding",
      "UTF-8",
      "-language:higherKinds",
      "-Xkind-projector:underscores",
      "-Werror",
      "-indent",
      "-print-lines",
      "-Wunused:all"
    ),
  Compile / console / scalacOptions := Seq(),
  Test / console / scalacOptions := Seq(),
  licenses := Seq(
    "GPL-3.0-or-later" -> url("https://spdx.org/licenses/GPL-3.0-or-later")
  ),
  homepage := Some(url("https://github.com/eikek/keeper")),
  versionScheme := Some("early-semver")
) ++ publishSettings

lazy val publishSettings = Seq(
  developers := List(
    Developer(
      id = "eikek",
      name = "Eike Kettner",
      url = url("https://github.com/eikek"),
      email = ""
    )
  ),
  Test / publishArtifact := false
)

lazy val noPublish = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false
)

val testSettings = Seq(
  libraryDependencies ++= (Dependencies.munit ++
    Dependencies.fs2Jvm ++
    Dependencies.borer).map(_ % Test),
  testFrameworks += TestFrameworks.MUnit
)

val scalafixSettings = Seq(
  semanticdbEnabled := true, // enable SemanticDB
  semanticdbVersion := scalafixSemanticdb.revision // use Scalafix compatible version
)

val buildInfoSettings = Seq(
  buildInfoKeys := Seq[BuildInfoKey](
    name,
    version,
    scalaVersion,
    sbtVersion,
    gitHeadCommit,
    gitHeadCommitDate,
    gitUncommittedChanges,
    gitDescribedVersion
  ),
  buildInfoOptions ++= Seq(BuildInfoOption.ToMap, BuildInfoOption.BuildTime),
  buildInfoPackage := "keeper"
)

val writeVersion = taskKey[Unit]("Write version into a file for CI to pick up")

lazy val common = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .withoutSuffixFor(JVMPlatform)
  .in(file("modules/common"))
  .settings(sharedSettings)
  .settings(testSettings)
  .settings(scalafixSettings)
  .settings(
    name := "keeper-common",
    description := "Shared code for data types and utilities",
    libraryDependencies ++=
      Dependencies.catsEffect.value ++
        Dependencies.fs2Core.value ++
        Dependencies.borerJs.value ++
        Dependencies.monocle.value
  )

lazy val commonJs = common.js
lazy val commonJvm = common.jvm

lazy val core = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .withoutSuffixFor(JVMPlatform)
  .in(file("modules/core"))
  .settings(sharedSettings)
  .settings(testSettings)
  .settings(scalafixSettings)
  .settings(
    name := "keeper-core",
    description := "Core data structures",
    libraryDependencies ++=
      Dependencies.catsEffect.value ++
        Dependencies.fs2Core.value ++
        Dependencies.borerJs.value ++
        Dependencies.monocle.value
  )
  .dependsOn(
    common % "compile->compile;test->test"
  )
lazy val coreJs = core.js
lazy val coreJvm = core.jvm

lazy val http4sBorer = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full)
  .withoutSuffixFor(JVMPlatform)
  .in(file("modules/http4s-borer"))
  .settings(sharedSettings)
  .settings(testSettings)
  .settings(scalafixSettings)
  .settings(
    name := "keeper-http4s-borer",
    description := "Use borer codecs with http4s",
    libraryDependencies ++=
      Dependencies.borerJs.value ++
        Dependencies.http4sCore.value ++
        Dependencies.fs2Core.value
  )
lazy val http4sBorerJs = http4sBorer.js
lazy val http4sBorerJvm = http4sBorer.jvm

lazy val strava = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full)
  .withoutSuffixFor(JVMPlatform)
  .in(file("modules/strava"))
  .settings(sharedSettings)
  .settings(testSettings)
  .settings(scalafixSettings)
  .settings(
    name := "keeper-strava",
    description := "Strava client supporting keeper",
    libraryDependencies ++=
      Dependencies.http4sJsClient.value ++
        Dependencies.borerJs.value ++
        Dependencies.fs2Core.value
  )
  .jvmSettings(
    libraryDependencies ++=
      Dependencies.http4sServer ++
        Dependencies.fs2Jvm ++
        Dependencies.scribe
  )
  .dependsOn(http4sBorer, common)

lazy val stravaJvm = strava.jvm
lazy val stravaJs = strava.js

lazy val bikes = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full)
  .withoutSuffixFor(JVMPlatform)
  .in(file("modules/bikes"))
  .settings(sharedSettings)
  .settings(testSettings)
  .settings(scalafixSettings)
  .settings(
    name := "keeper-bikes",
    description := "Manage bike configurations",
    libraryDependencies ++= Seq.empty
  )
  .jvmSettings(
    libraryDependencies ++=
      Dependencies.fs2Jvm ++
        Dependencies.skunk ++
        Dependencies.http4sClientJvm ++
        Dependencies.scribe,
    Test / testOptions += Tests.Setup(_ => PostgresServer.start()),
    Test / testOptions += Tests.Cleanup(_ => PostgresServer.stop())
  )
  .dependsOn(
    core % "compile->compile;test->test",
    common % "compile->compile;test->test"
  )

lazy val bikesJvm = bikes.jvm.dependsOn(http4sBorerJvm, stravaJvm)
lazy val bikesJs = bikes.js

lazy val client = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .withoutSuffixFor(JVMPlatform)
  .in(file("modules/client"))
  .settings(sharedSettings)
  .settings(testSettings)
  .settings(scalafixSettings)
  .settings(
    name := "keeper-client",
    description := "HTTP Client",
    libraryDependencies ++=
      Dependencies.catsEffect.value ++
        Dependencies.fs2Core.value ++
        Dependencies.borerJs.value ++
        Dependencies.monocle.value ++
        Dependencies.http4sJsClient.value ++
        Dependencies.scribeJs.value
  )
  .dependsOn(
    common % "compile->compile;test->test",
    core % "compile->compile;test->test",
    http4sBorer % "compile->compile;test->test",
    bikes % "compile->compile;test->test"
  )

lazy val server = project
  .in(file("modules/server"))
  .settings(sharedSettings)
  .settings(testSettings)
  .settings(scalafixSettings)
  .settings(
    name := "keeper-server",
    description := "HTTP Api",
    libraryDependencies ++=
      Dependencies.http4sServer ++
        Dependencies.fs2Jvm ++
        Dependencies.catsParse ++
        Dependencies.scribe
  )
  .dependsOn(coreJvm, http4sBorerJvm, commonJvm, bikesJvm, client.jvm)

val webclientTimezones = Set(
  "Europe/Berlin",
  "Europe/London"
)

lazy val webview =
  crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Full)
    .withoutSuffixFor(JVMPlatform)
    .in(file("modules/webview"))
    .enablePlugins(ScalaJSPlugin)
    .settings(sharedSettings)
    .settings(testSettings)
    .settings(scalafixSettings)
    .settings(
      name := "keeper-webview",
      description := "View activities in a browser",
      scalaVersion := V.scala3,
      libraryDependencies ++=
        Dependencies.fs2Core.value ++
          Dependencies.borerJs.value
    )
    .jsSettings(
      scalaJSUseMainModuleInitializer := true,
      scalaJSLinkerConfig ~= {
        _.withModuleKind(ModuleKind.ESModule)
          .withModuleSplitStyle(
            ModuleSplitStyle.SmallModulesFor(List("keeper.webview.client"))
          )
      },
      zonesFilter := { (z: String) =>
        webclientTimezones.contains(z)
      },
      Compile / sourceGenerators += Def.task {
        val log = streams.value.log
        val buildForProd =
          sys.env.get("KEEPER_BUILD_PROD").exists(_.equalsIgnoreCase("true"))
        val uri = if (buildForProd) "/api" else "http://localhost:8182/api"
        log.info(s"Using api uri: $uri")

        val srcDev =
          s"""package keeper.webview.client
             |// This file is generated.
             |import org.http4s.implicits.*
             |
             |private class BaseUrlImpl extends BaseUrl {
             |  val get = uri"$uri"
             |}
            """.stripMargin

        val target = (Compile / sourceManaged).value / "scala" / "BaseUrlImpl.scala"
        IO.write(target, srcDev)
        Seq(target)
      },
      libraryDependencies ++=
        Dependencies.scalaJsDom.value ++
          Dependencies.calico.value ++
          Dependencies.fs2Core.value ++
          Dependencies.scalaJsJavaTime.value ++
          Dependencies.http4sJsClient.value ++
          Dependencies.http4sDom.value ++
          Dependencies.scribeJs.value
    )
    .dependsOn(
      bikes % "compile->compile;test->test",
      core % "compile->compile;test->test",
      http4sBorer % "compile->compile;test->test",
      client % "compile->compile;test->test"
    )

lazy val webviewJvm = webview.jvm
lazy val webviewJs = webview.js.enablePlugins(TzdbPlugin)

lazy val cli = project
  .in(file("modules/cli"))
  .enablePlugins(JavaAppPackaging, ClasspathJarPlugin, BuildInfoPlugin)
  .settings(sharedSettings)
  .settings(testSettings)
  .settings(scalafixSettings)
  .settings(buildInfoSettings)
  .settings(
    name := "keeper-cli",
    description := "A command line interface to look at your fit files",
    libraryDependencies ++=
      Dependencies.fs2Jvm ++
        Dependencies.decline ++
        Dependencies.borer ++
        Dependencies.ciris ++
        Dependencies.http4sClientJvm ++
        Dependencies.scribe,
    writeVersion := {
      val out = (LocalRootProject / target).value / "version.txt"
      val versionStr = version.value
      IO.write(out, versionStr)
    },
    Universal / mappings := {
      val allMappings = (Universal / mappings).value
      allMappings.filter {
        // scalajs artifacts are not needed at runtime
        case (file, name) => !name.contains("_sjs1_")
      }
    }
  )
  .dependsOn(
    coreJvm % "compile->compile;test->test",
    bikesJvm % "compile->compile;test->test",
    server % "compile->compile;test->test"
  )

lazy val root = project
  .in(file("."))
  .settings(sharedSettings)
  .settings(noPublish)
  .settings(
    name := "keeper-root"
  )
  .aggregate(
    commonJvm,
    commonJs,
    coreJvm,
    coreJs,
    bikesJvm,
    bikesJs,
    http4sBorerJs,
    http4sBorerJvm,
    stravaJvm,
    stravaJs,
    webviewJs,
    webview.jvm,
    server,
    client.jvm,
    client.js,
    cli
  )

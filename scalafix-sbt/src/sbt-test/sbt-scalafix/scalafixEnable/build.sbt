val V = _root_.scalafix.Versions

// 2.10 is not supported, scalafix is not enabled
lazy val scala210 = project.settings(
  scalaVersion := "2.10.4",
  libraryDependencies := Nil,
  scalacOptions := Nil
)

// 2.12.0 is supported but the version is overidden
lazy val overridesSettings = project.settings(
  scalaVersion := "2.12.0",
  libraryDependencies := Nil,
  scalacOptions := Nil
)

// 2.12.3 is supported
lazy val isEnabled = project.settings(
  scalaVersion := V.scala212
)

TaskKey[Unit]("check") := {
  // nothing should change for the 2.10 project
  assert(scalaVersion.in(scala210).value == "2.10.4")
  assert(libraryDependencies.in(scala210).value.isEmpty)
  assert(scalacOptions.in(scala210).value.isEmpty)

  // 2.12.0 should be overidden to 2.12.X
  assert(scalaVersion.in(overridesSettings).value == V.scala212)
  assert(libraryDependencies.in(overridesSettings).value.nonEmpty)
  assert(scalacOptions.in(overridesSettings).value.contains("-Yrangepos"))

  // 2.12.X should not change that much
  assert(scalacOptions.in(isEnabled).value.count(_ == "-Yrangepos") == 1)
}

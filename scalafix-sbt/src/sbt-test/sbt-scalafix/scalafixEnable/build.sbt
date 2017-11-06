val V = _root_.scalafix.Versions

lazy val scala210 = project.settings(
  scalaVersion := "2.10.4",
  libraryDependencies := Nil,
  scalacOptions := Nil
)

lazy val overridesSettings = project.settings(
  scalaVersion := "2.12.0",
  libraryDependencies := Nil,
  scalacOptions := Nil
)

lazy val isEnabled = project.settings(
  scalaVersion := V.scala212
)

TaskKey[Unit]("check") := {
  assert(scalaVersion.in(scala210).value == "2.10.4")
  assert(scalaVersion.in(overridesSettings).value == V.scala212)

  assert(libraryDependencies.in(overridesSettings).value.nonEmpty)
  assert(libraryDependencies.in(scala210).value.isEmpty)

  assert(scalacOptions.in(overridesSettings).value.contains("-Yrangepos"))
  assert(scalacOptions.in(isEnabled).value.count(_ == "-Yrangepos") == 1)
  assert(scalacOptions.in(scala210).value.isEmpty)
}

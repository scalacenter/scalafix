lazy val x = {
  unmanagedSourceDirectories <+= Def.setting(file("nested"))
  "x"
}

unmanagedSourceDirectories <+= Def.setting(file("top"))

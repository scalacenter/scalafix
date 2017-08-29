lazy val x = {
  unmanagedSourceDirectories.in(Compile) <+= Def.setting(file("nested"))
  "x"
}

unmanagedSourceDirectories.in(Compile) <+= Def.setting(file("top"))

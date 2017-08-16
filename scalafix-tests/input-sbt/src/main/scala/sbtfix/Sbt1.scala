/*
rewrite = Sbt1
 */
package sbtfix

import sbt._

object Sbt1 {
  object `<++=` {
    val key = taskKey[Seq[File]]("Seed.")
    val target = taskKey[Seq[File]]("Target to be reassigned.")
    key := List(new File("."))
    target <++= key
    target <++= (key)
    target <++= (key in ThisBuild)
    target <++= Def.task(key.value)
    target <++= Def.task {
      println("Executing task.")
      (key).value
    }
  }

  object `<+=` {
    val key = taskKey[File]("Single.")
    val target = taskKey[Seq[File]]("Target to be reassigned.")
    key := new File(".")
    target <+= key
    target <+= (key)
    target <+= (key in ThisBuild)
    target <+= Def.task(key.value)
    target.<+=[File](Def.task(key.value))
    target <+= Def.task {
      println("Executing task.")
      (key).value
    }
  }

  object `<<=` {
    import Keys.{compile, name}
    lazy val test1 = taskKey[sbt.inc.Analysis]("Test 1.")
    test1 <<= (compile in Compile)
    lazy val test1b = taskKey[sbt.inc.Analysis]("Test 1b.")
    test1b <<= compile in Compile
    lazy val test2 = taskKey[sbt.inc.Analysis]("Test 2.")
    test2 <<= Def.task { (compile in Compile).value }
    lazy val test3 = settingKey[String]("Test 3.")
    test3 <<= name
    lazy val test4 = settingKey[String]("Test 4.")
    test4 <<= Def.setting { name.value }
    lazy val test5 = taskKey[sbt.inc.Analysis]("Test 5.")
    test5 <<= Def.task {
      println("Executing task.")
      (compile in Compile).value
    }
    lazy val test6 = settingKey[String]("Test 6.")
    test6 <<= Def.setting {
      println("Executing setting.")
      name.value
    }
  }

  object `special<<=` {
    import Keys.{run, testOnly, runMain, testQuick}
    val key = inputKey[Unit]("Seed.")
    key := {}
    run <<= key
    run <<= (key in ThisProject)
    run <<= key in ThisProject
    run <<= Def.inputTask { key.value }
    run <<= Def.inputTask { (key).value }
    run <<= Def.inputTask {
      println("Executing task.")
      key.value
    }

    val key2 = inputKey[Unit]("Seed 2.")
    key2 := {}
    runMain <<= key2
    runMain <<= (key2 in ThisProject)
    runMain <<= key2 in ThisProject
    runMain <<= Def.inputTask { key2.value }
    runMain <<= Def.inputTask { (key2).value }
    runMain <<= Def.inputTask {
      println("Executing task.")
      key2.value
    }

    val key3 = inputKey[Unit]("Seed 3.")
    key3 := {}
    testOnly <<= key3
    testOnly <<= (key3 in ThisProject)
    testOnly <<= key3 in ThisProject
    testOnly <<= Def.inputTask { key3.value }
    testOnly <<= Def.inputTask { (key3).value }
    testOnly <<= Def.inputTask {
      println("Executing task.")
      key3.value
    }

    val key4 = inputKey[Unit]("Seed 4.")
    key4 := {}
    testQuick <<= key4
    testQuick <<= (key4 in ThisProject)
    testQuick <<= key4 in ThisProject
    testQuick <<= Def.inputTask { key4.value }
    testQuick <<= Def.inputTask { (key4).value }
    testQuick <<= Def.inputTask {
      println("Executing task.")
      key4.value
    }
  }

  object mapAndApply {
    val sett1 = settingKey[String]("SettingKey 1")
    val sett2 = settingKey[String]("SettingKey 2")
    val sett3 = settingKey[String]("SettingKey 3")

    val task1 = taskKey[String]("TaskKey 1")
    val task2 = taskKey[String]("TaskKey 2")
    val task3 = taskKey[String]("TaskKey 3")
    val task4 = taskKey[String]("TaskKey 4")

    sett1 := "s1"
    sett2 := "s2"
    sett3 <<= (sett1, sett2)(_ + _)

    task1 := { println("t1"); "t1" }
    task2 := { println("t2"); "t2" }
    task3 <<= (task1, task2) map { (t1, t2) => println(t1 + t2); t1 + t2 }
    task4 <<= (sett1, sett2) map { (s1, s2) => println(s1 + s2); s1 + s2 }
  }
}

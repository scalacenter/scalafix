package scalafix.tests.cli

import java.io.File
import java.nio.file.Files

import scala.meta.io.AbsolutePath
import scala.meta.io.Classpath

import org.scalatest.funsuite.AnyFunSuite
import scalafix.internal.reflect.ClasspathOps

class AutoClasspathSuite extends AnyFunSuite {
  test("--classpath=auto") {
    val tmp = File.createTempFile("foo", "bar")
    assert(tmp.delete())
    val target = tmp.toPath.resolve("target")
    val target2 = tmp.toPath.resolve("bar")
    val semanticdb = target.resolve("META-INF").resolve("semanticdb")
    val semanticdb2 = target2.resolve("META-INF").resolve("semanticdb")
    val fakesemanticdb = tmp.toPath.resolve("blah").resolve("META-INF")
    assert(semanticdb.toFile.mkdirs())
    assert(semanticdb2.toFile.mkdirs())
    assert(fakesemanticdb.toFile.mkdirs())
    val obtained = ClasspathOps.autoClasspath(List(AbsolutePath(tmp)))
    val expected = Classpath(List(target, target2).map(AbsolutePath.apply))
    assert(obtained.entries.toSet == expected.entries.toSet)
  }

  test("--classpath=auto excludes .bak directories") {
    val tmp = File.createTempFile("foo", "bar")
    assert(tmp.delete())
    val target = tmp.toPath.resolve("target")
    val targetBak = tmp.toPath.resolve("target.bak")
    val semanticdb = target.resolve("META-INF").resolve("semanticdb")
    val semanticdbBak = targetBak.resolve("META-INF").resolve("semanticdb")
    assert(semanticdb.toFile.mkdirs())
    // Create a .bak directory that looks like a valid semanticdb root
    assert(semanticdbBak.toFile.mkdirs())
    val obtained = ClasspathOps.autoClasspath(List(AbsolutePath(tmp)))
    // Only the non-.bak directory should be included
    val expected = Classpath(List(target).map(AbsolutePath.apply))
    assert(obtained.entries.toSet == expected.entries.toSet)
  }

  test("--classpath=auto ignores missing roots before traversal starts") {
    val tmp = Files.createTempDirectory("scalafix-auto-classpath")
    Files.delete(tmp)
    val obtained = ClasspathOps.autoClasspath(List(AbsolutePath(tmp)))
    assert(obtained.entries.isEmpty)
  }
}

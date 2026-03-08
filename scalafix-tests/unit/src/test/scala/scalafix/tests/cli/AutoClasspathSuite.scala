package scalafix.tests.cli

import java.io.File
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.atomic.AtomicReference

import scala.meta.io.AbsolutePath
import scala.meta.io.Classpath

import org.scalatest.funsuite.AnyFunSuite
import scalafix.internal.reflect.ClasspathOps

class AutoClasspathSuite extends AnyFunSuite {
  private def deleteRecursively(path: Path): Unit = {
    if (Files.exists(path)) {
      Files.walkFileTree(
        path,
        new SimpleFileVisitor[Path] {
          override def visitFile(
              file: Path,
              attrs: BasicFileAttributes
          ): FileVisitResult = {
            Files.deleteIfExists(file)
            FileVisitResult.CONTINUE
          }

          override def postVisitDirectory(
              dir: Path,
              exc: IOException
          ): FileVisitResult = {
            Files.deleteIfExists(dir)
            FileVisitResult.CONTINUE
          }
        }
      )
    }
  }

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

  test("--classpath=auto ignores roots deleted during traversal") {
    val tmp = Files.createTempDirectory("scalafix-auto-classpath")
    val stable = tmp.resolve("stable")
    Files.createDirectories(stable.resolve("META-INF").resolve("semanticdb"))
    val transientRoots = (0 until 50).map { i =>
      val path = tmp.resolve(s"deleted-$i")
      Files.createDirectories(path.resolve("META-INF").resolve("semanticdb"))
      path
    }
    val deleterFailure = new AtomicReference[Throwable]()
    val deleter = new Thread(() => {
      try transientRoots.foreach(deleteRecursively)
      catch {
        case e: Throwable => deleterFailure.set(e)
      }
    })
    deleter.start()
    try {
      val obtained = ClasspathOps.autoClasspath(List(AbsolutePath(tmp)))
      assert(obtained.entries.contains(AbsolutePath(stable)))
    } finally {
      deleter.join()
      Option(deleterFailure.get()).foreach(e => throw e)
      deleteRecursively(tmp)
    }
  }
}

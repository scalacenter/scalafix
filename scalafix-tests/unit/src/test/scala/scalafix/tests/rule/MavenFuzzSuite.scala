package scalafix.tests.rule

import coursier._
import scala.collection.JavaConverters._
import org.scalatest.FunSuite
import scalafix.testkit.DiffAssertions
import scalafix.interfaces.Scalafix
import java.nio.file.Files
import scala.meta.internal.io.FileIO
import scala.meta.io.AbsolutePath
import java.nio.file.Path
import scala.sys.process._
import scala.collection.mutable
import scala.tools.nsc.reporters.StoreReporter
import scala.meta.internal.pc.ScalafixGlobal
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystems
import java.nio.file.Paths

import org.scalatest.Ignore

@Ignore // Ignored because this test is very slow.
class MavenFuzzSuite extends FunSuite with DiffAssertions {
  private def getCompilingSources(
      g: ScalafixGlobal,
      classfiles: Seq[Path],
      sourceJars: Seq[Path],
      tmp: Path
  ): Seq[Path] = {
    val result = Vector.newBuilder[Path]
    val matcher =
      FileSystems.getDefault().getPathMatcher("glob:*.scala")
    sourceJars.foreach { jar =>
      FileIO.withJarFileSystem(AbsolutePath(jar), false, true) { root =>
        FileIO.listAllFilesRecursively(root).files.foreach { relpath =>
          val in = root.resolve(relpath)
          val filename = relpath.toNIO.getFileName().toString()
          if (matcher.matches(Paths.get(filename))) {
            val text = FileIO.slurp(in, StandardCharsets.UTF_8)
            val errors = compileErrors(g, text, relpath.toString())
            if (errors.isEmpty) {
              val out = tmp.resolve(relpath.toString())
              Files.createDirectories(out.getParent())
              val stream = Files.newOutputStream(out)
              try Files.copy(in.toNIO, stream)
              finally stream.close()
              result += out
            } else {
              pprint.log(errors)
            }
          }
        }
      }
    }
    result.result()
  }

  def compileErrors(
      g: ScalafixGlobal,
      code: String,
      filename: String
  ): List[String] = {
    val reporter = new StoreReporter()
    val old = g.reporter
    g.reporter = reporter
    g.settings.stopAfter.value = List("typer")
    val run = new g.Run()
    val source = g.newSourceFile(code, filename)
    run.compileSources(List(source))
    g.reporter = old
    val errors = reporter.infos.filter(_.severity.id == 2)
    errors.toList.map { d =>
      new StringBuilder()
        .append(d.pos.source.path)
        .append(":")
        .append(d.pos.line)
        .append(" ")
        .append(d.msg)
        .append("\n")
        .append(d.pos.lineContent)
        .append("\n")
        .append(d.pos.lineCaret)
        .toString()
    }
  }

  val metals = List(
    Dependency(
      Module(
        Organization("org.scalameta"),
        ModuleName("metals_2.12")
      ),
      "0.7.6"
    )
  )
  // akka is a bad example since it has undeclared compile-time dependencies
  // on "silencer"
  val akka = Dependency(
    Module(
      Organization("com.typesafe.akka"),
      ModuleName("akka-actor_2.12")
    ),
    "2.5.25"
  )
  val ammonite = List(
    Dependency(
      Module(
        Organization("com.lihaoyi"),
        ModuleName("ammonite-repl_2.12.10")
      ),
      "1.7.4-0-cdefbd9"
    ),
    Dependency(
      Module(
        Organization("com.lihaoyi"),
        ModuleName("acyclic_2.12")
      ),
      "0.2.0"
    )
  )
  val dependencies = ammonite // ammonite
  val fetch = Fetch()

  def check(rule: String): Unit = {
    test(rule) {
      val classfiles = fetch
        .withDependencies(dependencies)
        .run()
        .map(_.toPath())
      val sources = fetch
        .withClassifiers(Set(Classifier("sources")))
        .withDependencies(dependencies)
        .run()
        .map(_.toPath())
        .filter(_.toString().contains("lihaoyi"))
      val scalafix =
        Scalafix.classloadInstance(this.getClass().getClassLoader())
      val tmp = Files.createTempDirectory("scalafix")
      def exec(cmd: String*): Unit = {
        val gitinit = Process(cmd.toList, Some(tmp.toFile())).!
        require(gitinit == 0, gitinit)
      }

      tmp.toFile().deleteOnExit()
      val g = ScalafixGlobal.newCompiler(
        classfiles.map(AbsolutePath(_)).toList,
        Nil,
        Map.empty
      )
      val paths = getCompilingSources(g, classfiles, sources, tmp)
      exec("git", "init")
      exec("git", "add", ".")
      exec("git", "commit", "-m", "first-commit")
      pprint.log(paths.length)
      val args = scalafix
        .newArguments()
        .withSourceroot(tmp)
        .withPaths(paths.asJava)
        .withRules(List(rule).asJava)
        .withClasspath(classfiles.asJava)
      // .withMode(ScalafixMainMode.CHECK)
      val exit = args.run()
      pprint.log(exit)
      // exec("git", "diff")
      FileIO.listAllFilesRecursively(AbsolutePath(tmp)).foreach { path =>
        if (path.toNIO.getFileName.toString.endsWith(".scala")) {
          val text = FileIO.slurp(path, StandardCharsets.UTF_8)
          val errors = compileErrors(g, text, path.toString())
          if (errors.nonEmpty) {
            pprint.log(path)
            pprint.log(errors)
          }
        }
      }
      pprint.log(tmp)
    }
  }
  check("ExplicitResultTypes")
}

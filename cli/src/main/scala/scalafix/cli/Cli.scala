package scalafix.cli

import scala.collection.GenSeq
import scalafix.FixResult
import scalafix.Scalafix
import scalafix.rewrite.ProcedureSyntax
import scalafix.rewrite.Rewrite
import scalafix.util.FileOps
import scalafix.util.LoggerOps
import scalafix.util.LoggerOps._

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.PrintStream
import java.util.concurrent.atomic.AtomicInteger

import com.martiansoftware.nailgun.NGContext

object Cli {
  case class Config(
      workingDirectory: File = new File(System.getProperty("user.dir")),
      out: PrintStream = System.out,
      in: InputStream = System.in,
      err: PrintStream = System.err,
      files: Set[File] = Set.empty[File],
      rewrites: Seq[Rewrite] = Rewrite.default,
      parallel: Boolean = true,
      inPlace: Boolean = false,
      debug: Boolean = false
  )

  def nameMap[T](t: sourcecode.Text[T]*): Map[String, T] = {
    t.map(x => x.source -> x.value).toMap
  }

  val rewriteMap: Map[String, Rewrite] = nameMap(
      ProcedureSyntax
  )
  val default = Config()

  implicit val weekDaysRead: scopt.Read[Rewrite] = scopt.Read.reads(rewriteMap)

  val parser = new scopt.OptionParser[Config]("scalafix") {
    head("scalafix", scalafix.Versions.nightly)

    opt[Seq[File]]('f', "files")
      .text("files to fix, can be directory or file path")
      .minOccurs(1)
      .maxOccurs(10000)
      .action((files, c) => c.copy(files = c.files ++ files))

    opt[Seq[Rewrite]]("rewrites")
      .maxOccurs(10000)
      .action((rewrites, c) => c.copy(rewrites = c.rewrites ++ rewrites))
      .text(
          s"rewrite rules to run. Available: ${rewriteMap.keys.mkString(", ")} ")

    opt[Boolean]("parallel")
      .text("if true, runs in parallel. If false, run on single thread.")
      .maxOccurs(1)
      .action((b, c) => c.copy(parallel = b))

    opt[Unit]('i', "in-place")
      .text("write fixes to file instead of printing to stdout")
      .maxOccurs(1)
      .action((_, c) => c.copy(inPlace = true))

    help("help").text("prints this usage text")

    note("""
           |Example usage:
           |  // Write fixes to file in place.
           |  scalafix -i -f fixme.scala
           |  // Write fixes to all *.scala files in directory src/main/scala
           |  scalafix -i -f src/main/scala
           |""".stripMargin)
  }

  def handleFile(file: File, config: Config): Unit = {
    Scalafix.fix(FileOps.readFile(file), config.rewrites) match {
      case FixResult.Success(code) =>
        if (config.inPlace) {
          FileOps.writeFile(file, code)
        } else config.out.write(code.getBytes)
      case FixResult.Failure(e) =>
        config.err.write(s"Failed to fix $file. Cause: $e".getBytes)
      case e: FixResult.ParseError =>
        if (config.files.contains(file)) {
          // Only log if user explicitly specified that file.
          config.err.write(e.toString.getBytes())
        }
    }
  }

  def runOn(config: Config): Unit = {
    config.files.foreach { path =>
      val realPath: File =
        if (path.isAbsolute) path
        else new File(config.workingDirectory, path.getPath)
      if (realPath.isDirectory) {
        val filesToFix: GenSeq[String] = {
          val files =
            FileOps.listFiles(realPath).filter(x => x.endsWith(".scala"))
          if (config.parallel) files.par
          else files
        }
        val logger = new TermDisplay(new OutputStreamWriter(System.out))
        logger.init()
        val msg = "Running scalafix..."
        logger.downloadingArtifact(msg, config.workingDirectory)
        logger.downloadLength(msg, filesToFix.length, 0)
        val counter = new AtomicInteger()
        filesToFix.foreach { x =>
          handleFile(new File(x), config)
          val progress = counter.incrementAndGet()
          logger.downloadProgress(msg, progress)
        }
        logger.stop()
      } else {
        handleFile(realPath, config)
      }
    }
  }

  def runMain(args: Seq[String], init: Config): Unit = {
    parser.parse(args, init) match {
      case Some(config) => runOn(config)
      case None => System.exit(1)
    }
  }

  def nailMain(nGContext: NGContext): Unit = {
    runMain(
        nGContext.getArgs,
        Config(
            workingDirectory = new File(nGContext.getWorkingDirectory),
            out = nGContext.out,
            in = nGContext.in,
            err = nGContext.err
        )
    )
  }

  def main(args: Array[String]): Unit = {
    runMain(args, Config())
  }
}

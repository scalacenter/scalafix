package scalafix.cli

import scalafix.FixResult
import scalafix.Scalafix
import scalafix.rewrite.ProcedureSyntax
import scalafix.rewrite.Rewrite
import scalafix.util.FileOps
import scalafix.util.LoggerOps._

import java.io.File
import java.io.OutputStream

object Cli {
  case class Config(
      files: Set[File] = Set.empty[File],
      rewrites: Seq[Rewrite] = Rewrite.default,
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

    opt[Rewrite]("rewrites").text("which rewrites ")

    opt[Unit]('i', "in-place")
      .text("write fixes to file instead of printing to stdout")
      .maxOccurs(1)
      .action((_, c) => c.copy(inPlace = true))

    help("help").text("prints this usage text")

    note("""
           |Example usages:
           |
           |  // Overwrite file with fixed contents.
           |  scalafix -f fixme.scala -i
           |""".stripMargin)
  }

  def handleFile(file: File, config: Config): Unit = {
    Scalafix.fix(FileOps.readFile(file), config.rewrites) match {
      case FixResult.Success(code) =>
        if (config.inPlace) {
          FileOps.writeFile(file, code)
        } else println(code)
      case FixResult.Error(e) => throw e
    }
  }

  def runOn(config: Config): Unit = {
    config.files.foreach { path =>
      if (path.isDirectory) {
        FileOps
          .listFiles(path)
          .withFilter(x => x.endsWith(".scala"))
          .foreach(x => handleFile(new File(x), config))
      } else {
        handleFile(path, config)
      }
    }
  }

  def main(args: Array[String]): Unit = {
    parser.parse(args, Config()) match {
      case Some(config) => runOn(config)
      case None => System.exit(1)
    }
  }
}

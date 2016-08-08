package scalafix.cli

import scalafix.FixResult
import scalafix.Scalafix
import scalafix.rewrite.ProcedureSyntax
import scalafix.rewrite.Rewrite
import scalafix.util.LoggerOps._

import java.io.{File => JFile}

import better.files.File.OpenOptions
import better.files._

object Cli {
  case class Config(
      files: Set[JFile] = Set.empty[JFile],
      rewrites: Seq[Rewrite] = Rewrite.default,
      inPlace: Boolean = false,
      debug: Boolean = false
  ) {
    def paths: Set[String] = files.map(_.getAbsolutePath)
  }

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

    opt[Seq[JFile]]('f', "files")
      .text("files to fix, can be directory or file path")
      .minOccurs(1)
      .maxOccurs(10000)
      .action((files, c) => c.copy(files = c.files ++ files))

    opt[Rewrite]("rewrites").text("which rewrites ")

    opt[Unit]('i', "in-place")
      .text("write fixes to file instead of printing to stdout")
      .maxOccurs(1)
      .action((_, c) => c.copy(inPlace = true))

  }

  def handleFile(file: File, config: Config): Unit = {
    Scalafix.fix(file.contentAsString, config.rewrites) match {
      case FixResult.Success(code) =>
        if (config.inPlace) {
          file.write(code.getBytes())(OpenOptions.default)
        } else println(code)
      case FixResult.Error(e) => throw e
    }
  }

  def runOn(config: Config): Unit = {
    config.files.foreach { path =>
      if (path.isDirectory) {
        path.toScala.listRecursively
          .withFilter(x => x.extension.contains("scala"))
          .foreach(x => handleFile(x, config))
      } else {
        handleFile(path.toScala, config)
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

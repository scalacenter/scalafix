package scalafix.internal.rule

import java.nio.file.Path
import java.io.InputStream

import org.langmeta.inputs.Input

import scala.collection.mutable.StringBuilder

import scala.io.Source

import scalafix.diff.{GitDiff, NewFile, ModifiedFile, GitChange}
import scalafix.internal.diff.GitDiffParser
import scalafix.internal.util.IntervalSet
import scalafix.LintMessage

object DiffDisable {
  def apply(input: InputStream, workingDir: Path): DiffDisable = {
    val itt = Source.fromInputStream(input).getLines
    new DiffDisable(new GitDiffParser(itt, workingDir).parse())
  }
}

class DiffDisable(diffs: List[GitDiff]) {
  private val newFiles: Set[Input] = diffs.collect {
    case NewFile(path) => Input.File(path)
  }.toSet

  private val modifiedFiles: Map[Input, IntervalSet] = diffs.collect {
    case ModifiedFile(path, changes) => {
      val ranges = changes.map {
        case GitChange(start, length) => (start, start + length)
      }
      Input.File(path) -> IntervalSet(ranges)
    }
  }.toMap

  def filter(lints: List[LintMessage]): List[LintMessage] = {
    def isAddition(lint: LintMessage): Boolean =
      newFiles.contains(lint.position.input)

    def isModification(lint: LintMessage): Boolean =
      modifiedFiles
        .get(lint.position.input)
        .fold(false)(
          interval =>
            interval.intersects(
              lint.position.startLine + 1,
              lint.position.endLine + 1
          ))

    lints.filter(lint => isAddition(lint) || isModification(lint))
  }

  override def toString: String = {

    val b = new StringBuilder()
    def add(in: String): Unit =
      b ++= in + "\n"

    add("== New Files ==")
    diffs.foreach {
      case NewFile(path) => add(path.toString)
      case _ => ()
    }

    add("== Modified Files ==")
    diffs.foreach {
      case ModifiedFile(path, changes) => {
        add(path.toString)
        changes.foreach {
          case GitChange(start, offset) => add(s"  [$start, ${start + offset}]")
        }
      }
      case _ => ()
    }
    b.result()
  }
}

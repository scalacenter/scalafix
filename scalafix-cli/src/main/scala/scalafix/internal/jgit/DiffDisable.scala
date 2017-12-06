package scalafix.internal.jgit

import metaconfig.Configured

import java.nio.file.Path
import java.io.InputStream

import org.langmeta.inputs.Input

import scala.collection.mutable.StringBuilder

import scala.io.Source

import scalafix.internal.util.IntervalSet
import scalafix.LintMessage

object DiffDisable {
  def apply(workingDir: Path, baseBranch: String): Configured[DiffDisable] = {
    JGitDiff(workingDir, baseBranch).map(diffs => new DiffDisable(diffs))
  }
}

class DiffDisable(diffs: List[GitDiff]) {
  private val newFiles: Set[Input] = diffs.collect {
    case NewFile(path) => Input.File(path)
  }.toSet

  private val modifiedFiles: Map[Input, IntervalSet] = diffs.collect {
    case ModifiedFile(path, changes) => {
      val ranges = changes.map {
        case GitChange(start, end) => (start, end)
      }
      Input.File(path) -> IntervalSet(ranges)
    }
  }.toMap

  def isDisabled(file: Input): Boolean =
    !(newFiles.contains(file) || modifiedFiles.contains(file))

  def filter(lints: List[LintMessage]): List[LintMessage] = {
    def isAddition(lint: LintMessage): Boolean =
      newFiles.contains(lint.position.input)

    def isModification(lint: LintMessage): Boolean =
      modifiedFiles
        .get(lint.position.input)
        .fold(false)(
          interval =>
            interval.intersects(
              lint.position.startLine,
              lint.position.endLine
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
          case GitChange(start, end) => add(s"  [$start, $end]")
        }
      }
      case _ => ()
    }
    b.result()
  }
}

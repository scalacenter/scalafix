package scalafix.internal.rule

import org.langmeta.inputs.Input

import scalafix.diff.{GitDiff, NewFile, ModifiedFile, Range}
import scalafix.internal.util.IntervalSet
import scalafix.LintMessage

class DiffDisable(diffs: List[GitDiff]) {
  private val newFiles: Set[Input] = diffs.collect {
    case NewFile(path) => Input.File(path)
  }.toSet

  private val modifiedFiles: Map[Input, IntervalSet] = diffs.collect {
    case ModifiedFile(path, changes) => {
      val ranges = changes.map {
        case Range(start, length) => (start, start + length)
      }
      Input.File(path) -> IntervalSet(ranges)
    }
  }.toMap

  def filter(lints: List[LintMessage]): List[LintMessage] = {
    lints.filter(
      lint =>
        newFiles.contains(lint.position.input) ||
          modifiedFiles
            .get(lint.position.input)
            .fold(false)(interval =>
              interval.intersect(
                lint.position.startLine + 1,
                lint.position.endLine + 1)))
  }
}

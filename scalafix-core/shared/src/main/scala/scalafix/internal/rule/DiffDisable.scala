package scalafix.internal.rule

import scalafix.diff.{GitDiff, NewFile}

import scalafix.LintMessage

import org.langmeta.inputs.Input

class DiffDisable(diffs: List[GitDiff]) {
  val newFiles: Set[Input] = diffs.collect {
    case NewFile(path) => Input.File(path)
  }.toSet

  // todo RANGE modified
  // case class ModifiedFile(path: String, changes: List[Range]) extends GitDiff
  // case class Range(start: Int, length: Int)

  def filter(lints: List[LintMessage]): List[LintMessage] = {
    lints.filter(lint => newFiles.contains(lint.position.input))
  }
}

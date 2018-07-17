package scalafix.internal.diff

import java.nio.file.Path

case class GitChange(start: Int, length: Int)

sealed trait GitDiff
case class NewFile(path: Path) extends GitDiff
case class ModifiedFile(path: Path, changes: List[GitChange]) extends GitDiff

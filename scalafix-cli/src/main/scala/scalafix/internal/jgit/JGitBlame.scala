package scalafix.internal.jgit

import scalafix.lint.{RuleDiagnostic, Diagnostic}

import scala.meta.Input

import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.blame.{BlameGenerator, BlameResult}
import org.eclipse.jgit.lib.Constants

import java.util.Date
import java.text.SimpleDateFormat
import java.nio.file.{Path, Paths}

import org.eclipse.jgit.revwalk.RevCommit

class JGitBlame(workingDir: Path, diffBase: Option[String]) {
  private val builder = new FileRepositoryBuilder()
  private val repository =
    builder.readEnvironment().setWorkTree(workingDir.toFile).build()
  private val dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm")

  def apply(diagnostics: List[RuleDiagnostic]): List[RuleDiagnostic] = {

    val diagnosticsByFilePath: List[(Path, List[RuleDiagnostic])] =
      diagnostics.groupBy { diagnostic =>
        val path =
          diagnostic.position.input match {
            case Input.VirtualFile(path, _) =>
              Paths.get(path)
            case Input.File(path, _) =>
              path.toNIO
            case input =>
              throw new Exception("cannot extract path from input: " + input)
          }

        workingDir.relativize(path)
      }.toList

    diagnosticsByFilePath.flatMap {
      case (filePath, diagnosticsForFilePath) =>
        blame(filePath, diagnosticsForFilePath)
    }
  }

  def formatCommit(commit: RevCommit): String = {
    val commitMsg = commit.getShortMessage()
    val shortSha = commit.abbreviate(8).name()
    val author = commit.getAuthorIdent()
    val authorName = author.getName
    val date = dateFormat.format(author.getWhen)
    s"$date $authorName $shortSha - $commitMsg"
  }

  private def blame(
      filePath: Path,
      diagnostics: List[RuleDiagnostic]): List[RuleDiagnostic] = {
    val blameGenerator = new BlameGenerator(repository, filePath.toString)
    val startRev = diffBase.getOrElse(Constants.HEAD)
    blameGenerator.push("", repository.resolve(startRev))
    val blameResult = BlameResult.create(blameGenerator)
    diagnostics.foreach(
      diagnostic =>
        blameResult.computeRange(
          diagnostic.position.startLine,
          diagnostic.position.startLine + 1))
    val lintsWithBlame =
      diagnostics.map { diagnostic =>
        val line = diagnostic.position.startLine
        val commit = blameResult.getSourceCommit(line)
        val blameLine = formatCommit(commit)

        val diag = diagnostic.diagnostic
        RuleDiagnostic(
          Diagnostic(
            diag.categoryID,
            "\n" + blameLine + "\n" + diag.message,
            diag.position,
            diag.explanation,
            diag.severity
          ),
          diagnostic.rule,
          diagnostic.overriddenSeverity
        )
      }

    lintsWithBlame
  }
}

package scalafix.internal.jgit

import scalafix.lint.RuleDiagnostic

import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.blame.{BlameGenerator, BlameResult}
import org.eclipse.jgit.lib.Constants

import java.text.SimpleDateFormat
import java.nio.file.Path
import scala.meta.io.RelativePath

import org.eclipse.jgit.revwalk.RevCommit

class JGitBlame(
    workingDirectory: Path,
    filePath: RelativePath,
    diffBase: Option[String]) {

  private val workTree = workingDirectory.toFile
  private val builder = new FileRepositoryBuilder()
  private val repository =
    builder.readEnvironment().setWorkTree(workTree).build()
  private val dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm")

  def formatCommit(commit: RevCommit): String = {
    val commitMsg = commit.getShortMessage()
    val shortSha = commit.abbreviate(8).name()
    val author = commit.getAuthorIdent()
    val authorName = author.getName
    val date = dateFormat.format(author.getWhen)
    s"\nIntroduced by $authorName on $date in commit $shortSha - $commitMsg"
  }

  def apply(diagnostics: List[RuleDiagnostic]): List[RuleDiagnostic] = {
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
        diagnostic.withBlame(formatCommit(commit))
      }

    lintsWithBlame
  }
}

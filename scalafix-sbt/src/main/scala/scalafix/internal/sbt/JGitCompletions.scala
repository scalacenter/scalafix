package scalafix.internal.sbt

import java.nio.file.Path
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.RefDatabase
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.util.GitDateFormatter
import scala.collection.JavaConverters._

class JGitCompletion(cwd: Path) {
  private val builder = new FileRepositoryBuilder()
  private val repo = builder.readEnvironment().setWorkTree(cwd.toFile).build()
  private val refList = repo.getRefDatabase().getRefs(RefDatabase.ALL).asScala
  private val git = new Git(repo)
  private val refs = git.log().setMaxCount(20).call().asScala.toList
  private val dateFormatter = new GitDateFormatter(
    GitDateFormatter.Format.RELATIVE)

  val branchesAndTags: List[String] =
    refList.map { case (a, b) => Repository.shortenRefName(a) }.toList

  val last20Commits: List[(String, String)] =
    refs.map { ref =>
      val relativeCommitTime =
        dateFormatter.formatDate(refs.head.getCommitterIdent)
      val abrev = ref.abbreviate(8).name
      val short = ref.getShortMessage
      (s"$abrev -- $short ($relativeCommitTime)", abrev)
    }
}

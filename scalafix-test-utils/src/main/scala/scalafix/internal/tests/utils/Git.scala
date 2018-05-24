package scalafix.internal.tests.utils

import java.nio.file.Path

class Git(workingDirectory: Path) {
  import org.eclipse.jgit.api.{Git => JGit}

  private val git = JGit.init().setDirectory(workingDirectory.toFile).call()
  private var revision = 0

  def add(filename: String): Unit =
    git.add().addFilepattern(filename).call()

  def rm(filename: String): Unit =
    git.rm().addFilepattern(filename).call()

  def checkout(branch: String): Unit =
    git.checkout().setCreateBranch(true).setName(branch).call()

  def tag(name: String): Unit =
    git.tag().setName(name).call()

  def deleteBranch(branch: String): Unit =
    git.branchDelete().setBranchNames(branch).call()

  def commit(): Unit = {
    git.commit().setMessage(s"r$revision").call()
    revision += 1
  }
}

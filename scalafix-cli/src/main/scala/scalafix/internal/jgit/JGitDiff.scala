package scalafix.internal.jgit

import java.nio.file.Path

import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.patch.FileHeader
import org.eclipse.jgit.lib.ObjectReader
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.ProgressMonitor
import org.eclipse.jgit.dircache.DirCacheIterator
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevTree
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.treewalk.AbstractTreeIterator
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.eclipse.jgit.treewalk.FileTreeIterator
import org.eclipse.jgit.util.io.NullOutputStream
import scala.collection.JavaConverters._

object JGitDiff {
  def apply(workingDir: Path, baseBranch: String): List[GitDiff] = {
    val builder = new FileRepositoryBuilder()
    val repository =
      builder.readEnvironment().setWorkTree(workingDir.toFile).build()

    val oldTree1 = ref(repository, s"refs/heads/$baseBranch")
    val newTree1 = new FileTreeIterator(repository)

    val diffs = List.newBuilder[GitDiff]

    def path(relative: String): Path =
      workingDir.resolve(relative)

    def edits(file: FileHeader): Unit = {
      val changes = List.newBuilder[GitChange]
      file.toEditList.asScala.foreach { edit =>
        changes += GitChange(edit.getBeginB, edit.getEndB)
      }
      diffs += ModifiedFile(path(file.getNewPath), changes.result())
    }

    getDiff(repository, oldTree1, newTree1).foreach { file =>
      file.getChangeType match {
        case DiffEntry.ChangeType.ADD => diffs += NewFile(path(file.getNewPath))
        case DiffEntry.ChangeType.MODIFY => edits(file)
        case DiffEntry.ChangeType.RENAME => edits(file)
        case DiffEntry.ChangeType.COPY => edits(file)
        case DiffEntry.ChangeType.DELETE => ()
      }
    }

    diffs.result()
  }

  private def ref(repository: Repository, ref: String): AbstractTreeIterator =
    iterator(repository, _.parseCommit(repository.exactRef(ref).getObjectId()))

  // could be handy in future
  private def commit(
      repository: Repository,
      objectId: String): AbstractTreeIterator =
    iterator(repository, _.parseCommit(ObjectId.fromString(objectId)))

  private def iterator(
      repository: Repository,
      idFrom: RevWalk => RevCommit): AbstractTreeIterator = {
    val walk = new RevWalk(repository)
    val commit = idFrom(walk)
    val tree = walk.parseTree(commit.getTree().getId())
    val treeParser = new CanonicalTreeParser()
    val reader = repository.newObjectReader()
    treeParser.reset(reader, tree.getId())
    walk.dispose()
    treeParser
  }

  private def getDiff(
      repository: Repository,
      oldTree: AbstractTreeIterator,
      newTree: AbstractTreeIterator): List[FileHeader] = {

    val diffFmt = new DiffFormatter(NullOutputStream.INSTANCE)
    diffFmt.setRepository(repository)
    diffFmt.setContext(0)
    diffFmt.setDetectRenames(true)

    val diffs = diffFmt.scan(oldTree, newTree)
    diffFmt.format(diffs)
    diffFmt.flush()
    diffs.asScala.map(diff => diffFmt.toFileHeader(diff)).toList
  }
}

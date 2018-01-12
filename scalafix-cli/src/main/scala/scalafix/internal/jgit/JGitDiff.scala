package scalafix.internal.jgit

import java.nio.file.Path

import scalafix.internal.diff._

import org.eclipse.jgit.util.FS
import org.eclipse.jgit.lib.RepositoryCache
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.diff.DiffEntry.ChangeType._
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.errors.AmbiguousObjectException
import org.eclipse.jgit.errors.IncorrectObjectTypeException
import org.eclipse.jgit.errors.RevisionSyntaxException
import org.eclipse.jgit.errors.MissingObjectException
import org.eclipse.jgit.patch.FileHeader
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.treewalk.AbstractTreeIterator
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.eclipse.jgit.treewalk.FileTreeIterator
import org.eclipse.jgit.util.io.NullOutputStream
import org.eclipse.jgit.lib.Constants.DOT_GIT

import scala.collection.JavaConverters._

import metaconfig.{ConfError, Configured}

object JGitDiff {
  def apply(workingDir: Path, diffBase: String): Configured[DiffDisable] = {

    if (isGitRepository(workingDir)) {
      val builder = new FileRepositoryBuilder()
      val repository =
        builder.readEnvironment().setWorkTree(workingDir.toFile).build()

      resolve(repository, diffBase) match {
        case Right(id) => {
          iterator(repository, id) match {
            case Right(oldTree) => {
              val newTree = new FileTreeIterator(repository)
              def path(relative: String): Path = workingDir.resolve(relative)

              def edits(file: FileHeader): ModifiedFile = {
                val changes =
                  file.toEditList.asScala.map(edit =>
                    GitChange(edit.getBeginB, edit.getEndB))

                ModifiedFile(path(file.getNewPath), changes.toList)
              }
              val diffs =
                getDiff(repository, oldTree, newTree).flatMap(file =>
                  file.getChangeType match {
                    case ADD => List(NewFile(path(file.getNewPath)))
                    case MODIFY => List(edits(file))
                    case RENAME => List(edits(file))
                    case COPY => List(edits(file))
                    case DELETE => Nil
                })

              Configured.Ok(DiffDisable(diffs))
            }
            case Left(msg) => ConfError.msg(msg).notOk
          }
        }
        case Left(msg) => ConfError.msg(msg).notOk
      }
    } else {
      ConfError.msg(s"$workingDir is not a git repository").notOk
    }
  }

  private def unknown[T](id: String): Left[String, T] =
    Left(s"'$id' unknown revision or path not in the working tree.")

  private def resolve(
      repo: Repository,
      revstr: String): Either[String, ObjectId] = {
    try {
      Option(repo.resolve(revstr)) match {
        case Some(id) => Right(id)
        case None => unknown(revstr)
      }
    } catch {
      case ambiguous: AmbiguousObjectException => {
        val out =
          s"$revstr is ambiguous. Possible candidates: " ::
            ambiguous.getCandidates.asScala.toList

        Left(out.mkString(System.lineSeparator))
      }
      case ot: IncorrectObjectTypeException => Left(ot.getMessage)
      case st: RevisionSyntaxException => Left(st.getMessage)
    }
  }

  private def isGitRepository(workingDir: Path): Boolean =
    RepositoryCache.FileKey
      .isGitRepository(workingDir.resolve(DOT_GIT).toFile, FS.DETECTED)

  private def iterator(
      repository: Repository,
      id: ObjectId): Either[String, AbstractTreeIterator] = {
    try {
      val walk = new RevWalk(repository)
      val tree = walk.parseTree(id)
      val treeParser = new CanonicalTreeParser()
      val reader = repository.newObjectReader()
      treeParser.reset(reader, tree.getId())
      walk.dispose()
      Right(treeParser)
    } catch {
      case missing: MissingObjectException =>
        unknown(id.getName)
    }
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

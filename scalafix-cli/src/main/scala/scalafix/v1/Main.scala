package scalafix.v1

import java.nio.file.FileSystems
import java.nio.file.PathMatcher
import java.nio.file.Paths
import org.langmeta.io.AbsolutePath
import metaconfig._
import org.langmeta.internal.io.PathIO

sealed abstract class Ls
object Ls {
  case object Find extends Ls
  // TODO: git ls-files
  implicit val codec = Meta
}

case class Args(
    ls: Ls,
    cwd: AbsolutePath = PathIO.workingDirectory,
    files: List[PathMatcher] = Nil
) {
  def matches(path: AbsolutePath): Boolean =
    Args.baseMatcher.matches(path.toNIO) &&
      files.forall(_.matches(path.toNIO))
}

object Args {
  val baseMatcher = FileSystems.getDefault.getPathMatcher("glob:**.{scala,sbt}")
  implicit val surface = generic.deriveSurface[Args]
  implicit val decoder = generic.deriveDecoder(Args)
}

object Main {
  def main(args: Array[String]): Unit = {
    val pm = FileSystems.getDefault.getPathMatcher("glob:**.{scala,java}")
    val p = Paths.get("").toAbsolutePath.resolve("a.java")
    pprint.log(pm.matches(p))
  }
}

package scalafix.internal.v1
import java.nio.charset.StandardCharsets
import scala.compat.Platform
import scala.meta.internal.io.FileIO
import scala.meta.io.AbsolutePath

object ArgExpansion {

  def expand(args: Array[String], cwd: AbsolutePath): List[String] = {
    args.toList.flatMap { arg =>
      if (arg.startsWith("@")) {
        val argPath = AbsolutePath(arg.substring(1))(cwd)
        val argText = FileIO.slurp(argPath, StandardCharsets.UTF_8)
        argText.split(Platform.EOL).map(_.trim).filter(_.nonEmpty).toList
      } else {
        List(arg)
      }
    }
  }

}

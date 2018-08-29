package scalafix.tests.util
import java.nio.charset.StandardCharsets
import java.nio.file.Files

object SaveExpect {
  def main(args: Array[String]): Unit = {
    val all = List[ExpectSuite](
      new PrettyExpectSuite
    )
    all.foreach { suite =>
      Files.write(
        suite.path.toNIO,
        suite.obtained.getBytes(StandardCharsets.UTF_8)
      )
      println(suite.path)
    }
  }
}

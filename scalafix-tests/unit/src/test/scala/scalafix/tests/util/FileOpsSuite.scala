package scalafix.tests.util

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.URI
import java.net.URL
import java.net.URLConnection
import java.nio.charset.StandardCharsets

import org.scalatest.funsuite.AnyFunSuite
import scalafix.internal.util.FileOps

class FileOpsSuite extends AnyFunSuite {
  private val GitHubTokenProperty = "scalafix.github.token"
  private val RuleSource = "package fix"

  test("readURL sends GitHub token when reading raw GitHub rule source") {
    withSystemProperty(GitHubTokenProperty, "secret-token") {
      val url = URI
        .create(
          "https://raw.githubusercontent.com/org/repo/main/" +
            "scalafix/rules/src/main/scala/fix/Rule.scala"
        )
        .toURL

      assert(
        FileOps.readURL(
          url,
          authenticatedConnection(
            expectedUrl =
              "https://api.github.com/repos/org/repo/contents/scalafix/rules/src/main/scala/fix/Rule.scala?ref=main",
            expectedAuthorization = Some("Bearer secret-token"),
            expectedAccept = Some("application/vnd.github.raw+json")
          )
        ) == RuleSource
      )
    }
  }

  test("readURL does not send GitHub token to non-GitHub URLs") {
    withSystemProperty(GitHubTokenProperty, "secret-token") {
      val url = URI.create("https://example.com/rules/Rule.scala").toURL

      assert(
        FileOps.readURL(
          url,
          authenticatedConnection(
            expectedUrl = "https://example.com/rules/Rule.scala",
            expectedAuthorization = None,
            expectedAccept = None
          )
        ) == RuleSource
      )
    }
  }

  private def authenticatedConnection(
      expectedUrl: String,
      expectedAuthorization: Option[String],
      expectedAccept: Option[String]
  ): URL => URLConnection = { url =>
    assert(url.toString == expectedUrl)
    new URLConnection(url) {
      override def connect(): Unit = ()

      override def getInputStream: InputStream = {
        val obtainedAuthorization = Option(getRequestProperty("Authorization"))
        assert(
          obtainedAuthorization == expectedAuthorization,
          s"expected Authorization header $expectedAuthorization, obtained $obtainedAuthorization"
        )
        val obtainedAccept = Option(getRequestProperty("Accept"))
        assert(
          obtainedAccept == expectedAccept,
          s"expected Accept header $expectedAccept, obtained $obtainedAccept"
        )
        new ByteArrayInputStream(
          RuleSource.getBytes(StandardCharsets.UTF_8)
        )
      }
    }
  }

  private def withSystemProperty[A](
      key: String,
      value: String
  )(body: => A): A = {
    val original = Option(System.getProperty(key))
    System.setProperty(key, value)
    try body
    finally {
      original match {
        case Some(originalValue) => System.setProperty(key, originalValue)
        case None => System.clearProperty(key)
      }
    }
  }
}

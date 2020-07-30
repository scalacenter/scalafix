package scalafix.testkit

import scala.meta._

import org.scalatest.FunSpecLike
import scalafix.internal.reflect.RuleCompiler
import scalafix.internal.testkit.EndOfLineAssertExtractor
import scalafix.internal.testkit.MultiLineAssertExtractor
import scalafix.v0.SemanticdbIndex

@deprecated(
  "Use AbstractSemanticRuleSuite with the styling trait of your choice mixed-in (*SpecLike or *SuiteLike)",
  "0.9.18"
)
class SemanticRuleSuite(
    override val props: TestkitProperties,
    override val isSaveExpect: Boolean
) extends AbstractSemanticRuleSuite
    with FunSpecLike {
  def this(props: TestkitProperties) = this(props, isSaveExpect = false)
  def this() = this(TestkitProperties.loadFromResources())

  @deprecated(
    "Use empty constructor instead. Arguments are passed as resource 'scalafix-testkit.properties'",
    "0.6.0"
  )
  def this(
      index: SemanticdbIndex,
      inputSourceroot: AbsolutePath,
      expectedOutputSourceroot: Seq[AbsolutePath]
  ) = this()
}

object SemanticRuleSuite {
  def defaultClasspath(classDirectory: AbsolutePath): Classpath = Classpath(
    classDirectory ::
      RuleCompiler.defaultClasspathPaths.filter(path =>
        path.toNIO.getFileName.toString.contains("scala-library")
      )
  )

  def stripTestkitComments(input: String): String =
    stripTestkitComments(input.tokenize.get)

  def stripTestkitComments(tokens: Tokens): String = {
    val configComment = findTestkitComment(tokens)
    tokens.filter {
      case `configComment` => false
      case EndOfLineAssertExtractor(_) => false
      case MultiLineAssertExtractor(_) => false
      case _ => true
    }.mkString
  }

  def findTestkitComment(tokens: Tokens): Token = {
    tokens
      .find { x =>
        x.is[Token.Comment] && x.syntax.startsWith("/*")
      }
      .getOrElse {
        val input = tokens.headOption.fold("the file")(_.input.syntax)
        throw new IllegalArgumentException(
          s"Missing /* */ comment at the top of $input"
        )
      }
  }

}

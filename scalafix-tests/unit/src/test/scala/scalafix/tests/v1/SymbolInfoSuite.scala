package scalafix.tests.v1
import org.scalatest.FunSuite
import scala.meta.internal.symtab.GlobalSymbolTable
import scalafix.internal.reflect.ClasspathOps
import scalafix.testkit.DiffAssertions
import scalafix.v1._

class SymbolInfoSuite extends FunSuite with DiffAssertions {
  private val classpath =
    ClasspathOps.bootClasspath.get ++ ClasspathOps.thisClasspath

  private val symtab = GlobalSymbolTable(classpath)
  private implicit val v1Symtab = new Symtab {
    override def info(symbol: Symbol): Option[SymbolInfo] =
      symtab.info(symbol.value).map(new SymbolInfo(_)(this))
  }

  def checkSyntax(original: String, expected: String): Unit = {
    test(original) {
      val info = v1Symtab.info(Symbol(original)).get
      assertNoDiff(info.toString, expected)
    }
  }

  checkSyntax(
    "java/lang/System#lineSeparator().",
    "java/lang/System#lineSeparator(). => static method lineSeparator(): String"
  )

  checkSyntax(
    "scala/Predef.assert().",
    "scala/Predef.assert(). => @elidable method assert(assertion: Boolean): Unit"
  )

}

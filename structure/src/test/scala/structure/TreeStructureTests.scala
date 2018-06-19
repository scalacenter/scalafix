package structure

import scala.meta._
import utest._

object TreeStructureTests extends TestSuite{
  val tests = Tests{
    "pretty(t)" - {
      val obtained = pretty(q"a.b.c.d")
      val expected = 
        """|Term.Select(
           |  Term.Select(
           |    Term.Select(
           |      Term.Name("a"),
           |      Term.Name("b")
           |    ),
           |    Term.Name("c")
           |  ),
           |  Term.Name("d")
           |)""".stripMargin
      assert(obtained == expected)
    }

    "pretty(t, showFieldNames = true)" - {
        val obtained = pretty(q"a.b.c.d", showFieldNames = true)
        val expected = 
          """|Term.Select(
             |  qual = Term.Select(
             |    qual = Term.Select(
             |      qual = Term.Name("a"),
             |      name = Term.Name("b")
             |    ),
             |    name = Term.Name("c")
             |  ),
             |  name = Term.Name("d")
             |)""".stripMargin
        assert(obtained == expected)
    }
  }
}

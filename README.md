# Pretty print scalameta structures

```scala
pretty(q"a.b.c.d")

Term.Select(
  Term.Select(
    Term.Select(
      Term.Name("a"),
      Term.Name("b")
    ),
    Term.Name("c")
  ),
  Term.Name("d")
)

pretty(q"a.b.c.d", showFieldNames = true) 

Term.Select(
  qual = Term.Select(
    qual = Term.Select(
      qual = Term.Name("a"),
      name = Term.Name("b")
    ),
    name = Term.Name("c")
  ),
  name = Term.Name("d")
)
```


## Ammonite

```scala
import $ivy.`org.typelevel::paiges-core:0.2.1`
import $ivy.`org.scalameta::contrib:3.7.4` 
import $ivy.`org.scalameta::testkit:3.7.4`

import scala.meta._
import scala.meta.Token._

import org.typelevel.paiges._

repl.pprinter() = {

  pprint.copy(additionalHandlers = { 
    case tree: Tree => pprint.Tree.Literal("\n" + structure.pretty(tree))
    case tokens: Tokens => pprint.Tree.Literal("\n" + tokens.structure)
  })
}
```
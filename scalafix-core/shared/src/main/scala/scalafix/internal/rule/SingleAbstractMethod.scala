package scalafix.internal.rule

import scala.meta._
import scala.meta.Token._

import scala.meta.internal.tokens.TokenInfo

import scalafix.Patch
import scalafix.SemanticdbIndex
import scalafix.rule.RuleCtx
import scalafix.rule.SemanticRule

import scala.collection.mutable

// SAM conversion: http://www.scala-lang.org/files/archive/spec/2.12/06-expressions.html#sam-conversion
case class SingleAbstractMethod(index: SemanticdbIndex)
    extends SemanticRule(index, "SingleAbstractMethod") {
  override def description: String = ???
  override def fix(ctx: RuleCtx): Patch = {
    val visited = mutable.Set.empty[Tree]
    def isSam(tree: Tree): Boolean =
      tree match {
        case Term.NewAnonymous(
            Template(
              _,
              List(Init(clazz, _, _)),
              _,
              List(Defn.Def(_, method, _, List(params), _, body))
            )
            ) if !visited.contains(tree) =>
          visited += tree

          val singleAbstractOverride =
            (for {
              definition <- index.denotation(method)
              overrideSymbol <- definition.overrides.headOption
              if definition.overrides.size == 1
              overrideDefinition <- index.denotation(overrideSymbol)
            } yield overrideDefinition.isAbstract).getOrElse(false)

          val singleMember =
            (for {
              symbol <- index.symbol(clazz)
              denfinition <- index.denotation(symbol)
            } yield denfinition.members.size == 1).getOrElse(false)

          singleAbstractOverride && singleMember

        case _ => false
      }

    class PeekableIterator[T](iterator: Iterator[T]) extends Iterator[T] {
      private var exhausted: Boolean = false
      private var slot: Option[T] = None
      private def fill(): Unit = {
        if (!(exhausted || slot.isDefined)) {
          if (iterator.hasNext) {
            slot = Some(iterator.next())
          } else {
            exhausted = true
            slot = None
          }
        }
      }
      override def hasNext: Boolean = {
        if (exhausted) {
          false
        } else {
          if (slot.isDefined) true
          else iterator.hasNext
        }
      }
      def peek(): Option[T] = {
        fill()
        if (exhausted) None
        else slot
      }
      def next: T = {
        if (!hasNext) throw new NoSuchElementException()
        val out = slot.getOrElse(iterator.next)
        slot = None
        out
      }
    }

    class PatchBuilder(tokens: Tokens) {
      private val iterator = new PeekableIterator(tokens.iterator)
      private val patches = List.newBuilder[Patch]
      def next: Token = iterator.next
      def find[T <: Token : TokenInfo]: Option[Token] =
        iterator.find(_.is[T](implicitly[TokenInfo[T]]))
      def remove[T <: Token : TokenInfo]: Unit = 
        doRemove(find[T])
      def remove(t: Token): Unit =
        patches += ctx.removeToken(t)
      def removeOptional[T <: Token : TokenInfo]: Unit = 
        if (iterator.peek.forall(_.is[T])) remove[T]
      def removeLast[T <: Token : TokenInfo]: Unit = {
        var last: Option[Token] = None
        while (iterator.hasNext) {
          last = find[T]
        }
        doRemove(last)
      }
      def addRight[T <: Token : TokenInfo](toAdd: String): Unit =
        doOp(find[T], tt => patches += ctx.addRight(tt, toAdd))
      def addLeft(t: Token, what: String): Unit = 
        patches += ctx.addLeft(t, what)
      def doRemove[T <: Token](t: Option[Token])(implicit ev: TokenInfo[T]): Unit =
        doOp(t, tt => patches += ctx.removeToken(tt))
      def doOp[T <: Token](t: Option[Token], op: Token => Unit)(implicit ev: TokenInfo[T]): Unit =
        t match {
          case Some(t) => op(t)
          case _ =>
            throw new Exception(
              s"""|cannot find ${ev.name}
                  |Tokens:
                  |$tokens""".stripMargin
            )
        }
      def result(): Patch = Patch.fromIterable(patches.result())
    }

    def patchSam(builder: PatchBuilder, keepClassName: Boolean): Patch = {
      import builder._

      // new X(){def m(a: A, b: B, c: C): D = <body> }
      remove[KwNew]
      if (!keepClassName) {
        remove[Ident]
      } else {
        addRight[Ident](" =") // class name
      }
      removeOptional[LeftParen]
      removeOptional[RightParen]

      remove[LeftBrace]
      remove[KwDef]
      remove[Ident] // method name

      // TODO: it's possible to remove the types in the parameter list
      // val withParams: WithParams = (a, b) => a + b

      // TODO: it's possible to remove the () when it has only one argument
      // val a: A = x => x

      // It's possible to have an empty parameter list
      val token = next
      if(token.is[Colon]) {
        remove(token)
        remove[Ident] // return type
      } else {
        // BUG: It's possible to have an empty return type

      }
      
      // BUG: It's possible to have procedure syntax
      remove[Equals]

      val body = next
      addLeft(body, " => ")
      
      removeLast[RightBrace]
      result()
    }

    def patchSamDefn(hasDecltpe: Boolean, tokens: Tokens): Patch = {
      if (!hasDecltpe) {
        // val v = new X(){def m(a: A, b: B, c: C): D = <body> }
        // val v: X = (a: A, b: B, c: C) => <body>
        val builder = new PatchBuilder(tokens)
        import builder._

        addRight[Ident](":")
        remove[Equals]
        patchSam(builder, keepClassName = true)
      } else {
        // trait B { def f(a: Int): Int }
        // trait C extends B
        // val c2: B = new C { def f(a: Int): Int = a }

        // what do we do here ?
        // val c2: C would expose C
        // val c2: B would remove C

        Patch.empty
      }
    }

    ctx.tree.collect {
      case term: Defn.Val if isSam(term.rhs) =>
        patchSamDefn(term.decltpe.nonEmpty, term.tokens)
      case term: Defn.Var if term.rhs.map(isSam).getOrElse(false) =>
        patchSamDefn(term.decltpe.nonEmpty, term.tokens)
      case term: Defn.Def if isSam(term.body) =>
        patchSamDefn(term.decltpe.nonEmpty, term.tokens)
      case term: Term.NewAnonymous if isSam(term) =>
        patchSam(new PatchBuilder(term.tokens), keepClassName = false)

    }.asPatch
  }
}

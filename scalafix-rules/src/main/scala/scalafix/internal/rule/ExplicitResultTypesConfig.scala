package scalafix.internal.rule

import scala.{meta => m}

import metaconfig.Conf.Bool
import metaconfig.Conf.Lst
import metaconfig.Conf.Str
import metaconfig._
import metaconfig.annotation._
import metaconfig.generic.Surface
import scalafix.internal.config._

case class ExplicitResultTypesConfig(
    @Description("Enable/disable this rule for defs, vals or vars.")
    @ExampleValue("[Def, Val, Var]")
    memberKind: List[MemberKind] =
      List(MemberKind.Def, MemberKind.Val, MemberKind.Var),
    @Description("Enable/disable this rule for private/protected members.")
    @ExampleValue("[Public, Protected]")
    memberVisibility: List[MemberVisibility] =
      List(MemberVisibility.Public, MemberVisibility.Protected),
    @Description(
      "If false, insert explicit result types even for simple definitions like `val x = 2`"
    )
    @ExampleValue("['Lit', 'Term.New']")
    skipSimpleDefinitions: SimpleDefinitions = SimpleDefinitions.default,
    @Hidden
    skipLocalImplicits: Boolean = true,
    @Description(
      "If true, report and fail unexpected errors. " +
        "If false, silently ignore errors to produce an explicit result type."
    )
    fatalWarnings: Boolean = false,
    @Description(
      "If false, disables rewriting of inferred structural types to named subclasses. " +
        "Beware that this option may produce code that no longer compiles if it previously " +
        " used `scala.language.reflectiveCalls` to access methods on structural types."
    )
    rewriteStructuralTypesToNamedSubclass: Boolean = true,
    @Description("If true, adds result types only to implicit definitions.")
    onlyImplicits: Boolean = false,
    @Hidden()
    symbolReplacements: Map[String, String] = Map.empty
)

object ExplicitResultTypesConfig {
  val default: ExplicitResultTypesConfig = ExplicitResultTypesConfig()
  implicit val reader: ConfDecoder[ExplicitResultTypesConfig] =
    generic.deriveDecoder[ExplicitResultTypesConfig](default)
  implicit val surface: Surface[ExplicitResultTypesConfig] =
    generic.deriveSurface[ExplicitResultTypesConfig]
}

case class SimpleDefinitions(kinds: Set[String]) {

  import scala.meta.classifiers.XtensionClassifiable

  private def isSimpleRef(tree: m.Tree): Boolean = tree match {
    case _: m.Name => true
    case t: m.Term.Select => isSimpleRef(t.qual)
    case _ => false
  }

  def isSimpleDefinition(body: m.Term): Boolean = {
    val kind =
      if (body.is[m.Lit]) Some("Lit")
      else if (body.is[m.Term.New]) Some("Term.New")
      else if (isSimpleRef(body)) Some("Term.Ref")
      else None
    kind.exists(kinds.contains)
  }
}
object SimpleDefinitions {
  def allKinds: Set[String] = Set("Term.Ref", "Lit", "Term.New")
  def default: SimpleDefinitions = SimpleDefinitions(allKinds)
  implicit val encoder: ConfEncoder[SimpleDefinitions] =
    ConfEncoder.instance[SimpleDefinitions](x =>
      Conf.Lst(x.kinds.toList.map(Conf.Str(_)))
    )
  implicit val decoder: ConfDecoder[SimpleDefinitions] =
    ConfDecoder.instanceExpect[SimpleDefinitions]("List[String]") {
      case Bool(false) => Configured.ok(SimpleDefinitions(Set.empty))
      case Bool(true) => Configured.ok(SimpleDefinitions(Set.empty))
      case conf @ Lst(values) =>
        val strings = values.collect { case Str(kind) =>
          kind
        }
        if (strings.length == values.length) {
          Configured.ok(SimpleDefinitions(strings.toSet))
        } else {
          Configured.typeMismatch("List[String]", conf)
        }
    }
}
sealed trait MemberVisibility
object MemberVisibility {
  case object Public extends MemberVisibility
  case object Protected extends MemberVisibility
  case object Private extends MemberVisibility
  def all: List[MemberVisibility] =
    List(Public, Protected, Private)
  implicit val readerMemberVisibility: ConfDecoder[MemberVisibility] =
    ReaderUtil.fromMap(all.map(x => x.toString -> x).toMap)
}

sealed trait MemberKind
object MemberKind {
  case object Def extends MemberKind
  case object Val extends MemberKind
  case object Var extends MemberKind
  def all: List[MemberKind] =
    List(Def, Val, Var)
  implicit val readerMemberKind: ConfDecoder[MemberKind] =
    ReaderUtil.fromMap(all.map(x => x.toString -> x).toMap)
}

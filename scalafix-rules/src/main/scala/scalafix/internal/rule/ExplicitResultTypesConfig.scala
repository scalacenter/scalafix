package scalafix.internal.rule

import metaconfig._
import metaconfig.generic.Surface
import metaconfig.annotation._
import scalafix.internal.config._

case class ExplicitResultTypesConfig(
    @Description("Enable/disable this rule for defs, vals or vars.")
    @ExampleValue("[Def, Val, Var]")
    memberKind: List[MemberKind] = Nil,
    @Description("Enable/disable this rule for private/protected members.")
    @ExampleValue("[Public, Protected]")
    memberVisibility: List[MemberVisibility] = Nil,
    @Description(
      "If false, insert explicit result types even for simple definitions like `val x = 2`")
    skipSimpleDefinitions: Boolean = true,
    @Description(
      "If false, insert explicit result types even for locally defined implicit vals")
    skipLocalImplicits: Boolean = true,
    // Experimental, still blocked by https://github.com/scalameta/scalameta/issues/1099
    // to work for defs. May insert names that conflicts with existing names in scope.
    // Use at your own risk.
    @Description(
      "If true, does a best-effort at inserting short names and add missing imports. " +
        "WARNING. This feature is currently implemented in a naive way and it contains many bugs.")
    unsafeShortenNames: Boolean = false,
    @Description(
      "If true, report and fail unexpected errors. " +
        "If false, silently ignore errors to produce an explicit result type."
    )
    fatalWarnings: Boolean = false
)

object ExplicitResultTypesConfig {
  val default: ExplicitResultTypesConfig = ExplicitResultTypesConfig()
  implicit val reader: ConfDecoder[ExplicitResultTypesConfig] =
    generic.deriveDecoder[ExplicitResultTypesConfig](default)
  implicit val surface: Surface[ExplicitResultTypesConfig] =
    generic.deriveSurface[ExplicitResultTypesConfig]
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

package scalafix.internal.config

import metaconfig._

case class ExplicitReturnTypesConfig(
    memberKind: List[MemberKind] = Nil,
    memberVisibility: List[MemberVisibility] = Nil,
    skipSimpleDefinitions: Boolean = true
) {
  implicit val reader: ConfDecoder[ExplicitReturnTypesConfig] =
    ConfDecoder.instanceF[ExplicitReturnTypesConfig] { c =>
      (
        c.getOrElse("memberKind")(memberKind) |@|
          c.getOrElse("memberVisibility")(memberVisibility) |@|
          c.getOrElse("skipSimpleDefinitions")(skipSimpleDefinitions)
      ).map {
        case ((a, b), c) =>
          ExplicitReturnTypesConfig(a, b, c)
      }
    }
}

object ExplicitReturnTypesConfig {
  val default = ExplicitReturnTypesConfig()
  implicit val reader: ConfDecoder[ExplicitReturnTypesConfig] = default.reader
}

sealed trait MemberVisibility
object MemberVisibility {
  case object Public extends MemberVisibility
  case object Protected extends MemberVisibility
  case object Private extends MemberVisibility
  def all = List(Public, Protected, Private)
  implicit val readerMemberVisibility: ConfDecoder[MemberVisibility] =
    ReaderUtil.fromMap(all.map(x => x.toString -> x).toMap)
}

sealed trait MemberKind
object MemberKind {
  case object Def extends MemberKind
  case object Val extends MemberKind
  case object Var extends MemberKind
  def all = List(Def, Val, Var)
  implicit val readerMemberKind: ConfDecoder[MemberKind] =
    ReaderUtil.fromMap(all.map(x => x.toString -> x).toMap)
}

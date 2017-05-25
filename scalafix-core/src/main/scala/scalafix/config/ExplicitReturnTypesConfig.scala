package scalafix.config

import metaconfig._

@DeriveConfDecoder
case class ExplicitReturnTypesConfig(
    memberKind: Seq[MemberKind] = Nil,
    memberVisibility: Seq[MemberVisibility] = Nil
)

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

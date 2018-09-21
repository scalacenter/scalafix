package scalafix.internal.v1

import scala.meta.internal.{semanticdb => s}
import scalafix.v1._

object SymtabFromProtobuf {
  def apply(symtab: Symtab): SymtabFromProtobuf =
    new SymtabFromProtobuf(symtab)
}
final class SymtabFromProtobuf(symtab: Symtab) {

  def info(sym: String): SymbolInformation =
    symtab.info(Symbol(sym)).getOrElse(throw new NoSuchElementException(sym))

  def sscope(scope: Option[s.Scope]): List[SymbolInformation] = scope match {
    case None => Nil
    case Some(sc) =>
      if (sc.hardlinks.isEmpty) sc.symlinks.iterator.map(info).toList
      else {
        sc.infos.iterator.map(i => new SymbolInformation(i)(symtab)).toList
      }
  }

  def stype(t: s.Type): SemanticType = t match {
    case s.IntersectionType(types) =>
      IntersectionType(types.convert)
    case s.SuperType(prefix, symbol) =>
      SuperType(prefix.convert, symbol.convert)
    case s.ByNameType(tpe) =>
      ByNameType(tpe.convert)
    case s.AnnotatedType(annotations, tpe) =>
      AnnotatedType(annotations.convert, tpe.convert)
    case s.ConstantType(constant) =>
      ConstantType(sconstant(constant))
    case s.TypeRef(prefix, symbol, typeArguments) =>
      TypeRef(prefix.convert, symbol.convert, typeArguments.convert)
    case s.StructuralType(tpe, declarations) =>
      StructuralType(tpe.convert, declarations.convert)
    case s.RepeatedType(tpe) =>
      RepeatedType(tpe.convert)
    case s.ThisType(symbol) =>
      ThisType(symbol.convert)
    case s.WithType(types) =>
      WithType(types.convert)
    case s.UniversalType(typeParameters, tpe) =>
      UniversalType(typeParameters.convert, tpe.convert)
    case s.SingleType(prefix, symbol) =>
      SingleType(prefix.convert, symbol.convert)
    case s.ExistentialType(tpe, declarations) =>
      ExistentialType(tpe.convert, declarations.convert)
    case s.UnionType(types) =>
      UnionType(types.convert)
    case s.NoType =>
      NoType
  }

  def sconstant(c: s.Constant): Constant = c match {
    case s.NoConstant =>
      throw new IllegalArgumentException(c.toString)
    case s.UnitConstant() =>
      UnitConstant
    case s.BooleanConstant(value) =>
      BooleanConstant(value)
    case s.ByteConstant(value) =>
      ByteConstant(value.toByte)
    case s.ShortConstant(value) =>
      ShortConstant(value.toShort)
    case s.CharConstant(value) =>
      CharConstant(value.toChar)
    case s.IntConstant(value) =>
      IntConstant(value)
    case s.LongConstant(value) =>
      LongConstant(value)
    case s.FloatConstant(value) =>
      FloatConstant(value)
    case s.DoubleConstant(value) =>
      DoubleConstant(value)
    case s.StringConstant(value) =>
      StringConstant(value)
    case s.NullConstant() =>
      NullConstant
  }

  def ssignature(sig: s.Signature): Signature = sig match {
    case s.ValueSignature(tpe) =>
      ValueSignature(tpe.convert)
    case s.TypeSignature(typeParameters, lowerBound, upperBound) =>
      TypeSignature(
        typeParameters.convert,
        lowerBound.convert,
        upperBound.convert
      )
    case s.ClassSignature(typeParameters, parents, self, declarations) =>
      ClassSignature(
        typeParameters.convert,
        parents.convert,
        self.convert,
        declarations.convert
      )
    case s.MethodSignature(typeParameters, parameterLists, returnType) =>
      MethodSignature(
        typeParameters.convert,
        parameterLists.convert,
        returnType.convert
      )
    case s.NoSignature =>
      NoSignature
  }

  def sannotation(a: s.Annotation): Annotation =
    Annotation(a.tpe.convert)

  implicit class RichAnnotations(annots: Seq[s.Annotation]) {
    def convert: List[Annotation] = annots.iterator.map(sannotation).toList
  }
  implicit class RichSymbol(sym: String) {
    def convert: Symbol = Symbol(sym)
  }
  implicit class RichType(t: s.Type) {
    def convert: SemanticType = stype(t)
  }
  implicit class RichTypes(types: Seq[s.Type]) {
    def convert: List[SemanticType] = types.iterator.map(stype).toList
  }
  implicit class RichScope(scope: Option[s.Scope]) {
    def convert: List[SymbolInformation] = sscope(scope)
  }
  implicit class RichScopes(scopes: Seq[s.Scope]) {
    def convert: List[List[SymbolInformation]] =
      scopes.iterator.map(s => sscope(Some(s))).toList
  }
}

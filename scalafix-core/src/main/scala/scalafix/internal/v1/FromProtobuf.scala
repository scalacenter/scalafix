package scalafix.internal.v1

import scala.meta.internal.{semanticdb => s}
import scalafix.v1._

class FromProtobuf(doc: SemanticDoc) {

  def info(sym: String): SymbolInfo =
    doc.internal
      .info(Symbol(sym))
      .getOrElse(throw new NoSuchElementException(sym))

  def sscope(scope: Option[s.Scope]): List[SymbolInfo] = scope match {
    case None => Nil
    case Some(sc) =>
      if (sc.hardlinks.isEmpty) sc.symlinks.iterator.map(info).toList
      else sc.infos.iterator.map(i => new SymbolInfo(i)).toList
  }

  def stype(t: s.Type): Tpe = t match {
    case s.IntersectionType(types) =>
      new IntersectionTpe(types.convert)
    case s.SuperType(prefix, symbol) =>
      new SuperTpe(prefix.convert, symbol.convert)
    case s.ByNameType(tpe) =>
      new ByNameTpe(tpe.convert)
    case s.AnnotatedType(annotations, tpe) =>
      new AnnotatedTpe(annotations.convert, tpe.convert)
    case s.ConstantType(constant) =>
      new ConstantTpe(sconstant(constant))
    case s.TypeRef(prefix, symbol, typeArguments) =>
      new TpeRef(prefix.convert, symbol.convert, typeArguments.convert)
    case s.StructuralType(tpe, declarations) =>
      new StructuralTpe(tpe.convert, declarations.convert)
    case s.RepeatedType(tpe) =>
      new RepeatedTpe(tpe.convert)
    case s.ThisType(symbol) =>
      new ThisTpe(symbol.convert)
    case s.WithType(types) =>
      new WithTpe(types.convert)
    case s.UniversalType(typeParameters, tpe) =>
      new UniversalTpe(typeParameters.convert, tpe.convert)
    case s.SingleType(prefix, symbol) =>
      new SingleTpe(prefix.convert, symbol.convert)
    case s.ExistentialType(tpe, declarations) =>
      new ExistentialTpe(tpe.convert, declarations.convert)
    case s.UnionType(types) =>
      new UnionTpe(types.convert)
    case s.NoType =>
      NoTpe
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
      new ValueSignature(tpe.convert)
    case s.TypeSignature(typeParameters, lowerBound, upperBound) =>
      new TypeSignature(
        typeParameters.convert,
        lowerBound.convert,
        upperBound.convert
      )
    case s.ClassSignature(typeParameters, parents, self, declarations) =>
      new ClassSignature(
        typeParameters.convert,
        parents.convert,
        self.convert,
        declarations.convert
      )
    case s.MethodSignature(typeParameters, parameterLists, returnType) =>
      new MethodSignature(
        typeParameters.convert,
        parameterLists.convert,
        returnType.convert
      )
    case s.NoSignature =>
      NoSignature
  }

  def sannotation(a: s.Annotation): Annotation =
    new Annotation(a.tpe.convert)

  implicit class RichAnnotations(annots: Seq[s.Annotation]) {
    def convert: List[Annotation] = annots.iterator.map(sannotation).toList
  }
  implicit class RichSymbol(sym: String) {
    def convert: Symbol = Symbol(sym)
  }
  implicit class RichType(t: s.Type) {
    def convert: Tpe = stype(t)
  }
  implicit class RichTypes(types: Seq[s.Type]) {
    def convert: List[Tpe] = types.iterator.map(stype).toList
  }
  implicit class RichScope(scope: Option[s.Scope]) {
    def convert: List[SymbolInfo] = sscope(scope)
  }
  implicit class RichScopes(scopes: Seq[s.Scope]) {
    def convert: List[List[SymbolInfo]] =
      scopes.iterator.map(s => sscope(Some(s))).toList
  }
}

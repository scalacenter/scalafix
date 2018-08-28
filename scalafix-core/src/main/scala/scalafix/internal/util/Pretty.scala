package scalafix.internal.util

import org.typelevel.paiges.Doc
import scala.collection.mutable.ListBuffer
import scala.meta.Lit
import scalafix.v1._

object Pretty {
  import DocConstants._

  def pretty(constant: Constant): Doc = constant match {
    case UnitConstant => unit
    case BooleanConstant(value) => Doc.str(value)
    case ByteConstant(value) => Doc.text(Lit.Byte(value).syntax)
    case ShortConstant(value) => Doc.text(Lit.Short(value).syntax)
    case CharConstant(value) => Doc.text(Lit.Char(value).syntax)
    case IntConstant(value) => Doc.str(value)
    case LongConstant(value) => Doc.text(Lit.Long(value).syntax)
    case FloatConstant(value) => Doc.text(Lit.Float(value).syntax)
    case DoubleConstant(value) => Doc.text(Lit.Double(value).syntax)
    case StringConstant(value) => Doc.text(Lit.String(value).syntax)
    case NullConstant => `null`
  }

  def pretty(sym: Symbol): Doc =
    Doc.text(sym.displayName)

  def pretty(tpe: SType): Doc = {
    def prefix(tpe: SType): Doc = {
      tpe match {
        case NoType => Doc.str("<no type>")
        case t: TypeRef =>
          val pre: Doc = t.prefix match {
            case _: SingleType | _: ThisType | _: SuperType =>
              prefix(t.prefix) + dot
            case NoType =>
              Doc.empty
            case _ =>
              prefix(t.prefix) + hash
          }
          val symbol = pretty(t.symbol)
          val targs =
            if (t.typeArguments.isEmpty) Doc.empty
            else {
              leftBracket +
                Doc.intercalate(Doc.comma, t.typeArguments.map(normal)) +
                rightBracket
            }
          pre + symbol + targs
        case t: SingleType =>
          if (t.prefix.isEmpty) pretty(t.symbol)
          else prefix(t.prefix) + dot + pretty(t.symbol)
        case t: ThisType =>
          if (t.symbol.isNone) pretty(t.symbol)
          else pretty(t.symbol) + dot + `this`
        case t: SuperType =>
          val pre =
            if (t.prefix.isEmpty) Doc.empty
            else prefix(t.prefix) + dot
          val sym =
            if (t.symbol.isNone) Doc.empty
            else leftBracket + pretty(t.symbol) + rightBracket
          pre + `super` + sym
        case t: ConstantType =>
          pretty(t.constant)
        case t: IntersectionType =>
          Doc.intercalate(
            Doc.space + ampersand + Doc.space,
            t.types.map(normal)
          )
        case t: UnionType =>
          Doc.intercalate(
            Doc.space + pipe + Doc.space,
            t.types.map(normal)
          )
        case t: WithType =>
          Doc.intercalate(
            Doc.space + `with` + Doc.space,
            t.types.map(normal)
          )
        case t: StructuralType =>
          val init = normal(t.tpe)
          val declarations =
            if (t.declarations.isEmpty) {
              Doc.space + leftCurly + rightCurly
            } else {
              Doc.space +
                Doc
                  .intercalate(Doc.line, t.declarations.map(pretty))
                  .tightBracketBy(leftCurly, rightCurly)
            }
          init + declarations
        case t: AnnotatedType =>
          val annotations = t.annotations.filter(_.tpe.nonEmpty)
          val annots =
            if (annotations.isEmpty) {
              Doc.empty
            } else {
              Doc.space + Doc.intercalate(Doc.space, annotations.map(pretty))
            }
          normal(t.tpe) + annots
        case t: ExistentialType =>
          val init = normal(t.tpe)
          val decls = Doc
            .intercalate(Doc.line, t.declarations.map(pretty))
            .tightBracketBy(
              Doc.space + `forSome` + Doc.space + leftCurly,
              rightCurly)
          init + decls
        case t: UniversalType =>
          val tparams = Doc
            .intercalate(Doc.comma, t.typeParameters.map(pretty))
            .tightBracketBy(
              leftBracket,
              rightBracket + Doc.space + rightArrow + Doc.space)
          normal(t.tpe) + tparams
        case t: ByNameType =>
          rightArrow + Doc.space + pretty(t.tpe)
        case t: RepeatedType =>
          asterisk + Doc.space + pretty(t.tpe)
      }
    }
    def normal(tpe: SType): Doc = tpe match {
      case _: SingleType | _: ThisType | _: SuperType =>
        prefix(tpe) + dot + `type`
      case _ =>
        prefix(tpe)
    }
    normal(tpe)
  }

  def pretty(annotation: Annotation): Doc =
    if (annotation.tpe.isEmpty) Doc.empty
    else `@` + pretty(annotation.tpe)

  def pretty(info: SymbolInfo): Doc = {
    val symbol = Doc.text("/* ") + Doc.text(info.sym.value) + Doc.text(" */ ")
    val annotations = info.annotations.filter(_.tpe.nonEmpty)
    val annotation =
      if (annotations.isEmpty) Doc.empty
      else {
        Doc
          .intercalate(
            Doc.space,
            info.annotations.filter(_.tpe.nonEmpty).map(pretty)) +
          Doc.space
      }
    val saccess =
      if (info.isPrivate) "private"
      else if (info.isPrivateThis) "private[this]"
      else if (info.isPrivateWithin) s"private[${pretty(info.within.get)}]"
      else if (info.isProtected) "protected"
      else if (info.isProtectedThis) "protected[this]"
      else if (info.isProtectedWithin) s"protected[${pretty(info.within.get)}]"
      else ""
    val access =
      if (saccess.isEmpty) Doc.empty
      else Doc.text(saccess) + Doc.space
    val mods = ListBuffer.empty[Doc]
    def append(mod: String): Unit = mods += Doc.text(mod)
    if (info.isAbstract) append("abstract")
    if (info.isFinal && !info.isObject) append("final")
    if (info.isSealed) append("sealed")
    if (info.isImplicit) append("implicit")
    if (info.isLazy) append("lazy")
    if (info.isCase) append("case")
    if (info.isCovariant) append("<covariant>")
    if (info.isContravariant) append("<contravariant>")
    if (info.isStatic) append("static")
    if (info.isPrimary) append("<primary>")
    if (info.isDefault) append("<default>")

    val property =
      if (mods.isEmpty) Doc.empty
      else Doc.intercalate(Doc.space, mods) + Doc.space

    val skind =
      if (info.isMethod || info.isLocal) {
        if (info.isVal) "val"
        else if (info.isVar && !info.isSetter) "var"
        else if (info.isScala) "def"
        else "java-method"
      } else if (info.isField) {
        if (info.isScala) "val"
        else "java-field"
      } else if (info.isTrait) "trait"
      else if (info.isInterface) "interface"
      else if (info.isClass) "class"
      else if (info.isObject) "object"
      else if (info.isPackageObject) "package object"
      else if (info.isType) "type"
      else if (info.isConstructor) "def"
      else if (info.isMacro) "def"
      else if (info.isParameter) ""
      else if (info.isTypeParameter) ""
      else if (info.isSelfParameter) ""
      else throw new IllegalArgumentException(info.toString)
    val kind =
      if (skind.isEmpty) Doc.empty
      else Doc.text(skind) + Doc.space
    val name =
      if (info.isConstructor) Doc.text("this")
      else Doc.text(info.displayName)

    val signature =
      if (info.isConstructor) Doc.empty
      else if (info.isParameter) `:` + Doc.space + pretty(info.signature)
      else pretty(info.signature)

    symbol + annotation + access + property + kind + name + signature
  }

  def prettyTypeParameters(typeParameters: List[SymbolInfo]): Doc = {
    if (typeParameters.isEmpty) Doc.empty
    else {
      leftBracket +
        Doc.intercalate(Doc.comma, typeParameters.map(pretty)) +
        rightBracket
    }
  }
  def pretty(signature: Signature): Doc = signature match {
    case NoSignature => Doc.empty
    case t: ClassSignature =>
      val tparams = prettyTypeParameters(t.typeParameters)
      val parents = `extends` + Doc.space +
        Doc.intercalate(Doc.space + `with` + Doc.space, t.parents.map(pretty))
      val body =
        if (t.self.isEmpty && t.declarations.isEmpty) Doc.empty
        else {
          val self =
            if (t.self.isEmpty) Doc.empty
            else Doc.text("self: ") + pretty(t.self) + Doc.text(" =>")
          val declarations =
            if (t.declarations.isEmpty) Doc.empty
            else
              Doc.text(s" +") + Doc.str(t.declarations.length) +
                Doc.text(" decls ")
          Doc.space + leftCurly + self + declarations + rightCurly
        }
      tparams + Doc.space + parents + body
    case t: MethodSignature =>
      val tparams = prettyTypeParameters(t.typeParameters)
      val params =
        if (t.parameterLists.isEmpty) Doc.empty
        else {
          val params = t.parameterLists.map { parameterList =>
            Doc.intercalate(Doc.comma, parameterList.map(pretty))
          }
          val paramss = Doc.intercalate(rightParen + leftParen, params)
          leftParen + paramss + rightParen
        }
      val returnType = pretty(t.returnType)
      tparams + params + `:` + Doc.space + returnType
    case t: TypeSignature =>
      val tparams = prettyTypeParameters(t.typeParameters)
      if (t.lowerBound == t.upperBound) {
        tparams + Doc.text(" = ") + pretty(t.lowerBound)
      } else {
        val lowerBound =
          if (t.lowerBound == Types.scalaNothing) Doc.empty
          else Doc.text(" >: ") + pretty(t.lowerBound)
        val upperBound =
          if (t.upperBound == Types.scalaAny) Doc.empty
          else if (t.upperBound == Types.javaObject) Doc.empty
          else Doc.text(" <: ") + pretty(t.upperBound)
        tparams + lowerBound + upperBound
      }
    case t: ValueSignature =>
      pretty(t.tpe)
  }

  def pretty(tree: STree): Doc = tree match {
    case NoTree =>
      Doc.empty
    case t: IdTree =>
      pretty(t.symbol)
    case t: SelectTree =>
      pretty(t.qualifier) + dot + pretty(t.id)
    case t: ApplyTree =>
      Doc
        .intercalate(Doc.comma, t.arguments.map(pretty))
        .tightBracketBy(pretty(t.function) + leftParen, rightParen)
    case t: TypeApplyTree =>
      val targs = Doc.intercalate(Doc.comma, t.typeArguments.map(pretty))
      pretty(t.function) + leftBracket + targs + rightBracket
    case t: FunctionTree =>
      val params = leftParen +
        Doc.intercalate(Doc.comma, t.parameters.map(pretty)) +
        rightParen + Doc.space + rightArrow
      pretty(t.body).bracketBy(leftCurly + Doc.space + params, rightCurly)
    case t: LiteralTree =>
      pretty(t.constant)
    case t: MacroExpansionTree =>
      leftParen + Doc.text("`after-expansion` : ") + pretty(t.tpe) + rightParen
    case r: OriginalTree =>
      if (r.matchesFullOriginalRange) asterisk
      else Doc.text("orig") + leftParen + Doc.text(r.tree.syntax) + rightParen
  }

}

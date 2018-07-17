package scalafix.v0

trait HasFlags {
  import Flags._
  def flags: Long
  def hasFlag(flag: Long): Boolean = (flags & flag) == flag

  def isVal: Boolean = hasFlag(VAL)
  def isVar: Boolean = hasFlag(VAR)
  def isMethod: Boolean = hasFlag(METHOD)
  def isMacro: Boolean = hasFlag(MACRO)
  def isType: Boolean = hasFlag(TYPE)
  def isParam: Boolean = hasFlag(PARAM)
  def isSelfParam: Boolean = hasFlag(SELFPARAM)
  def isTypeParam: Boolean = hasFlag(TYPEPARAM)
  def isObject: Boolean = hasFlag(OBJECT)
  def isPackage: Boolean = hasFlag(PACKAGE)
  def isPackageObject: Boolean = hasFlag(PACKAGEOBJECT)
  def isClass: Boolean = hasFlag(CLASS)
  def isTrait: Boolean = hasFlag(TRAIT)
  def isInterface: Boolean = hasFlag(INTERFACE)
  def isPrivate: Boolean = hasFlag(PRIVATE)
  def isProtected: Boolean = hasFlag(PROTECTED)
  def isAbstract: Boolean = hasFlag(ABSTRACT)
  def isFinal: Boolean = hasFlag(FINAL)
  def isSealed: Boolean = hasFlag(SEALED)
  def isImplicit: Boolean = hasFlag(IMPLICIT)
  def isLazy: Boolean = hasFlag(LAZY)
  def isCase: Boolean = hasFlag(CASE)
  def isCovariant: Boolean = hasFlag(COVARIANT)
  def isContravariant: Boolean = hasFlag(CONTRAVARIANT)
  def isInline: Boolean = hasFlag(INLINE)
  def isJavaDefined: Boolean = hasFlag(JAVADEFINED)
  def isLocal: Boolean = hasFlag(LOCAL)
  def isField: Boolean = hasFlag(FIELD)
  def isCtor: Boolean = hasFlag(CTOR)
  def isPrimary: Boolean = hasFlag(PRIMARY)
  def isEnum: Boolean = hasFlag(ENUM)
  def isStatic: Boolean = hasFlag(STATIC)

  protected def flagSyntax: String = {
    val buf = new StringBuilder
    def append(flag: String) = {
      if (buf.isEmpty) buf ++= flag
      else buf ++= (" " + flag)
    }
    def hasFlag(flag: Long) = (flags & flag) == flag
    if (hasFlag(PRIVATE)) append("PRIVATE")
    if (hasFlag(PROTECTED)) append("PROTECTED")
    if (hasFlag(ABSTRACT)) append("ABSTRACT")
    if (hasFlag(FINAL)) append("FINAL")
    if (hasFlag(SEALED)) append("SEALED")
    if (hasFlag(IMPLICIT)) append("IMPLICIT")
    if (hasFlag(LAZY)) append("LAZY")
    if (hasFlag(CASE)) append("CASE")
    if (hasFlag(COVARIANT)) append("COVARIANT")
    if (hasFlag(CONTRAVARIANT)) append("CONTRAVARIANT")
    if (hasFlag(INLINE)) append("INLINE")
    if (hasFlag(JAVADEFINED)) append("JAVADEFINED")
    if (hasFlag(PRIMARY)) append("PRIMARY")
    if (hasFlag(ENUM)) append("ENUM")
    if (hasFlag(STATIC)) append("STATIC")
    if (hasFlag(VAL)) append("VAL")
    if (hasFlag(VAR)) append("VAR")
    if (hasFlag(METHOD)) append("METHOD")
    if (hasFlag(MACRO)) append("MACRO")
    if (hasFlag(TYPE)) append("TYPE")
    if (hasFlag(PARAM)) append("PARAM")
    if (hasFlag(SELFPARAM)) append("SELFPARAM")
    if (hasFlag(TYPEPARAM)) append("TYPEPARAM")
    if (hasFlag(OBJECT)) append("OBJECT")
    if (hasFlag(PACKAGE)) append("PACKAGE")
    if (hasFlag(PACKAGEOBJECT)) append("PACKAGEOBJECT")
    if (hasFlag(CLASS)) append("CLASS")
    if (hasFlag(TRAIT)) append("TRAIT")
    if (hasFlag(INTERFACE)) append("INTERFACE")
    if (hasFlag(LOCAL)) append("LOCAL")
    if (hasFlag(FIELD)) append("FIELD")
    if (hasFlag(CTOR)) append("CTOR")
    buf.toString.toLowerCase
  }

  protected def flagStructure: String = {
    flagSyntax.replace(" ", " | ").toUpperCase
  }
}

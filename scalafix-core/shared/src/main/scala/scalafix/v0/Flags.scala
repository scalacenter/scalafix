package scalafix.v0

trait Flags {
  final val VAL: Long = 1L << 0
  final val VAR: Long = 1L << 1
  final val METHOD: Long = 1L << 2
  final val MACRO: Long = 1L << 5
  final val TYPE: Long = 1L << 6
  final val PARAM: Long = 1L << 7
  final val TYPEPARAM: Long = 1L << 8
  final val OBJECT: Long = 1L << 9
  final val PACKAGE: Long = 1L << 10
  final val PACKAGEOBJECT: Long = 1L << 11
  final val CLASS: Long = 1L << 12
  final val TRAIT: Long = 1L << 13
  final val PRIVATE: Long = 1L << 14
  final val PROTECTED: Long = 1L << 15
  final val ABSTRACT: Long = 1L << 16
  final val FINAL: Long = 1L << 17
  final val SEALED: Long = 1L << 18
  final val IMPLICIT: Long = 1L << 19
  final val LAZY: Long = 1L << 20
  final val CASE: Long = 1L << 21
  final val COVARIANT: Long = 1L << 22
  final val CONTRAVARIANT: Long = 1L << 23
  final val INLINE: Long = 1L << 24
  final val JAVADEFINED: Long = 1L << 25
  final val SELFPARAM: Long = 1L << 28
  final val INTERFACE: Long = 1L << 29
  final val LOCAL: Long = 1L << 30
  final val FIELD: Long = 1L << 31
  final val CTOR: Long = 1L << 32
  final val PRIMARY: Long = 1L << 33
  final val ENUM: Long = 1L << 34
  final val STATIC: Long = 1L << 35
}
object Flags extends Flags

package scalafix.v1

sealed abstract class Constant extends Product with Serializable
case object UnitConstant extends Constant
final case class BooleanConstant(value: Boolean) extends Constant
final case class ByteConstant(value: Byte) extends Constant
final case class ShortConstant(value: Short) extends Constant
final case class CharConstant(value: Char) extends Constant
final case class IntConstant(value: Int) extends Constant
final case class LongConstant(value: Long) extends Constant
final case class FloatConstant(value: Float) extends Constant
final case class DoubleConstant(value: Double) extends Constant
final case class StringConstant(value: String) extends Constant
case object NullConstant extends Constant

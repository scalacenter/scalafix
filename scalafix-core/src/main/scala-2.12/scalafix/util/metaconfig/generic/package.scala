package metaconfig

import scalafix.internal.util.MetaconfigCompatMacros

import scala.language.experimental.macros

// shadows https://github.com/scalameta/metaconfig/blob/v0.10.0/metaconfig-core/shared/src/main/scala-2/metaconfig/generic/package.scala
package object generic {
  def deriveSurface[T]: Surface[T] =
    macro MetaconfigCompatMacros.deriveSurfaceImpl[T]
  def deriveDecoder[T](default: T): ConfDecoder[T] =
    macro metaconfig.internal.Macros.deriveConfDecoderImpl[T]
  def deriveEncoder[T]: ConfEncoder[T] =
    macro metaconfig.internal.Macros.deriveConfEncoderImpl[T]
  def deriveCodec[T](default: T): ConfCodec[T] =
    macro metaconfig.internal.Macros.deriveConfCodecImpl[T]
  def deriveDecoderEx[T](default: T): ConfDecoderEx[T] =
    macro metaconfig.internal.Macros.deriveConfDecoderExImpl[T]
  def deriveCodecEx[T](default: T): ConfCodecEx[T] =
    macro metaconfig.internal.Macros.deriveConfCodecExImpl[T]
}

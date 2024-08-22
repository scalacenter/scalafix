package metaconfig.generic

import java.io.File
import java.nio.file.Path

import scala.annotation.StaticAnnotation
import scala.deriving.*
import scala.quoted.*

import metaconfig.*
import metaconfig.annotation.TabCompleteAsPath

inline def deriveEncoder[T]: metaconfig.ConfEncoder[T] =
  ${ deriveEncoderImpl[T] }

inline def deriveDecoder[T](inline default: T): metaconfig.ConfDecoder[T] =
  ${ deriveConfDecoderImpl[T]('default) }

inline def deriveDecoderEx[T](
    inline default: T
): metaconfig.ConfDecoderExT[T, T] =
  ${ deriveConfDecoderExImpl[T]('default) }

inline def deriveSurface[T]: Surface[T] =
  ${ deriveSurfaceImpl[T] }

inline def deriveCodec[T](inline default: T): metaconfig.ConfCodec[T] =
  ${ deriveCodecImpl[T]('default) }

private[generic] def deriveCodecImpl[T: Type](default: Expr[T])(using Quotes) =
  val encoder = deriveEncoderImpl[T]
  val decoder = deriveConfDecoderImpl[T](default)

  '{
    ConfCodec.EncoderDecoderToCodec[T](
      $encoder,
      $decoder
    )
  }

private[generic] def deriveEncoderImpl[T](using tp: Type[T])(using q: Quotes) =
  assumeCaseClass[T]
  Expr("Show: " + Type.show[T])
  val encoders = params[T]
  val fields = paramNames[T]
  Expr.summon[Mirror.Of[T]].get
  '{
    new metaconfig.ConfEncoder[T]:
      override def write(value: T): metaconfig.Conf =
        val prod = value.asInstanceOf[Product]
        new metaconfig.Conf.Obj(
          $fields.zip($encoders).zipWithIndex.map { case ((name, enc), idx) =>
            name -> enc
              .asInstanceOf[ConfEncoder[Any]]
              .write(prod.productElement(idx))
          }
        )
  }
end deriveEncoderImpl

private[generic] def deriveConfDecoderImpl[T: Type](default: Expr[T])(using
    q: Quotes
) =
  import q.reflect.*
  assumeCaseClass[T]

  Expr(Type.show[T])
  val clsTpt = TypeRepr.of[T]
  val settings =
    Expr.summon[Settings[T]] match
      case None =>
        report.error(s"Missing Implicit for Settings[${Type.show[T]}]"); ???
      case Some(v) => v

  val paramss = TypeRepr.of[T].classSymbol.get.primaryConstructor.paramSymss

  def next(p: ValDef): Expr[Conf => Configured[Any]] =
    p.tpt.tpe.asType match
      case '[t] =>
        val name = Expr(p.name)
        val getter = clsTpt.classSymbol.get.declaredField(p.name)
        val fallback = Select(default.asTerm, getter).asExprOf[t]
        val dec = Expr
          .summon[ConfDecoder[t]]
          .getOrElse {
            report.error(
              "Could not find an implicit decoder for type" +
                s"'${TypeTree.of[t].show}' for field ${p.name} of class ${clsTpt.show}"
            )
            ???
          }

        '{ conf =>
          conf.getSettingOrElse[t](
            $settings.unsafeGet($name),
            $fallback
          )(using $dec)
        }

  if paramss.head.isEmpty then '{ ConfDecoder.constant($default) }
  else
    val (head :: params) :: Nil = paramss: @unchecked
    val vds = paramss.head.map(_.tree).collect { case vd: ValDef =>
      next(vd)
    }

    val mir = Expr.summon[Mirror.ProductOf[T]].get

    val parameters = Expr.ofList(vds)
    val merged = '{ (conf: Conf) =>
      $parameters
        .map { f =>
          f(conf).map(Array.apply(_))
        }
        .reduceLeft((acc, nx) =>
          acc.product(nx).map { x =>
            x._1 ++ x._2
          }
        )
        .map(Tuple.fromArray)
        .map($mir.fromProduct)
    }

    '{
      new ConfDecoder[T]:
        def read(conf: Conf): Configured[T] = $merged(conf)
    }
  end if
end deriveConfDecoderImpl

private[generic] def deriveConfDecoderExImpl[T: Type](default: Expr[T])(using
    q: Quotes
) =
  import q.reflect.*
  assumeCaseClass[T]

  Expr(Type.show[T])
  val clsTpt = TypeRepr.of[T]
  val settings =
    Expr.summon[Settings[T]] match
      case None =>
        report.error(s"Missing Implicit for Settings[${Type.show[T]}]"); ???
      case Some(v) => v

  val paramss = TypeRepr.of[T].classSymbol.get.primaryConstructor.paramSymss

  def next(p: ValDef): Expr[(Conf, T) => Configured[Any]] =
    p.tpt.tpe.asType match
      case '[t] =>
        val name = Expr(p.name)
        val getter = clsTpt.classSymbol.get.declaredField(p.name)
        val fallback = (from: Expr[Any]) =>
          Select(from.asTerm, getter).asExprOf[t]
        val dec = Expr
          .summon[ConfDecoderEx[t]]
          .getOrElse {
            report.error(
              "Could not find an implicit decoder for type" +
                s"'${TypeTree.of[t].show}' for field ${p.name} of class ${clsTpt.show}"
            )
            ???
          }

        '{ (conf, from) =>
          Conf.getSettingEx[t](
            ${ fallback('{ from }) },
            conf,
            $settings.unsafeGet($name)
          )(using $dec)
        }

  if paramss.head.isEmpty then
    '{
      new ConfDecoderEx[T]:
        def read(state: Option[T], conf: Conf) =
          Configured.Ok(state.getOrElse($default))
    }
  else
    val (head :: params) :: Nil = paramss: @unchecked
    val vds = paramss.head.map(_.tree).collect { case vd: ValDef =>
      next(vd)
    }

    val mir = Expr.summon[Mirror.ProductOf[T]].get

    val parameters = Expr.ofList(vds)
    val merged = '{ (conf: Conf, from: T) =>
      $parameters
        .map { f =>
          f(conf, from).map(Array.apply(_))
        }
        .reduceLeft((acc, nx) =>
          acc.product(nx).map { x =>
            x._1 ++ x._2
          }
        )
        .map(Tuple.fromArray)
        .map($mir.fromProduct)
    }

    '{
      new ConfDecoderExT[T, T]:
        def read(state: Option[T], conf: Conf): Configured[T] =
          $merged(conf, state.getOrElse($default))
    }
  end if
end deriveConfDecoderExImpl

private[generic] def deriveSurfaceImpl[T: Type](using q: Quotes) =
  import q.reflect.*
  val target = TypeRepr.of[T] match
    case at: AppliedType =>
      at.tycon.asType match
        case '[t] => assumeCaseClass[t]; at.tycon
        case _ => report.error(at.tycon.show); ???
    case other => other

  val cls = target.classSymbol.get
  val argss = cls.primaryConstructor.paramSymss
    .filter(paramList => paramList.forall(!_.isTypeParam))
    .map { params =>
      val fields = params.flatMap { param =>
        param.tree match
          case vd: ValDef =>
            inline def derivesFrom[A: Type]: Boolean =
              vd.tpt.tpe.derivesFrom(TypeRepr.of[A].typeSymbol)

            val baseAnnots = param.annotations.collect {
              case annot
                  if annot.tpe.derivesFrom(
                    TypeRepr.of[StaticAnnotation].typeSymbol
                  ) =>
                annot.asExprOf[StaticAnnotation]
            }
            val isConf = derivesFrom[metaconfig.Conf]
            val isMap = derivesFrom[Map[?, ?]]
            val isIterable = derivesFrom[Iterable[?]]
            val repeated =
              if isIterable && !isMap then
                List('{ new metaconfig.annotation.Repeated })
              else Nil

            val dynamic =
              if isMap || isConf then
                List('{ new metaconfig.annotation.Dynamic })
              else Nil

            val flag =
              if vd.tpt.tpe.derivesFrom(TypeRepr.of[Boolean].typeSymbol) then
                List('{ new metaconfig.annotation.Flag })
              else Nil

            val tabCompletePath: List[Expr[StaticAnnotation]] =
              if derivesFrom[Path] || derivesFrom[File] then
                List('{ new metaconfig.annotation.TabCompleteAsPath })
              else Nil

            val finalAnnots =
              Expr.ofList(
                repeated ++ dynamic ++ flag ++ tabCompletePath ++ baseAnnots
              )

            val fieldType = vd.tpt.tpe

            val underlying: Expr[List[List[Field]]] = vd.tpt.tpe.asType match
              case '[t] =>
                Expr.summon[Surface[t]] match
                  case None => '{ Nil }
                  case Some(e) => '{ $e.fields }

            val fieldName = Expr(vd.name)
            val tpeString =
              fieldType.asType match
                case '[t] =>
                  val renderer = Expr.summon[metaconfig.pprint.TPrint[t]].get
                  '{ $renderer.render.render }

            val fieldExpr = '{
              new Field($fieldName, $tpeString, $finalAnnots, $underlying)
            }
            Some(fieldExpr)
          case _ => None
      }

      Expr.ofList(fields)
    }

  val args = Expr.ofList(argss)
  val classAnnotations = Expr.ofList {
    cls.annotations.collect {
      case annot
          if annot.tpe.derivesFrom(
            TypeRepr.of[StaticAnnotation].typeSymbol
          ) =>
        annot.asExprOf[StaticAnnotation]
    }
  }

  '{ new Surface[T]($args, $classAnnotations) }
end deriveSurfaceImpl

private[generic] def assumeCaseClass[T: Type](using q: Quotes) =
  import q.reflect.*
  val sym = TypeTree.of[T].symbol
  val isCaseClass = sym.isClassDef && sym.flags.is(Flags.Case)
  if !isCaseClass then report.error(s"${Type.show[T]} must be a case class")

private[generic] def params[T: Type](using q: Quotes) =
  import q.reflect.*
  val fields = TypeTree.of[T].symbol.caseFields
  val encoder = TypeRepr.of[ConfEncoder]
  Expr.ofList {
    fields.map { f =>
      val tp = TypeRepr.of[T].memberType(f)
      f.name

      Implicits.search(encoder.appliedTo(tp)) match
        case iss: ImplicitSearchSuccess =>
          iss.tree.asExpr
        case _: ImplicitSearchFailure =>
          report.error(
            s"can't find conversion for ${tp.show(using Printer.TypeReprCode)}"
          )
          '{ ??? }
    }
  }

end params

private[generic] def paramNames[T: Type](using q: Quotes) =
  import q.reflect.*
  val fields = TypeTree.of[T].symbol.caseFields.map(_.name)

  Expr(fields)

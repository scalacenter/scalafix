package scalafix.internal.util

import java.lang.reflect.InvocationTargetException
import scalafix.SemanticCtx
import scala.reflect.ClassTag
import scala.util.Success
import scala.util.Try
import scalafix.rule.Rule
import metaconfig.ConfError
import metaconfig.Configured

class ClassloadRewrite[T](classLoader: ClassLoader)(implicit ev: ClassTag[T]) {
  private val t = ev.runtimeClass

  private val functionClasstag =
    implicitly[ClassTag[Function[SemanticCtx, T]]].runtimeClass
  object LambdaRewrite {
    def unapply(fqcn: String): Option[(Class[_], String)] = {
      val idx = fqcn.lastIndexOf(".")
      if (idx == 1) None
      else {
        val (obj, field) = fqcn.splitAt(idx)
        getClassFor(obj + "$").map(x => x -> field.stripPrefix(".")).toOption
      }
    }
  }
  private def classloadLambdaRewrite(
      clazz: Class[_],
      args: Seq[AnyRef],
      fieldName: String): Try[T] = Try {
    val field = clazz.getDeclaredField(fieldName)
    val obj = {
      val constructor = clazz.getDeclaredConstructor()
      constructor.setAccessible(true)
      constructor.newInstance()
    }
    field.setAccessible(true)
    val rule = field.get(obj)
    if (t.isInstance(rule)) rule.asInstanceOf[T]
    else
      args match {
        case (sctx: SemanticCtx) :: Nil
            if functionClasstag.isInstance(rule) =>
          rule.asInstanceOf[Function[SemanticCtx, T]].apply(sctx)
        case _ =>
          throw new IllegalArgumentException(
            s"Unable to load rule from field $fieldName on object $obj with arguments $args")
      }
  }

  private def getClassFor(fqcn: String): Try[Class[_]] =
    Try { Class.forName(fqcn, false, classLoader) }

  private def classloadClassRewrite(
      clazz: Class[_],
      args: Seq[AnyRef]): Try[T] =
    Try {
      val argsLen = args.length
      val constructors =
        Try(clazz.getDeclaredConstructor()).toOption.toList ++
          clazz.getDeclaredConstructors.toList
      val constructor = constructors
        .find(_.getParameterCount == argsLen)
        .orElse(constructors.find(_.getParameterCount == 0))
        .getOrElse {
          val argsMsg =
            if (args.isEmpty) ""
            else s" or constructor matching arguments $args"
          throw new IllegalArgumentException(
            s"""No suitable constructor on $clazz.
               |Expected : zero argument constructor $argsMsg
               |Found    : $constructors
             """.stripMargin)
        }
      constructor.setAccessible(true)
      val obj = {
        if (constructor.getParameterCount == argsLen)
          constructor.newInstance(args: _*)
        else constructor.newInstance()
      }
      if (t.isInstance(obj)) obj.asInstanceOf[T]
      else {
        throw new ClassCastException(s"${clazz.getName} is not a subtype of $t")
      }
    } recover {
      case i: InvocationTargetException if i.getTargetException ne null =>
        throw i.getTargetException
    }

  def classloadRewrite(fqcn: String, args: Class[_] => Seq[AnyRef]): Try[T] = {
    val combined = List.newBuilder[Try[T]]
    combined += getClassFor(fqcn).flatMap(cls =>
      classloadClassRewrite(cls, args(cls)))
    if (!fqcn.endsWith("$")) {
      combined += getClassFor(fqcn + "$").flatMap(cls =>
        classloadClassRewrite(cls, args(cls)))
    }
    fqcn match {
      case LambdaRewrite(cls, field) =>
        combined += classloadLambdaRewrite(cls, args(cls), field)
      case _ =>
    }
    val result = combined.result()
    val successes = result.collect { case Success(t) => t }
    val failures = result.collect { case util.Failure(e) => e }
    def pretty(ex: Throwable): String =
      s"""$ex
         | ${ex.getStackTrace.take(10).mkString(" \n")}""".stripMargin
    if (successes.nonEmpty) Success(successes.head)
    else {
      util.Failure(
        new IllegalArgumentException(
          s"""Unable to load rule $fqcn with args $args. Tried the following:
             |${failures.map(pretty).mkString("\n")}""".stripMargin))
    }
  }
}

object ClassloadRewrite {
  lazy val defaultClassloader = getClass.getClassLoader
  def apply(
      fqn: String,
      args: Class[_] => Seq[AnyRef],
      classloader: ClassLoader = defaultClassloader): Configured[Rule] = {
    val result =
      new ClassloadRewrite[Rule](classloader).classloadRewrite(fqn, args)
    result match {
      case Success(e) => Configured.Ok(e)
      case util.Failure(e) => Configured.NotOk(ConfError.msg(e.toString))
    }
  }
}

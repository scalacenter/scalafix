package scalafix.internal.util

import java.lang.reflect.InvocationTargetException
import scalafix.v0.SemanticdbIndex
import scala.reflect.ClassTag
import scala.util.Success
import scala.util.Try
import scalafix.v0.Rule
import metaconfig.ConfError
import metaconfig.Configured

class ClassloadRule[T](classLoader: ClassLoader)(implicit ev: ClassTag[T]) {
  private val t = ev.runtimeClass

  private val functionClasstag =
    implicitly[ClassTag[Function[SemanticdbIndex, T]]].runtimeClass
  object LambdaRule {
    def unapply(fqcn: String): Option[(Class[_], String)] = {
      val idx = fqcn.lastIndexOf(".")
      if (idx == 1) None
      else {
        val (obj, field) = fqcn.splitAt(idx)
        getClassFor(obj + "$").map(x => x -> field.stripPrefix(".")).toOption
      }
    }
  }
  private def classloadLambdaRule(
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
        case (index: SemanticdbIndex) :: Nil
            if functionClasstag.isInstance(rule) =>
          rule.asInstanceOf[Function[SemanticdbIndex, T]].apply(index)
        case _ =>
          throw new IllegalArgumentException(
            s"Unable to load rule from field $fieldName on object $obj with arguments $args")
      }
  }

  private def getClassFor(fqcn: String): Try[Class[_]] =
    Try { Class.forName(fqcn, false, classLoader) }

  private def classloadClassRule(clazz: Class[_], args: Seq[AnyRef]): Try[T] =
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

  def classloadRule(fqcn: String, args: Class[_] => Seq[AnyRef]): Try[T] = {
    val combined = List.newBuilder[Try[T]]
    combined += getClassFor(fqcn).flatMap(cls =>
      classloadClassRule(cls, args(cls)))
    if (!fqcn.endsWith("$")) {
      combined += getClassFor(fqcn + "$").flatMap(cls =>
        classloadClassRule(cls, args(cls)))
    }
    fqcn match {
      case LambdaRule(cls, field) =>
        combined += classloadLambdaRule(cls, args(cls), field)
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

object ClassloadRule {
  def defaultClassloader: ClassLoader = getClass.getClassLoader
  def apply(
      fqn: String,
      args: Class[_] => Seq[AnyRef],
      classloader: ClassLoader = defaultClassloader): Configured[Rule] = {
    val result =
      new ClassloadRule[Rule](classloader).classloadRule(fqn, args)
    result match {
      case Success(e) => Configured.Ok(e)
      case util.Failure(e) => Configured.NotOk(ConfError.message(e.toString))
    }
  }
}

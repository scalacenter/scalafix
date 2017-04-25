package scalafix
package util

import scala.meta._
import scala.reflect.ClassTag
import scala.util.Failure
import scala.util.Success
import scala.util.Try

import java.lang.reflect.InvocationTargetException

import metaconfig.ConfError
import metaconfig.Configured
import org.scalameta.logger

class ClassloadRewrite[T](classLoader: ClassLoader)(implicit ev: ClassTag[T]) {
  private val t = ev.runtimeClass
  private val functionClasstag =
    implicitly[ClassTag[Function[Mirror, T]]].runtimeClass

  object LambdaRewrite {
    def unapply(fqcn: String): Option[(Class[_], String)] = {
      val idx = fqcn.lastIndexOf(".")
      if (idx == -1) None
      else {
        val (obj, field) = fqcn.splitAt(idx)
        getClassFor(obj + "$").map(x => x -> field.stripPrefix(".")).toOption
      }
    }
  }

  private def getClassFor(fqcn: String): Try[Class[_]] =
    Try { Class.forName(fqcn, false, classLoader) }

  private def classloadLambdaRewrite(clazz: Class[_],
                                     args: Seq[AnyRef],
                                     fieldName: String): Try[T] = Try {
    val field = clazz.getDeclaredField(fieldName)
    val obj = {
      val constructor = clazz.getDeclaredConstructor()
      constructor.setAccessible(true)
      constructor.newInstance()
    }
    field.setAccessible(true)
    val rewrite = field.get(obj)
    if (t.isInstance(rewrite)) rewrite.asInstanceOf[T]
    else
      args match {
        case (mirror: Mirror) :: Nil if functionClasstag.isInstance(rewrite) =>
          rewrite.asInstanceOf[Function[Mirror, T]].apply(mirror)
        case _ =>
          throw new IllegalArgumentException(
            s"Unable to load rewrite from field $fieldName on object $obj with arguments $args")
      }
  }

  private def classloadClassRewrite(clazz: Class[_],
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
               |Expected : zero-argument constructor$argsMsg
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
        throw new ClassCastException(
          s"${clazz.getName} is not a subtype of $t")
      }
    } recover {
      case i: InvocationTargetException if i.getTargetException ne null =>
        throw i.getTargetException
    }

  def classloadRewrite(fqcn: String, args: Seq[AnyRef]): Try[T] = {
    val combined = List.newBuilder[Try[T]]
    combined += getClassFor(fqcn).flatMap(cls =>
      classloadClassRewrite(cls, args))
    if (!fqcn.endsWith("$")) {
      combined += getClassFor(fqcn + "$").flatMap(cls =>
        classloadClassRewrite(cls, args))
    }

    val lambdaRewrite = fqcn match {
      case LambdaRewrite(cls, field) =>
        combined += classloadLambdaRewrite(cls, args, field)
      case _ => Nil
    }
    val result = combined.result()
    val successes = result.collect { case Success(t) => t }
    val failures = result.collect { case Failure(e) => e }
    def pretty(ex: Throwable): String =
      s"""$ex
         | ${ex.getStackTrace.take(10).mkString(" \n")}""".stripMargin
    if (successes.nonEmpty) Success(successes.head)
    else {
      Failure(new IllegalArgumentException(
        s"""Unable to load rewrite $fqcn with args $args. Tried the following:
           |${failures.map(pretty).mkString("\n")}""".stripMargin))
    }
  }
}

object ClassloadRewrite {
  lazy val defaultClassloader = getClass.getClassLoader
  def apply(
      fqn: String,
      args: Seq[AnyRef],
      classloader: ClassLoader = defaultClassloader): Configured[Rewrite] = {
    val result =
      new ClassloadRewrite[Rewrite](classloader).classloadRewrite(fqn, args)
    result match {
      case Success(e) => Configured.Ok(e)
      case Failure(e) => Configured.NotOk(ConfError.msg(e.toString))
    }
  }
}

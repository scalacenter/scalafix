package scalafix.util

import scala.collection.immutable.Seq
import scala.reflect.ClassTag
import scala.util.Failure
import scala.util.Success
import scala.util.Try

import java.lang.reflect.InvocationTargetException

// Helper to classload object or no argument class.
class ClassloadObject[T](classLoader: ClassLoader)(implicit ev: ClassTag[T]) {
  private val t = ev.runtimeClass

  private def getClassFor(fqcn: String): Try[Class[_ <: T]] =
    Try[Class[_ <: T]]({
      val c =
        Class.forName(fqcn, false, classLoader).asInstanceOf[Class[_ <: T]]
      if (t.isAssignableFrom(c)) c
      else throw new ClassCastException(s"$t is not assignable from $c")
    })

  private def createInstanceFor(clazz: Class[_]): Try[T] =
    Try {
      val constructor = clazz.getDeclaredConstructor()
      constructor.setAccessible(true)
      val obj = constructor.newInstance()
      if (t.isInstance(obj)) obj.asInstanceOf[T]
      else
        throw new ClassCastException(
          s"${clazz.getName} is not a subtype of $t")
    } recover {
      case i: InvocationTargetException if i.getTargetException ne null ⇒
        throw i.getTargetException
    }

  def createInstanceFor(fqcn: String): Try[T] =
    getClassFor(fqcn).flatMap(c => createInstanceFor(c))
}

object ClassloadObject {
  def apply[T: ClassTag](fqn: String): Either[Throwable, T] =
    new ClassloadObject(this.getClass.getClassLoader)
      .createInstanceFor(fqn) match {
      case Success(e) => Right(e)
      case Failure(e) => Left(e)
    }
}

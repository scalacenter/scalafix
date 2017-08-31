package com.geirsson

class Future[T]
object Future {
  def empty[T]: Future[T] = ???
  def successful[T](e: T): Future[T] = ???
}

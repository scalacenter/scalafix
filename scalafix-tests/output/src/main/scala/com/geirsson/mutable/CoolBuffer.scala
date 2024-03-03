package com.geirsson.mutable

// dummy code to make symbol patches compile

class CoolBuffer[T]
object CoolBuffer {
  def empty[T] = new CoolBuffer[T]
}

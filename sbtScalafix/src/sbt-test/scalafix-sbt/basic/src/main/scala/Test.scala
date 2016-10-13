package main

object Main {
  def main(args: String) {
    println("hello")
  }
}

class Y

class X {
  implicit val y = new Y
}
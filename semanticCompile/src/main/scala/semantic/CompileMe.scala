package semantic

class Goodbye

class CompileMe() {
  implicit val x = 1
  implicit val goodbye = new Goodbye
}

object CompileMe {
  implicit val y = "string"
  implicit def method = new CompileMe
}


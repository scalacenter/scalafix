package test.redundantSyntax

class StringInterpolator {

  val a = 42d

  var b = ""
  b = "foo"
  b = "foo"
  b = s"foo $a bar"

  b = """foo"""
  b =
    """foo
       |bar"""
  b = s"""foo $a bar"""
  b = s"""$a"""

  b = "foo"
  b = f"foo $a%2.2f"

  b = raw"foo $a \nbar"
  b = raw"foo\nbar\\"
  b = "foo bar"

  b = my"foo"
  b = my"foo $a bar"

  implicit class MyInterpolator(sc: StringContext) {
    def my(subs: Any*): String = sc.toString + subs.mkString("")
  }
}
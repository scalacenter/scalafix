package test.redundantSyntax

class StringInterpolator {

  val a = 42d

  var b = ""
  b = "foo"
  b = "foo"
  b = s"foo $a bar"
  b = "my \"quoted\" string"

  b = """foo"""
  b =
    """foo
       |bar"""
  b = s"""my \"quoted\" string"""
  b = s"""foo $a bar"""
  b = s"""$a"""

  b = "foo"
  b = f"foo $a%2.2f"
  b = "foo \n bar"

  b = raw"foo $a \nbar"
  b = """foo\nbar\\"""
  b = raw"foo\nbar\\"
  b = "foo bar"
  b = raw"a\*b\+"

  b = my"foo"
  b = my"foo $a bar"

  b = s"foo" // scalafix:ok

  implicit class MyInterpolator(sc: StringContext) {
    def my(subs: Any*): String = sc.toString + subs.mkString("")
  }
}

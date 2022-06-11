/*
rules = RedundantSyntax
RedundantSyntax.stringInterpolator = true
*/

package test.redundantSyntax

class StringInterpolator {

  val a = 42d

  var b = ""
  b = "foo"
  b = s"foo"
  b = s"foo $a bar"

  b = """foo"""
  b =
    s"""foo
       |bar"""
  b = s"""foo $a bar"""
  b = s"""$a"""

  b = f"foo"
  b = f"foo $a%2.2f"

  b = raw"foo $a \nbar"
  b = raw"foo\nbar\\"
  b = raw"foo bar"
  b = raw"a\*b\+"

  b = my"foo"
  b = my"foo $a bar"

  implicit class MyInterpolator(sc: StringContext) {
    def my(subs: Any*): String = sc.toString + subs.mkString("")
  }
}

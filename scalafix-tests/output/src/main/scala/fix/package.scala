
package object fix {
  implicit class XtensionList[T](lst: List[T]) {
    def unsafeMkString = lst.mkString
    def unsafeMkString(sep: String) = lst.mkString(sep)
  }
  implicit class XtensionString[T](str: String) {
    def substringFrom(beginIndex: Int): String = str.substring(beginIndex)
    def substringBetween(beginIndex: Int, endIndex: Int): String = str.substring(beginIndex, endIndex)
  }

}

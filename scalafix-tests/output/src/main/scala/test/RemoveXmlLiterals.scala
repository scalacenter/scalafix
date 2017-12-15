package test

import scala.xml.quote._
class RemoveXmlLiterals {
  object A {
    xml"<div></div>"
  }

  object B {
    val a = xml"""<div b="Hello"/>"""
    val b = xml"""<a>\</a>"""
  }

  object C {
    val bar = "bar"
    xml"<div>${bar}</div>"
  }

  object D {
    val foo =
      xml"""<div>
        <span>Hello</span>
      </div>"""
  }

  object E {
    val foo =
      xml"""<div>
        <span>${"Hello"}</span>
      </div>"""
  }

  object F {
    xml"<foo bar=${"Hello"}/>"
  }

  object G {
    xml"<a>${xml"<a>${"Hello"}</a>"}</a>"
  }

  object H {
    xml"<div>$$</div>"
  }

  object I {
    val a = xml"<div>{</div>"
    val b = xml"<div>}</div>"
    // <div b="{{"/> >>> xml"""<div b="{{"/>"""
  }

  object J {
    xml"<a>${1}${2}</a>"
  }

  object K {
    null match { case <a></a> => }
  }

  object L {
    null match { case <a>{_*}</a> => }
  }

  <div></div> // scalafix:ok RemoveXmlLiterals
}


---
layout: docs
id: LeakingImplicitClassVal
title: LeakingImplicitClassVal
---

Non-private `val` fields of implicit classes leak as publicly accessible
extension methods. This rule adds the `private` access modifier on the field of
implicit value classes in order to prevent direct access.

```scala
// before
implicit class XtensionVal(val str: String) extends AnyVal {
  def doubled: String = str + str
}
"message".str // compiles

// after
implicit class XtensionValFixed(private val str: String) extends AnyVal {
  def doubled: String = str + str
}
"message".str // does not compile
```

Note: This rule only triggers for `val` fields, it ignores other modifiers such
as `var`.

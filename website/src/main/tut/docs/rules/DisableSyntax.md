---
layout: docs
title: Disable
---

# DisableSyntax

_Since 0.5.4_

This rule reports errors when a "disallowed" syntax is used.

Example:

```scala
MyCode.scala:7: error: [DisableSyntax.xml] xml is disabled.
  <a>xml</a>
  ^
```

## Configuration

By default, this rule does allows all syntax.

It contains the following elements:

* keywords such as: `null, throw, var, return`
* carriage return aka <CR> (windows uses <CR><LF>)
* semicolons (`;`)
* tabs
* xml literals

To disallow a syntax:

```
DisableSyntax {
  keywords = [
    abstract
    case
    catch
    class
    def
    do
    else
    enum
    extends
    false
    final
    finally
    for
    forSome
    if
    implicit
    import
    lazy
    match
    macro
    new
    null
    object
    override
    package
    private
    protected
    return
    sealed
    super
    this
    throw
    trait
    true
    try
    type
    val
    var
    while
    with
    yield
  ]
  carriageReturn = true
  semicolons = true
  tabs = true
  xml = true
}
```
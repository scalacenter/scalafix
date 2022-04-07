---
layout: docs
id: RedundantSyntax
title: RedundantSyntax
---

This rule removes redundant syntax. Currently the only syntax it removes is the `final` keyword on an `object`.

Example:

```diff
- final object foo
+ object Foo
```

Note: in Scala 2.12 and earlier removing the `final` modifier will slightly change the resulting bytecode - see [this bug ticket](https://github.com/scala/bug/issues/11094) for further information.
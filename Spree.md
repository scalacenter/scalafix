# Write a scalafix rule for Scala 3 

## What's possible ?
- Refactoring
```diff
- def myComplexMethod = 1.to(10).map(i => i -> i.toString).toMap
+ def myComplexMethod: Map[Int, String] = 1.to(10).map(i => i -> i.toString).toMap
```
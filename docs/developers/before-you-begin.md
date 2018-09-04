---
id: before-you-begin
title: Before you write code
---

Before you dive right into the implementation details of your rule, it's good to
ask yourself the following questions first.

## Do you want a linter or rewrite?

- Rewrites can automatically fix problems, which is ideal but means rewrites can
  only address problems that have obvious solutions.

- Linters highlight problematic code without providing a fix, which means they
  demand more interaction from the user but can at the same time target a larger
  problem space than rewrites.

## Is your rule syntactic or semantic?

- Syntactic rules are simple to run since they don't require compilation but
  syntactic rules are limited since they don't have access to symbols and types.
- Semantic rules are are slower and more complicated to run since they require
  compilation but at the same time semantic rules are able to perform more
  advanced analysis since they have access to symbols and types.

## What is an acceptable false-positive rate?

- If you are doing a one-off migration script for your company codebase then it
  might be OK to manually fix trickier corner cases.
- If your linter catches critical production bugs it's maybe OK that it has a
  few false positives. If your linter catches low-importance bugs like enforcing
  a code style then false positives are not acceptable.

## What diff does the rewrite produce?

Before implementing a rule, it's good to manually migrate/refactor a few
examples first. Manual refactoring is great to discover corner and estimate how
complicated the rule needs to be.

## Who will use your rule?

The target audience/users of your rule can impact the implementation the rule.
If you are the only end-user of the rule, then you can maybe take shortcuts and
worry less about rare corner cases that may be easier to fix manually. If your
rule is intended to be used by the entire Scala community, then you might want
to be more careful with corner cases.

## What codebase does your rule target?

Is your rule specific to a particular codebase or is the rule intended to be
used for any arbitrary project? It's easier to validate your a rule if it will
only run on a single codebase. You may not even need tests, since the codebase
is the only test. If your rule is intended to be used in any random codebase,
you need to put more effort into handling corner cases. In general, the smaller
the target domain of your rule, the easier it is to implement a rule.

## How often will your rule run?

Are you writing a one-off migration script or will your rule run on every pull
request? A rule that runs on every pull request should ideally have some unit
tests and be documented so that other people can help maintain the rule.

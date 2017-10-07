---
layout: docs
title: Sharing your rule
---

# Sharing your rule
You have implemented a rule, you have tests, it works,
and now you want to share it with the world. Congrats!

There are several ways to share a rule if the rule is contained in a single file and uses no external dependencies:

- If you used the [Giter8 template]({{ site.baseurl }}{% link docs/creating-your-own-rule/giter-template.md %}) to build your project, push your rule to github and tell users to run `scalafix github:$org/$reponame/$version`.

- otherwise, tell users to use the @sect.ref{http:} protocol, `scalafix --rules https://gist....` where the url points to the plaintext contents of your rule.

- If your rule uses a custom library, then it's a bit tricky to share it. See [#201](https://github.com/{{ site.githubOwner }}/{{ site.githubRepo }}/issues/201) for more updates.


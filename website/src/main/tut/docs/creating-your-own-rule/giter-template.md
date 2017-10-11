---
layout: docs
title: Giter8 template
---

# Giter8 template

Run the following commands to generate a skeleton project:

```sh
cd reponame # The project you want to implement rules for.

# --rule= should ideally match the GitHub repo name, to make
# it possible for users to run `scalafix "github:org/reponame/v1.0"`
sbt new scalacenter/scalafix.g8 --rule="reponame" --version="v1.0"
cd scalafix
sbt tests/test
```

Note that the `scalafix` directory is a self-contained sbt build and can be put into the root directory of your repo. The tests are written using [scalafix-testkit]({{ site.baseurl }}{% link docs/creating-your-own-rule/scalafix-testkit.md %}).



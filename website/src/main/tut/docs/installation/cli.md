---
layout: docs
title: scalafix-cli
---

## scalafix-cli

The recommended way to install the scalafix command-line interface is with
[coursier](https://github.com/coursier/coursier#command-line).


```sh
// coursier
coursier bootstrap ch.epfl.scala:scalafix-cli_@{{ site.scalaVersion }}:{{ site.stableVersion }} -f --main scalafix.cli.Cli -o scalafix
./scalafix --help
```

```sh
// homebrew
brew install --HEAD olafurpg/scalafmt/scalafix
scalafix --help
```

```sh
// wget
wget -O scalafix https://github.com/scalacenter/scalafix/blob/master/scalafix?raw=true
./scalafix --help
```

Once the scalafix cli is installed, consult the --help page for further usage instructions.

> TODO(gabro): automatically include help message?

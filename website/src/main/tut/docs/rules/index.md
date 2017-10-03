---
layout: docs
title: Rules
---

# Rules

Scalafix comes with a few rules out-of-the-box.
These rules have been chosen to meet the long-term goal of scalafix to
[clarify the Scala to Dotty migration path](http://scala-lang.org/blog/2016/05/30/scala-center-advisory-board.html#the-first-meeting).

Here's a list of the currently implemented rules:

{% assign rules = site.data.menu.options | where:"title","Rules" %}
{% for rule in rules[0].nested_options %}
  - [{{ rule.title }}]({{ rule.title }}.html)
{% endfor %}

To create custom rules, see @sect.ref{scalafix-core}.

## Planned rules
See [here](https://github.com/scalacenter/scalafix/labels/rule).

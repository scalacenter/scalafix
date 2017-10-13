---
layout: docs
title: Rules
---

# Rules

A Scalafix rule is a small program that can analyze your source code,
report messages and automatically fix problems.
Scalafix comes with a few rules out-of-the-box.
These rules have been chosen to meet the long-term goal of scalafix to
[clarify the Scala to Dotty migration path](http://scala-lang.org/blog/2016/05/30/scala-center-advisory-board.html#the-first-meeting).

Here's a list of the currently implemented rules:

{% assign rules = site.data.rules.rules %}
{% for rule in rules %}
  - [{{ rule }}]({{ site.baseurl }}/docs/rules/{{ rule }})
{% endfor %}

To create custom rules, see {% doc_ref Rule Authors %}.

## Planned rules
See [here](https://github.com/scalacenter/scalafix/labels/rule).

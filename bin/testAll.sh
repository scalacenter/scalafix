#!/bin/bash
set -e
sbt -Dsbt.ivy.home=/drone/cache/ivy2 clean test publishLocal scripted

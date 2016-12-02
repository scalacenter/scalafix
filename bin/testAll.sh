#!/bin/bash
set -e
sbt -Dsbt.home.ivy=/drone/cache/ivy2 clean test publishLocal scripted

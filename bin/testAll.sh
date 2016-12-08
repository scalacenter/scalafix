#!/bin/bash
set -e
export SBT_OPTS="-Xmx24G -XX:MaxPermSize=4G -Xss4M"
sbt clean test scripted

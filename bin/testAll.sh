#!/bin/bash
set -e
sbt clean test publishLocal scripted

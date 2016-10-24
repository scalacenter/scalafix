#!/usr/bin/env bash
set -e
apt-get install nailgun
alias ng=ng-nailgun
export PATH=$PATH:/root/local/bin
pip install --user codecov

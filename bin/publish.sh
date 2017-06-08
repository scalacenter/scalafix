#!/usr/bin/env bash
set -eu
PUBLISH=${CI_PUBLISH:-false}

if [[ "$TRAVIS_BRANCH" == "master" && "$PUBLISH" == "true" ]]; then
  openssl aes-256-cbc -K $encrypted_468467362b53_key -iv $encrypted_468467362b53_iv -in .travis/github_deploy_key.enc -out .travis/github_deploy_key -d
  $(npm bin)/set-up-ssh --key "$encrypted_468467362b53_key" --iv "$encrypted_468467362b53_iv"  --path-encrypted-key ".travis/github_deploy_key.enc"
  echo "Running publish from $(pwd)"
  git log | head -n 20
  mkdir -p $HOME/.bintray
  cat > $HOME/.bintray/.credentials <<EOF
realm = Bintray API Realm
host = api.bintray.com
user = ${BINTRAY_USERNAME}
password = ${BINTRAY_API_KEY}
EOF
  sbt ci-publish
else
  echo "Skipping publish, branch=$TRAVIS_BRANCH publish=$PUBLISH test=$CI_TEST"
fi



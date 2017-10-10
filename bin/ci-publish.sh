#!/usr/bin/env bash
set -eu

set-up-ssh() {
  echo "Setting up ssh..."
  mkdir -p $HOME/.ssh
  ssh-keyscan -t rsa github.com >> ~/.ssh/known_hosts
  git config --global user.email "olafurpg@gmail.com"
  git config --global user.name "Scalafix Bot"
  git config --global push.default simple
  DEPLOY_KEY_FILE=$HOME/.ssh/id_rsa
  echo "$GITHUB_DEPLOY_KEY" | base64 --decode > ${DEPLOY_KEY_FILE}
  chmod 600 ${DEPLOY_KEY_FILE}
  eval "$(ssh-agent -s)"
  ssh-add ${DEPLOY_KEY_FILE}
}

if [[ "$TRAVIS_SECURE_ENV_VARS" == true && "$CI_PUBLISH" == true ]]; then
  echo "Publishing..."
  git log | head -n 20
  if [ -n "$TRAVIS_TAG" ]; then
    echo "$PGP_SECRET" | base64 --decode | gpg --import
    echo "Tag push, publishing release to Sonatype."
    sbt "sonatypeOpen scalafix-$TRAVIS_TAG" "^ very publishSigned" sonatypeReleaseAll
  fi
  set-up-ssh
  # FIXME(gabro): this is temporarily disabled while we finalize the new website
  # sbt website/publishMicrosite
else
  echo "Skipping publish, branch=$TRAVIS_BRANCH"
fi

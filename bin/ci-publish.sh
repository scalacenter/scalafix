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

set-up-jekyll() {
  rvm use 2.2.3 --install --fuzzy
  gem update --system
  gem install sass
  gem install jekyll -v 3.2.1
  export PATH=${PATH}:./vendor/bundle
}

if [[ "$TRAVIS_SECURE_ENV_VARS" == true && "$CI_PUBLISH" == true ]]; then
  echo "Publishing..."
  git log | head -n 20
  if [ -n "$TRAVIS_TAG" ]; then
    echo "$PGP_SECRET" | base64 --decode | gpg --import
    echo "Tag push, publishing release to Sonatype."
    sbt ci-release sonatypeReleaseAll
  fi
  set-up-ssh
  set-up-jekyll
  sbt website/publishMicrosite
else
  echo "Skipping publish, branch=$TRAVIS_BRANCH"
fi

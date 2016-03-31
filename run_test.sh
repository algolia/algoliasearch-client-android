#!/bin/bash

set -e
set -o pipefail

FILE=algoliasearch/src/test/java/com/algolia/search/saas/Helpers.java
export FILE

if ! [[ $TRAVIS_JOB_NUMBER && ${TRAVIS_JOB_NUMBER-_} ]]; then
    echo "/!\ TRAVIS_JOB_NUMBER is not set."
    TRAVIS_JOB_NUMBER=$RANDOM.$RANDOM
fi

echo "Running Android test..."
./setup_tests.sh
./gradlew testOnlineRelease
./teardown_tests.sh

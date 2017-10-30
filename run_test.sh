#!/bin/bash

set -e
set -o pipefail

my_travis_retry() {
    local result=0
    local count=1
    while [ $count -le 3 ]; do
        [ $result -ne 0 ] && {
            echo -e "\n${ANSI_RED}The command \"$@\" failed. Retrying, $count of 3.${ANSI_RESET}\n" >&2
        }
        "$@"
        result=$?
        [ $result -eq 0 ] && break
        count=$(($count + 1))
        sleep 1
    done
    [ $count -gt 3 ] && {
        echo -e "\n${ANSI_RED}The command \"$@\" failed 3 times.${ANSI_RESET}\n" >&2
        }
    return $result
}

if [ "$TRAVIS" = "true" ]; then # If on travis, use travis_retry
    if [ -n "$(type -t travis_retry)" ] && [ "$(type -t travis_retry)" = function ]; then
        RETRY="travis_retry" 
    else
        # travis_retry is undefined in subshells. the recommended way to handle this
        # is to "include the function code directly", see following conversation
        # Conversation:  https://twitter.com/plexus/status/499194992632811520
        # Source: https://git.io/vV5md
        RETRY="my_travis_retry"
    fi
fi

if ! [[ $TRAVIS_JOB_NUMBER && ${TRAVIS_JOB_NUMBER-_} ]]; then
    echo "/!\ TRAVIS_JOB_NUMBER is not set."
    TRAVIS_JOB_NUMBER=$RANDOM.$RANDOM
fi

echo "Running Android test..."
$RETRY ./gradlew testRelease

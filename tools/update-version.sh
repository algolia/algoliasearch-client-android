#!/usr/bin/env ksh
# ============================================================================ #
# UPDATE VERSION                                                               #
# ============================================================================ #
# SUMMARY
# -------
# Updates the version number in the sources.
# ============================================================================ #

set -eo pipefail

SELF_ROOT=$(cd $(dirname "$0") && pwd)
SELF_NAME=$(basename "$0")

function call_sed
{
    PATTERN="$1"
    FILENAME="$2"

    # Mac needs a space between sed's inplace flag and extension
    if [ "$(uname)" == "Darwin" ]; then
        sed -E -i '' "$PATTERN" "$FILENAME"
    else
        sed -E -i "$PATTERN" "$FILENAME"
    fi
}

function usage
{
    cat <<#EOF
    Usage:
        $SELF_NAME <version_no>
    EOF
}

PROJECT_ROOT=`cd "$SELF_ROOT/.." && pwd`
FILE_BUILD_GRADLE="$PROJECT_ROOT/algoliasearch/common.gradle"
FILE_API_CLIENT="$PROJECT_ROOT/algoliasearch/src/main/java/com/algolia/search/saas/AbstractClient.java"

if [ $# -ne 1 ]; then
    usage 1>&2
    exit -1
fi

VERSION_CODE=$1

echo "Updating version number to $VERSION_CODE..."
call_sed "s/(PUBLISH_VERSION =) '.*'/\1 '$VERSION_CODE'/" $FILE_BUILD_GRADLE
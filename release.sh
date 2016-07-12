#!/usr/bin/env bash

set -eo pipefail

SELF_ROOT=$(cd $(dirname "$0") && pwd)

function call_sed(){
PATTERN="$1"
FILENAME="$2"

# Mac needs a space between sed's inplace flag and extension
if [ "$(uname)" == "Darwin" ]; then
    sed -E -i '' "$PATTERN" "$FILENAME"
else
    sed -E -i "$PATTERN" "$FILENAME"
fi
}

FILE_BUILD_GRADLE="$SELF_ROOT/algoliasearch/common.gradle"
FILE_API_CLIENT="$SELF_ROOT/algoliasearch/src/main/java/com/algolia/search/saas/Client.java"

if [ $# -ne 1 ]; then
    echo "$0 | A script to release new versions automatically"
    echo "Usage: $0 VERSION_CODE"
    exit -1
fi

VERSION_CODE=$1

# Check that the working repository is clean (without any changes, neither staged nor unstaged).
# An exception is the change log, which should have been edited, but not necessarily committed (we usually commit it
# along with the version number).
if [[ ! -z `git status --porcelain | grep -v "ChangeLog.md"` ]]; then
    echo "ERROR: Working copy not clean! Aborting." 1>&2
    echo "Please revert or commit any pending changes before releasing." 1>&2
    exit 1
fi

# Check that the change log contains information for the new version.
set +e
version_in_change_log=$(cat "$SELF_ROOT/ChangeLog.md" | grep -E "^#+" | sed -E 's/^#* ([0-9.]*)\s*.*$/\1/g' | grep -x "$VERSION_CODE")
set -e
if [[ -z $version_in_change_log ]]; then
    echo "Version $VERSION_CODE not found in change log! Aborting." 1>&2
    exit 2
fi

$SELF_ROOT/tools/update-version.sh $VERSION_CODE

for flavor in online offline
do
    echo "==================== Processing flavor '$flavor' ===================="
    $SELF_ROOT/select-flavor.sh $flavor
    $SELF_ROOT/gradlew clean

    # Test publication locally.
    echo "-------------------- Publishing locally --------------------"
    $SELF_ROOT/gradlew testUploadArchives
    if [[ $flavor = "online" ]]; then
        module_name="algoliasearch-android"
    elif [[ $flavor = "offline" ]]; then
        module_name="algoliasearch-offline-android"
    fi
    # Dump the contents that has been published, just for the sake of manual checking.
    $SELF_ROOT/tools/dump-local-mvnrep.sh $module_name
    echo "-------------------- Testing publication --------------------"
    $SELF_ROOT/tools/test-publication.sh $module_name $VERSION_CODE

    # Perform the actual publication.
    echo "-------------------- Publishing remotely --------------------"
    $SELF_ROOT/gradlew uploadArchives
done

# Revert flavor to original.
git checkout $SELF_ROOT/algoliasearch/build.gradle

# Commit to Git... but do *not* push (see below).
git add .
git commit -m "Version $VERSION_CODE"
git tag $VERSION_CODE

echo "SUCCESS!"

# NOTE: We don't automatically publish nor Git push. Why?
# - We cannot be sure what the state of the staging repository was at the beginning of the script. So publishing
#   without controlling it manually is risky.
# - We don't want to push to Git before the release is actually available on Maven Central, which can take a few hours
#   after the staging repository has been promoted.
#
cat <<EOF
Next steps:
- Check the staging repository on Sonatype.
- If everything is OK: close, release and drop the staging repository.
- Git commit.
- When the release is available on Maven Central: Git push.
EOF

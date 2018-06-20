#!/usr/bin/env bash

set -eo pipefail

if [ $# -ne 1 ]; then
    echo "$0 | A script to release new versions automatically"
    echo "Usage: $0 VERSION_CODE"
    exit -1
fi

SELF_ROOT=$(cd $(dirname "$0") && pwd)
FILE_BUILD_GRADLE="$SELF_ROOT/algoliasearch/common.gradle"
VERSION_CODE=$1
CHANGELOG="CHANGELOG.md"

set +eo pipefail
COUNT_DOTS=$(grep -o "\." <<< $VERSION_CODE | wc -l)
set -eo pipefail

if [ $COUNT_DOTS -ne 2 ]; then
    echo "$VERSION_CODE is not a valid version code, please use the form X.Y.Z (e.g. v1 = 1.0.0)"
    exit -1
fi

# Check that the working repository is clean (without any changes, neither staged nor unstaged).
# An exception is the change log, which should have been edited, but not necessarily committed (we usually commit it
# along with the version number).
if [[ ! -z `git status --porcelain | grep -v "$CHANGELOG"` ]]; then
    echo "ERROR: Working copy not clean! Aborting." 1>&2
    echo "Please revert or commit any pending changes before releasing." 1>&2
    echo "Changes: $(git status)"
    exit 1
fi

# Check that the change log contains information for the new version.
set +e
version_in_change_log=$(cat "$SELF_ROOT/$CHANGELOG" | grep -E "^#+" | sed -E 's/^#* ([0-9.]*)\s*.*$/\1/g' | grep -x "$VERSION_CODE")
set -e
if [[ -z $version_in_change_log ]]; then
    echo "Version $VERSION_CODE not found in change log! Aborting." 1>&2
    exit 2
fi

# Only release on master (for manual runs, cannot happen through fastlane)
currentBranch=$(git rev-parse --abbrev-ref HEAD)
if [ "$currentBranch" != 'master' ]; then
  printf "Release: You must be on master\\n"
  exit 1
fi

function call_sed(){
PATTERN="$1"
FILENAME="$2"

# Mac needs a space between sed's inplace flag and extension
# if [ "$(uname)" == "Darwin" ]; then
#     sed -E -i '' "$PATTERN" "$FILENAME"
# else
    sed -E -i "$PATTERN" "$FILENAME"
# fi
}

echo "Updating version number to $VERSION_CODE..."
call_sed "s/PUBLISH_VERSION = '.*'/PUBLISH_VERSION = '$VERSION_CODE'/" "$FILE_BUILD_GRADLE"

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
#    echo "-------------------- Testing publication --------------------"
#    $SELF_ROOT/tools/test-publication.sh $module_name $VERSION_CODE

    # Perform the actual publication.
    echo "-------------------- Publishing remotely --------------------"
    $SELF_ROOT/gradlew uploadArchives
done

echo "Closing..."
$SELF_ROOT/gradlew closeRepository
echo "Sleeping 70s to ensure close completes before promoting."
# Example from closing logs: started Wednesday, April 11, 2018 16:03:05, stopped 16:04:03
sleep 70
echo "Promoting..."
$SELF_ROOT/gradlew promoteRepository
# Revert flavor to original.
git checkout $SELF_ROOT/algoliasearch/build.gradle

# Commit to git and push to GitHub
git add .
git commit -m "chore(release): Version $VERSION_CODE [ci skip]"

echo "Release complete! Pushing to GitHub"
git push origin HEAD

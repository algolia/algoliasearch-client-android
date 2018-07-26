#!/usr/bin/env bash

set -e # fail on first error

# Reflection.
SELF_ROOT=$(cd $(dirname "$0") && pwd)
PROJECT_ROOT=$(cd "$SELF_ROOT"/.. && pwd)

GIT_REPO_URL=$(cd "$PROJECT_ROOT" && git remote get-url origin)
BRANCH_NAME="gh-pages"
DST_DIR="$PROJECT_ROOT/algoliasearch/build/doc"

# Prepare the destination directory
# ---------------------------------
# If the destination directory exists, make sure that it's a Git checkout of the `gh-pages` branch.
if [[ -d "$DST_DIR" ]]; then
    branch_name=$(cd "$DST_DIR" && git rev-parse --abbrev-ref HEAD)
    if [[ "$branch_name" != "$BRANCH_NAME" ]]; then
        echo "ERROR: Directory '$DST_DIR' is not a checkout of the '$BRANCH_NAME' branch!" 1>&2
        exit 1
    fi
    pwd=$(pwd)
    # Clean the Git checkout.
    echo "Git checkout found; cleaning"
    cd "$DST_DIR" && git clean -f
    cd "$pwd"
# Otherwise, create it.
else
    echo "No Git checkout found; cloning"
    mkdir -p $(dirname "$DST_DIR")
    git clone "$GIT_REPO_URL" -b "$BRANCH_NAME" --single-branch "$DST_DIR"
fi

# Make the documentation
# ----------------------
# Clean the destination directory.
echo "Cleaning destination directory"
rm -rf "$DST_DIR"/*

# Online flavor -> root directory.
echo "# Generating online flavor"
"$PROJECT_ROOT/select-flavor.sh" online
./gradlew javadoc
# Offline flavor -> `offline` subdirectory.
echo "# Generating offline flavor"
"$PROJECT_ROOT/select-flavor.sh" offline
./gradlew javadoc

# Copy license.
LICENSE_FILE_NAME="LICENSE.txt"
cp "$PROJECT_ROOT/LICENSE" "$DST_DIR/$LICENSE_FILE_NAME"
cp "$PROJECT_ROOT/LICENSE" "$DST_DIR/offline/$LICENSE_FILE_NAME"

echo "Done. In $DST_DIR, git diff then add commit and push."

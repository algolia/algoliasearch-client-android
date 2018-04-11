#! /bin/sh
GRADLE_FOLDER=~/.gradle/
GRADLE_PROPERTIES=$GRADLE_FOLDER/gradle.properties
KEY_PATH=`pwd`/tools/bitrise-secret.key

add_signing() {
    echo "Adding bitrise key to gradle properties..."
    cat > $GRADLE_PROPERTIES << EOF
signing.keyId=9719DC41
signing.password=
signing.secretKeyRingFile=$KEY_PATH
EOF
    echo "New gradle properties:"
    cat $GRADLE_PROPERTIES
}

# Install gnupg2 if missing
if ! gpg --version > /dev/null; then
    echo "No gnupg* found. Installing gnupg2..."
    apt install gnupg2
fi

# Install conventional-changelog-cli if missing
npm list -g conventional-changelog-cli || npm install -g conventional-changelog-cli

# Setup gradle properties for gpg signing artifacts
if [ ! -d $GRADLE_FOLDER ]
then
    mkdir $GRADLE_FOLDER
fi

if [ -e $GRADLE_PROPERTIES ]
then
    if grep -q "signing." $GRADLE_PROPERTIES
    then
        echo "The gradle properties already have a signing profile, leaving untouched."
        exit 0
    fi
else
    touch $GRADLE_PROPERTIES
fi

add_signing
gpg --import $KEY_PATH

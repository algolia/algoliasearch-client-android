#! /bin/sh
GRADLE_FOLDER=~/.gradle/
GRADLE_PROPERTIES=$GRADLE_FOLDER/gradle.properties
KEY_PATH=`pwd`/tools/bitrise-secret.key

add_signing() {
    echo "Adding bitrise key to gradle properties..."
    cat > $GRADLE_PROPERTIES << EOF
signing.keyId=9719DC41
signing.secretKeyRingFile=$KEY_PATH
EOF
    echo "New gradle properties:"
    cat $GRADLE_PROPERTIES
}

if [ ! -d $GRADLE_FOLDER ]
then
    mkdir $GRADLE_FOLDER
fi

if [ -e $GRADLE_PROPERTIES ]
then
    if grep -q "signing." $GRADLE_PROPERTIES
    then
        echo "Fatal error: the gradle properties already have a signing profile"
        exit 2
    fi
else
    touch $GRADLE_PROPERTIES
fi

add_signing
gpg --import $KEY_PATH

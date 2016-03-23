#! /bin/sh
if ! [[ $ANDROID_TARGET && $ANDROID_ABI ]]; then
    echo "Error: Android Target and ABI should be set."
    echo "Target: $ANDROID_TARGET"
    echo "ABI: $ANDROID_ABI"
    exit 1
else
    expect -c "
set timeout -1;
spawn android update sdk --no-ui --all --filter platform-tools,extra-android-support,$ANDROID_TARGET,sys-img-$ANDROID_ABI-$ANDROID_TARGET,sysimg${ANDROID_TARGET/android/};
expect {
		    \"Do you accept the license\" { exp_send \"y\r\" ; exp_continue }
		        eof
}
"
fi

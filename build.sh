#!/bin/bash
# MorningLock — Replit Build Script
# Run this once in the Replit Shell to build your APK

echo "=== MorningLock Build Script ==="

# Install Java if needed
if ! command -v java &> /dev/null; then
    echo "Installing Java..."
    apt-get install -y openjdk-17-jdk
fi

# Download Android command line tools
echo "Downloading Android SDK tools..."
wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O cmdtools.zip
unzip -q cmdtools.zip -d android-sdk
mkdir -p android-sdk/cmdline-tools/latest
mv android-sdk/cmdline-tools/* android-sdk/cmdline-tools/latest/ 2>/dev/null || true

export ANDROID_HOME=$PWD/android-sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

# Accept licenses and install build tools
echo "y" | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

# Make gradlew executable
chmod +x gradlew

# Build debug APK
echo "Building APK..."
./gradlew assembleDebug

echo ""
echo "=== BUILD COMPLETE ==="
echo "APK is at: app/build/outputs/apk/debug/app-debug.apk"
echo "Download it from the Replit file browser on the left."

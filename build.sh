#!/bin/bash

# Lyfter App Build Script

set -e

echo "🚀 Building Lyfter App..."

# Clean previous builds
echo "🧹 Cleaning previous builds..."
./gradlew clean

# Build debug APK
echo "🔨 Building debug APK..."
./gradlew assembleDebug

# Build release APK
echo "📦 Building release APK..."
./gradlew assembleRelease

echo "✅ Build completed successfully!"
echo ""
echo "📱 Debug APK location: app/build/outputs/apk/debug/app-debug.apk"
echo "📱 Release APK location: app/build/outputs/apk/release/app-release-unsigned.apk"
echo ""
echo "To install the debug APK on a connected device:"
echo "adb install app/build/outputs/apk/debug/app-debug.apk"

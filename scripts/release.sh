#!/usr/bin/env bash
# Bumps versionCode + versionName (patch) in app/build.gradle.kts, then builds
# a signed release APK and AAB using ../signing/release.jks.
set -euo pipefail

cd "$(dirname "$0")/.."

GRADLE_FILE="app/build.gradle.kts"

current_code=$(grep -oE 'versionCode = [0-9]+' "$GRADLE_FILE" | grep -oE '[0-9]+')
current_name=$(grep -oE 'versionName = "[0-9]+\.[0-9]+\.[0-9]+"' "$GRADLE_FILE" | grep -oE '[0-9]+\.[0-9]+\.[0-9]+')

new_code=$((current_code + 1))

IFS='.' read -r major minor patch <<< "$current_name"
new_patch=$((patch + 1))
new_name="$major.$minor.$new_patch"

sed -i "s/versionCode = $current_code/versionCode = $new_code/" "$GRADLE_FILE"
sed -i "s/versionName = \"$current_name\"/versionName = \"$new_name\"/" "$GRADLE_FILE"

echo "Bumped versionCode $current_code -> $new_code"
echo "Bumped versionName $current_name -> $new_name"

./gradlew assembleRelease bundleRelease

echo ""
echo "Release build complete:"
echo "  APK: app/build/outputs/apk/release/app-release.apk"
echo "  AAB: app/build/outputs/bundle/release/app-release.aab"

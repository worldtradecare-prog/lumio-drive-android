#!/usr/bin/env bash
set -euo pipefail
# Run on chiaying-MS-S1-MAX (machineId 27b0d1d8-fe60-47fc-8f2f-21c859943a6e)
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
export JAVA_HOME="${JAVA_HOME:-/home/chiaying/sdk/jdk-17}"
export ANDROID_HOME="${ANDROID_HOME:-/home/chiaying/Android/Sdk}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
echo "sdk.dir=$ANDROID_HOME" > local.properties
# Prefer S1 RAM; fall back to low-mem if needed
./gradlew :app:assembleGenericDebug --no-daemon
APK="$ROOT/app/build/outputs/apk/generic/debug/app-generic-debug.apk"
ls -lh "$APK"
"$ANDROID_HOME/build-tools/"*/aapt dump badging "$APK" 2>/dev/null | head -1 || true
echo "APK=$APK"

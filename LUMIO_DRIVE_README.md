# Lumio Drive Android (Nextcloud fork)

Branded Nextcloud Android client for **Lumio Drive** (`https://lumiodrive.worldtrade.com.my`).

## Features vs stock Nextcloud
- `applicationId`: `my.worldtrade.lumiodrive` / app name **Lumio Drive**
- Brand color `#2563EB` (accent + gallery thumbnail loading skeletons)
- Fixed-server Lumio login (`LumioLoginActivity`)
- **Library by year** dialog on Media (gallery) three-dot menu → bottom sheet, calling `GET /apps/lumio_ui/years`
- Gallery default media folder is `/` so both `/Photos` and `/Videos` are included

## Build (debug APK)
```bash
export JAVA_HOME=/workspace/jdk-17   # or JDK 17+
export ANDROID_HOME=/workspace/android-sdk
echo "sdk.dir=$ANDROID_HOME" > local.properties
./gradlew :app:assembleGenericDebug
```
APK: `app/build/outputs/apk/generic/debug/app-generic-debug.apk`

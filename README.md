# Moodtrack

Android application for tracking mood and daily activities.

## Signed release build

- файл `keystore.properties` уже должен лежать в корне проекта;
- keystore `moodtrack-release-key.jks` должен лежать в корне проекта или по пути из `storeFile`;
- `keystore.properties` и `*.jks` нельзя коммитить;
- команда сборки:
  `./gradlew assembleRelease --no-configuration-cache`
- где лежит APK:
  `app/build/outputs/apk/release/app-release.apk`
- как установить:
  `adb install app/build/outputs/apk/release/app-release.apk`

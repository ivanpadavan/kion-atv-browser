# KION Browser for Android TV

Минимальный Android TV WebView-лаунчер для `https://hkion.kion.ru`.

## Управление

- D-pad и OK передаются в WebView как нативные Android key events.
- Android Back нативно преобразуется в `KEYCODE_ESCAPE` (`DOM keyCode = 27`).
- Menu открывает или скрывает адресную строку.
- Пять последовательных нажатий Back также открывают адресную строку; другая кнопка сбрасывает последовательность.
- Один Back или кнопка ✕ закрывают адресную строку.
- В адресной строке можно указать произвольный HTTP или HTTPS URL.
- URL, открытый кнопкой «Открыть», сохраняется в SharedPreferences и используется при следующем запуске.
- Protected Media ID разрешён для любого открытого origin; камера и микрофон не разрешаются.
- Video poster заменяется на `a.jpg` через MutationObserver.

## Сборка

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Эмулятор

```bash
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
"$ANDROID_HOME/emulator/emulator" -avd kion_atv_api31
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n ru.kion.atvbrowser/.MainActivity
```

Эмулятор подходит для проверки интерфейса и пульта. Widevine и уровень безопасности DRM необходимо проверять на физическом Android TV устройстве.

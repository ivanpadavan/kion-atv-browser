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

## Подготовка окружения без Android Studio

Команды ниже рассчитаны на macOS с Apple Silicon и Homebrew.

### 1. Установить JDK и Android CLI

```bash
brew install openjdk@17
brew install --cask android-commandlinetools android-platform-tools
```

`openjdk@17` — keg-only formula, поэтому для текущего терминала нужно явно указать JDK и Android SDK:

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
```

При желании эти три строки можно добавить в `~/.zshrc`.

### 2. Принять лицензии и установить SDK

```bash
yes | sdkmanager --licenses

sdkmanager \
  "platform-tools" \
  "platforms;android-35" \
  "build-tools;35.0.0" \
  "emulator" \
  "system-images;android-31;android-tv;arm64-v8a"
```

Проект компилируется с Android SDK 35, но `minSdk = 28`, поэтому APK поддерживает Android 9 и новее.

### 3. Создать Android TV AVD

```bash
printf 'no\n' | avdmanager create avd \
  --name kion_atv_api31 \
  --package "system-images;android-31;android-tv;arm64-v8a" \
  --device tv_1080p
```

В `~/.android/avd/kion_atv_api31.avd/config.ini` следует установить:

```ini
hw.keyboard=yes
```

Это позволяет использовать стрелки, Enter и Backspace клавиатуры как TV-пульт.

## Сборка APK

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Эмулятор

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools

"$ANDROID_HOME/emulator/emulator" \
  -avd kion_atv_api31 \
  -no-snapshot \
  -no-boot-anim

adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n ru.kion.atvbrowser/.MainActivity
```

Эмулятор подходит для проверки интерфейса и пульта. Widevine и уровень безопасности DRM необходимо проверять на физическом Android TV устройстве.

## Установка на Android TV по сети

На устройстве должна быть включена ADB-отладка и подтверждён RSA-ключ компьютера.

```bash
adb connect 192.168.1.69:5555
adb -s 192.168.1.69:5555 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s 192.168.1.69:5555 shell am start -n ru.kion.atvbrowser/.MainActivity
```

Флаг `-r` обновляет APK, не очищая cookies, localStorage и SharedPreferences.

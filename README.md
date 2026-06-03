# openAAC

OpenAAC is a planned free, public-source AAC app for Android tablets.

The goal is to make a practical communication app for non-speaking autistic children and their carers without accounts, cloud storage, subscriptions, paid icon packs, or external analytics.

## Current Status

This repository now contains the first Android prototype.

Planning docs:

- [Product plan](product.md)
- [Style guide](style.md)
- [Technical plan](tech.md)
- [Future ideas](future.md)

## Core Principles

- Free forever for families, carers, schools, and non-commercial use.
- Local-only by design.
- No sign in or sign up.
- No cloud database.
- No internet permission in the app.
- Android tablet first.
- Built for landscape tablet use.
- Designed first around 12-inch tablets, while still working decently on 10-inch tablets.
- Custom vocabulary and images from day one.
- Admin editing protected from accidental child access.
- Local usage insights only, never external analytics.

## MVP Direction

The first prototype is a native Android app using Kotlin, Jetpack Compose, SharedPreferences local storage, and Android local TextToSpeech. Room is still the intended next storage step once the prototype behavior is proven.

The MVP should include:

- First-time carer setup flow.
- Child communication mode.
- Admin mode with temporary default passcode `1234`.
- Forced admin passcode change during setup.
- 4x5 communication grid.
- Fixed bottom pinned-word strip.
- Top sentence bar with icons and text.
- Speak sentence, backspace, long-press clear, and `?`/`hmm` controls.
- Configurable vocabulary packs.
- Custom words and images.
- Local usage tracking and admin insights.
- Restore default layout and restore all defaults.

## License Status

OpenAAC is licensed under the [PolyForm Noncommercial License 1.0.0](LICENSE).

People should be able to inspect, use, and customize OpenAAC for free, but selling it, bundling it into a paid product, or using the code in a commercial AAC product requires explicit written permission from the project owner.

That goal is different from standard OSI open source, because OSI-approved open-source licenses allow commercial use. Do not assume commercial reuse is permitted.

## Build

This project uses the checked-in Gradle wrapper.

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
./gradlew assembleDebug
```

The debug APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Install on a connected Android tablet with:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

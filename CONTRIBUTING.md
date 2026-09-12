# Contributing to Suave

## Scope

Suave is developed as its own keyboard. S12 is the primary layout. Prefer changes that the live
engine actually uses; do not leave dead settings in the UI (see `CLAUDE.md`).

## Code

1. Open the project in Android Studio (Giraffe or newer) or use the Gradle CLI.
2. Build and install a debug APK after UI or keyboard changes:

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/suave-debug.apk
```

3. Name commits after the feature the user gained, not the chore that produced the diff.

## Layouts

New or edited layouts live under `app/src/main/java/com/suave/keyboard/layout/`. Follow existing
S12 patterns for keys, zones, and intents. Wire them through the builtin layout registry used by
settings.

## Translations

Edit `app/src/main/res/values/strings.xml` (English) and `values-de/strings.xml` (German) first.
Other locale files are incomplete leftovers; prefer adding keys to EN/DE and falling back rather
than inventing Thumb-Key-era copy.

## Upstream

Architectural history comes from [Thumb-Key](https://github.com/dessalines/thumb-key). Link it for
credit when relevant; do not treat upstream release notes or layouts as Suave product docs.

# Shared validation

Use [absolute-root notation](common.md). Domain test maps choose the tests; this file supplies commands and harness conventions, not a requirement to load every test.

## Android prerequisites

- Java 17 and Android SDK Platform 36, with a configured SDK (`ANDROID_HOME` or local Android configuration). `${ROOT}/gradle.properties` also names `JAVA_HOME_17_X64`.
- Full Git history: `${ROOT}/app/build.gradle.kts` rejects a shallow checkout and computes versions from first-parent commits. Check `git -C "$ROOT" rev-parse --is-shallow-repository`; if shallow, fetch the required history before validation.
- Use the checked-in wrapper. Dependencies/plugins are defined in `${ROOT}/app/build.gradle.kts` and `${ROOT}/build.gradle.kts`; no separate npm, Python package installation, or custom lint runner is needed.

## Smallest useful suite first

```sh
sh "$ROOT/gradlew" -p "$ROOT" :app:testDebugUnitTest \
  --tests 'com.ssmath.app.MathGameTest'
```

Replace the class with the domain map's selection. Repeat `--tests` for additional classes, or select an existing method:

```sh
sh "$ROOT/gradlew" -p "$ROOT" :app:testDebugUnitTest \
  --tests 'com.ssmath.app.MathAppTest.homeCollectionButtonsShowImagesBeforeLabelsAtEveryTextSize'
```

All Kotlin domains compile in `:app`; class filters reduce test execution, not compilation boundaries. Search `${TEST}` for the changed symbol/test tag when a mapping is insufficient. Add directly affected callers' tests, not every class whose name shares a prefix.

Existing full debug CI validation:

```sh
sh "$ROOT/gradlew" -p "$ROOT" \
  :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Use broader validation for shared behavior/build changes and final Android integration. Release-sensitive changes use the existing manual-release tasks:

```sh
sh "$ROOT/gradlew" -p "$ROOT" \
  :app:testDebugUnitTest :app:lintRelease :app:assembleRelease
```

Build validation does not authorize publishing a release or altering signing keys. For setup/network failures, report what could not run; do not claim a passing build. Documentation-only Skills edits need reference/metadata review, not an APK build.

## Reading content tooling

Python 3.9+ and the standard library suffice:

```sh
python3 "$DATA/generate.py" --check
python3 -m unittest discover -s "$DATA" -p 'test_*.py'
```

For an initial focused Python class/method, run from the data directory:

```sh
(cd "$DATA" && python3 -m unittest test_generation.WordSentenceStructureTest)
```

Generation without `--check` writes a new checksum-named pack and its compatible manifest; it is not a read-only validation step. Content changes also need the Android compatibility tests selected by reading-data. Python checks are not currently part of `${ROOT}/.github/workflows/android-ci.yml`.

## Existing test conventions

- Pure model tests use JUnit; Android/Compose tests use Robolectric, commonly SDK 34 and `Application::class`. Follow the nearby test harness rather than adding instrumentation infrastructure.
- Compose overflow/pixel assertions use `@GraphicsMode(GraphicsMode.Mode.NATIVE)` for real font metrics/rendering. Pixel tests draw the content view into a Bitmap and recycle it.
- Inject existing clocks, random sources, dispatchers, storage, and transport seams for determinism. Do not make regression tests depend on live GitHub or installed TTS.
- Synchronize Compose rendering before waiting for presentation-triggered persistence. Model completion alone does not execute UI effects; see `${TEST}/ReadingEntryTest.kt` / `completedReadingBatchPresentsAndClaimsSharedRewardsWithoutMathHistory`.
- Reuse `${TEST}/SettingsTestNavigation.kt` for Admin/game-settings navigation. Reading fixtures live in `${TEST}/ReadingModelTest.kt`, `${TEST}/ReadingSessionTest.kt`, and `${TEST}/ReadingSqliteStoreTest.kt`; repository-pack fixtures are in `${TEST}/ReadingPackTest.kt`.
- Preserve test setup/cleanup isolation for SharedPreferences, files, databases, and ViewModel scopes. No repository Markdown-specific test suite is currently defined.

Command sources: `${ROOT}/.github/workflows/android-ci.yml`, `${ROOT}/.github/workflows/manual-release.yml`, `${ROOT}/app/build.gradle.kts`, and `${DATA}/generate.py`.

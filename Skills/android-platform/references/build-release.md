# Android configuration, build, and release

Paths use [common notation](../../repo-router/references/common.md). Read executable configuration before relying on version descriptions in the README.

## Configuration map

| File | Authority |
| --- | --- |
| `${ROOT}/settings.gradle.kts` | Repository resolution, project name, single `:app` module |
| `${ROOT}/build.gradle.kts` | Android, Compose, and serialization plugin versions |
| `${ROOT}/app/build.gradle.kts` | Namespace/application ID, SDK levels, Java toolchain, dependencies, tests/lint, signing, Git-derived APK version |
| `${ROOT}/gradle.properties` | JVM heap/toolchain environment hint, AndroidX and Gradle configuration |
| `${ROOT}/gradle/wrapper/gradle-wrapper.properties` | Wrapper distribution and checksum |
| `${ROOT}/gradlew`, `${ROOT}/gradlew.bat` | Existing launchers |
| `${ROOT}/app/src/main/AndroidManifest.xml` | Application/Activity, Internet/install permissions, TTS query, no cleartext, backup/transfer and FileProvider |
| `${RES}/xml/data_extraction_rules.xml`, `${RES}/xml/update_paths.xml` | Private-data transfer exclusions and narrow update cache sharing |
| `${RES}/mipmap-anydpi/ic_launcher.xml`, `${RES}/drawable/ic_launcher_*.xml`, `${RES}/drawable-nodpi/ic_launcher_artwork.png` | Launcher identity/artwork |

## Build invariants

- Use the existing Java 17 toolchain and Android SDK configuration. Version numbers belong in build files, not duplicated Skill metadata.
- Full Git history is required; the build computes `versionCode` from first-parent commit count and formats `versionName`. Do not patch version logic to work around a shallow checkout.
- Keep `com.ssmath.app` as installed identity unless a deliberately incompatible migration is requested. The ssAcademy display name did not rename the installed package.
- Compose and serialization use the configured plugins; Android unit tests include resources and use existing Robolectric/Compose dependencies. Do not introduce a second build/test system.
- `${ROOT}/app/debug.keystore` is an intentional checked-in development signing key. Its contents do not belong in Skill context. Builds without a distribution key share that signing identity.
- Distribution signing reads `APK_SIGNING_STORE_FILE`, `APK_SIGNING_STORE_PASSWORD`, `APK_SIGNING_KEY_ALIAS`, and `APK_SIGNING_KEY_PASSWORD`. Record variable names only, never values. Preserve certificate continuity for in-place updates.

## Workflow map

All workflow filenames below are under `${ROOT}/.github/workflows`.

| Workflow | Behavior |
| --- | --- |
| `android-ci.yml` | Full-history checkout, Java 17, debug unit tests/lint/build on PRs and pushes to main; debug APK artifact |
| `manual-release.yml` | Explicit dispatch; builds main with release validation, verifies/names APK, publishes a stable release, cleans temporary signing key |
| `release-reminder.yml` | Posts a manual-release link after merge; event metadata only, no checkout/execution of PR code |
| `cleanup-releases.yml` | Scheduled/manual cleanup retaining the newest five releases and associated retained tags |

Release secrets are optional as a complete set; the manual workflow restores `APK_SIGNING_KEYSTORE_BASE64` to a temporary store only when configured. Do not weaken partial-configuration checks or key cleanup.

Publication derives its tag/asset name from built output metadata. The updater expects one `ssAcademy-v<version>.apk` stable release asset. Verify both sides if changing names or version format; see [updates](updates.md).

## Validation

[Shared validation](../../repo-router/references/validation.md) contains the exact debug/release commands and prerequisites. Dependency/build configuration changes warrant the full applicable Android pipeline, not just a single feature test.

Python generation checks are separate from current Android CI. Reading pack hosting is owned by reading-data; the pack manifest URL is not the APK release endpoint. Building does not authorize publishing, deleting releases, or rotating a signing key.

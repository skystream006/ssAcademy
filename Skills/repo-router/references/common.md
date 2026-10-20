# Shared facts and path notation

Read once when unfamiliar; domain references are not an instruction to preload this file.

## Portable absolute paths

Resolve `ROOT` to the absolute checkout root, for example with `git rev-parse --show-toplevel` from inside the checkout. This task's checkout is `/home/runner/work/ssAcademy/ssAcademy`; do not hardcode that machine path in future tooling.

| Variable | Absolute expansion |
| --- | --- |
| `SRC` | `${ROOT}/app/src/main/java/com/ssmath/app` |
| `TEST` | `${ROOT}/app/src/test/java/com/ssmath/app` |
| `RES` | `${ROOT}/app/src/main/res` |
| `DATA` | `${ROOT}/datasets/reading` |

Resolve these variables before using shell commands or file tools. Markdown links between these documents are relative to the containing document so the Skills remain portable.

## Repository-wide facts

- One Android Gradle module, `:app`; one production Kotlin package, `com.ssmath.app`. Skills are logical ownership boundaries, not build modules.
- Kotlin/Compose/Material 3 with activity-scoped ViewModels, coroutines, Kotlin serialization, and OkHttp. Dependencies live in `${ROOT}/app/build.gradle.kts`; plugin versions in `${ROOT}/build.gradle.kts`; module inclusion in `${ROOT}/settings.gradle.kts`.
- Offline-first local math/reading application. No backend, account API, cloud persistence, or separate worker service. Science and Advanced are placeholders.
- Source and tests are authoritative. `${ROOT}/README.md` supplies product context, but some generator/version descriptions lag executable configuration.
- Preserve existing persisted identifiers and installed app identity; consult the owning domain before changing a contract. Do not copy all persistence rules into every Skill.

## Skill discovery and maintenance

`${ROOT}/Skills` is the requested canonical location. It is not assumed to be an automatically discovered Skill directory in every agent host. Configure a host that supports custom Skill roots to index its immediate domain subdirectories; otherwise explicitly load the selected `${ROOT}/Skills/<domain>/SKILL.md`. Confirm discovery with the intended host before relying on automatic invocation.

Index `name` and `description` metadata, not all reference bodies. The router is a fallback for uncertain requests, not a mandatory prelude to every domain.

Keep `SKILL.md` as a small control plane. Put source entry symbols, invariants, and behavior-to-test mappings in references; update the corresponding reference when a boundary changes. Prefer symbol names over cached line numbers. Do not duplicate the architecture map or whole source files.

No installer, mirrored Skill tree, new application dependency, or agent configuration is introduced here.

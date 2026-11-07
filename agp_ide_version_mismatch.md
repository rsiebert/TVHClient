# AGP 9.5.1 vs Android Studio 2026.1.3 — version mismatch note

> [!NOTE]
> Recorded 2026-11-06. **Decision: Path B — upgrade Android Studio.** No build files were changed.
> **RESOLVED same day** — Studio updated to 2026.2.2 (see [Resolution](#resolution)).

## Symptom

Android Studio reports on sync:

```
The project is using an incompatible version (AGP 9.5.1) of the Android Gradle plugin.
Latest supported version is AGP 9.3.0
```

## Diagnosis

This is an **IDE-side** limit, not a build defect. AGP 9.5.1 postdates Studio 2026.1.3, whose bundled
Gradle/AGP integration stops at 9.3.0, so the IDE refuses to import the project model.

### State of the project at the time of writing

| Item | Value | Where |
| --- | --- | --- |
| AGP | **9.5.1** | [build.gradle](file:///C:/Users/Robert/StudioProjects/TVHClient/build.gradle) — `ext.agpVersion` |
| `com.android.legacy-kapt` shim | 9.5.1 (tracks `$agpVersion`) | same file |
| Kotlin | 2.4.21 | same file |
| KSP | 2.3.13 | same file |
| Gradle wrapper | **9.8.2** | [gradle-wrapper.properties](file:///C:/Users/Robert/StudioProjects/TVHClient/gradle/wrapper/gradle-wrapper.properties) |
| Composite build | `includeBuild('../ExoPlayer')`, 4 `substitute` rules | [settings.gradle](file:///C:/Users/Robert/StudioProjects/TVHClient/settings.gradle) |
| Android Studio | **2026.1.3** | `C:\Program Files\Android\Android Studio` |
| Cached AGP versions | 8.13.0, 9.5.1 (**9.3.0 not cached**) | `~/.gradle/caches/modules-2/files-2.1/com.android.tools.build/gradle` |
| Downloaded Gradle distributions | 5.4.1, 8.13, 9.8.2 (all/bin) | `~/.gradle/wrapper/dists` |

The build itself is green: `./gradlew :app:assembleDebug` passes on Gradle 9.8.2 / AGP 9.5.1 / Kotlin 2.4.21
(see [migration_requirements.md](file:///C:/Users/Robert/StudioProjects/TVHClient/migration_requirements.md) §9).
Nothing needs fixing to build from the CLI — only the IDE import is blocked.

### Version-matrix context

- Kotlin 2.4.21 declares support for AGP **8.5.2 – 9.3.1**, i.e. AGP 9.5.1 is already *outside* the official
  Kotlin-tested range. AGP 9.3.x would put the toolchain back inside it.
- KGP 2.4.21 declares support for Gradle 7.6.3 – **9.7.0**, so Gradle 9.8.2 is also beyond the tested ceiling.
- AGP 9.3.0 requires Gradle **≥ 9.5.0** and supports API ≤ 37 — both satisfied by the current setup.
- `migration_requirements.md` §2 itself lists AGP **9.3** as the target, so the project overshot by two minors.

## Path B — chosen: upgrade Android Studio

1. Update Studio to a build that supports AGP 9.5.1 (2026.2 or later).
2. Invalidate caches / restart if the error banner persists, then run **Sync Project with Gradle Files**.
3. Expected result: sync succeeds, `:app:assembleDebug` still green. **No build-file edits required.**

Sanity check after the update — the current build must not regress, so confirm:

- [ ] `./gradlew :app:assembleDebug` green
- [ ] Data Binding codegen still works (`BR` / binding classes) — `app` depends on `com.android.legacy-kapt`
- [ ] The composite build (`../ExoPlayer`) still resolves; it is pinned to AGP 9.5.1 as well

## Path A — not taken: align the project down to AGP 9.3.0

Kept for reference only. The composite build requires an *identical* AGP in both builds (this already broke the
build twice as "Using multiple versions of AGP is not allowed"), so both repos must move together, and
`../ExoPlayer` is outside my sandbox.

1. [build.gradle](file:///C:/Users/Robert/StudioProjects/TVHClient/build.gradle): `agpVersion = '9.5.1'` → `'9.3.0'`.
   The `com.android.legacy-kapt` classpath follows automatically because it interpolates `$agpVersion`.
2. `../ExoPlayer`: same AGP pin (currently 9.5.1 from commit `d952d9e8e`).
3. Gradle: leave at 9.8.2 and test, or drop to 9.7.0 to match KGP 2.4.21's tested maximum.
4. AGP 9.3.0 is not in the local cache, so the first build needs network access to `google()`.
5. Re-run `./gradlew :app:assembleDebug`.

Trade-off: needs two repos edited, and AGP-9.3-on-Gradle-9.8 is untested territory. Path B keeps the
already-green build untouched.

## Resolution

Android Studio upgraded **2026.1.3 → 2026.2.2** (`AI-262.10968.63.2622.16840690`, data dir
`AndroidStudio2026.2.2`, requires JDK 25 as its own runtime).

| Check | Result |
| --- | --- |
| Studio build | 2026.2.2 — AGP 9.5.1 no longer flagged |
| `./gradlew :app:assembleDebug` | **green** |
| Build files changed | none — AGP 9.5.1 / Gradle 9.8.2 / Kotlin 2.4.21 untouched |
| `../ExoPlayer` fork | untouched (still AGP 9.5.1, matching) |

Path A is therefore permanently parked, not pending. The remaining post-update checklist from Path B
still applies and was **not** exercised by the build alone:

- [ ] IDE **Sync Project with Gradle Files** clean (the automated sync attempt reported "Sync skipped")
- [ ] Data Binding codegen visible to the IDE (`BR` / binding classes) — `app` uses `com.android.legacy-kapt`
- [ ] Composite build `../ExoPlayer` imports without an AGP-lockstep complaint in the IDE model

## Why this matters beyond the banner

`migration_requirements.md` §10 still has open steps (Media3 migration, dialog/drawer replacement, targetSdk 37)
and §9 carries runtime verification debt — notably the MaterialDrawer excluded `kotlin-android-extensions-runtime`
and the `BillingDataSource.onResume` lifecycle rewrite. Those are unrelated to this mismatch but should not be
forgotten once the IDE is updated.
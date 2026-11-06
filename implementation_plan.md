# Consume the local ExoPlayer fork via a Gradle composite build

> [!IMPORTANT]
> **Status: Phase 1 implemented and verified.** `:app:assembleDebug` succeeds both from the IDE
> (`gradle_build`) and from the shell, and `gradle_sync` succeeds. **Phase 2 (native FFmpeg libraries) was
> not executed** — see the end of this document. This file has been updated to describe what was actually
> done, including deviations from the original plan (see [Deviations from the original plan](#deviations-from-the-original-plan)).

## Goal

Make `:app:assembleDebug` succeed in TVHClient.

The build failed for two independent reasons:

1. **Unresolvable ExoPlayer artifacts.** [app/build.gradle](file:///home/rsiebert/projects/TVHClient/app/build.gradle#L143-L148) depends on
   `org.tvheadend.tvhclient.exoplayer:{exoplayer-core,exoplayer-ui,exoplayer-dash,extension-ffmpeg}:2.11.7`.
   Those were published to Bintray (now shut down) and exist in no configured repository.
2. **Kotlin could not target JVM 21.** The project is configured for JDK 21 ([.idea/misc.xml](file:///home/rsiebert/projects/TVHClient/.idea/misc.xml) —
   `project-jdk-name="zulu-21"`, `languageLevel="JDK_21"`), but `kotlinVersion = '1.8.22'`
   ([build.gradle](file:///home/rsiebert/projects/TVHClient/build.gradle#L5), mirrored in
   [.idea/kotlinc.xml](file:///home/rsiebert/projects/TVHClient/.idea/kotlinc.xml)) had no `JvmTarget`
   constant for 21 → `Unknown Kotlin JVM target: 21` in `:data:kaptGenerateStubsDebugKotlin`.

The fix keeps the dependency coordinates untouched and has Gradle **substitute** them with the local source
clone at `/home/rsiebert/projects/ExoPlayer` (the sibling of this project). That clone is on branch
`release-v2`; work was done on a new branch `tvhclient-local-build` cut from it (HEAD `e09eb8b13` — a merge
whose history contains `8fcbbb09a` "Bump release to 2.11.7"), and its
[constants.gradle](file:///home/rsiebert/projects/ExoPlayer/constants.gradle) sets
`releaseVersion = '2.11.7'`, an exact match for `exoplayerVersion = '2.11.7'`.

Because the fork is a 2019-era build (AGP `3.5.3`, `jcenter()`, Bintray publishing, no `namespace`,
`android.libraryVariants`), it first had to be modernised for AGP 8.13 / Gradle 8.13.

> [!NOTE]
> The composite build is deliberate: `app/build.gradle` still uses
> `org.tvheadend.tvhclient.exoplayer:*:2.11.7`. Only `settings.gradle` gained a substitution rule.
> The alternative (adding the ExoPlayer modules to TVHClient's own `settings.gradle`) would have forced
> `app/build.gradle` to switch to `project(...)` dependencies and would have dragged ~20 more modules into
> the build.

## Environment (this machine)

> [!NOTE]
> This development host is **native WSL2 Linux** (`Linux … -microsoft-standard-WSL2`, x86_64), and **both
> repositories live on the WSL ext4 filesystem** (not under `/mnt/c`):
> - App: `/home/rsiebert/projects/TVHClient`
> - Fork: `/home/rsiebert/projects/ExoPlayer` (git remote `https://github.com/rsiebert/ExoPlayer`)
>
> - Android SDK: `/home/rsiebert/Android/Sdk`. Gradle wrapper: **8.13**.
> - NDK: `/home/rsiebert/Android/Sdk/ndk/30.0.16248370` (**r30**), a native Linux build.
> - Because everything is already on ext4, the FFmpeg build (Phase 2) runs **in place**.

> [!WARNING]
> **The machine had no Gradle-compatible JDK.** The original plan assumed "JDK 11 and JDK 21 are
> installed", but the only JDK present was Android Studio's bundled JBR at `/opt/android-studio/jbr`,
> which is **JDK 25** — Gradle 8.13 supports Java 1.8–23 and rejects it. There was no `java` on `PATH`, no
> `/usr/lib/jvm`, and no `~/.jdks`.
>
> **Resolution:** `openjdk-21-jdk-headless` was installed via `apt` (Ubuntu 22.04) →
> `/usr/lib/jvm/java-21-openjdk-amd64`, and `org.gradle.java.home` was set in
> `~/.gradle/gradle.properties` so all Gradle invocations use it.

## Deviations from the original plan

| # | The plan said | What was actually found | What was done |
| --- | --- | --- | --- |
| 1 | JDK 11 + 21 are installed | Only Android Studio's JBR **25**; Gradle 8.13 needs ≤ 23 | Installed `openjdk-21-jdk-headless` (apt); set `org.gradle.java.home` in `~/.gradle/gradle.properties` |
| 2 | `.idea/gradle.xml` selects `#GRADLE_LOCAL_JAVA_HOME` | No `gradle.xml` existed; the IDE's Gradle JDK lived in the binary `project.dat` **and** in `.gradle/config.properties`, both pinned to `C:\Users\Robert\.jdks\azul-21.0.1` | Created [.idea/gradle.xml](file:///home/rsiebert/projects/TVHClient/.idea/gradle.xml) with `gradleJvm=#GRADLE_LOCAL_JAVA_HOME` and repointed [.gradle/config.properties](file:///home/rsiebert/projects/TVHClient/.gradle/config.properties) at the Linux JDK |
| 3 | Bump Kotlin `1.8.22` → `1.9.24` and that is sufficient | At 1.9.24 the build then failed because AGP 8.13's `databinding-ktx` ships **Kotlin 2.2.0 metadata**, which a 1.9.24 compiler cannot read. Bumping to 2.2.0 instead failed because **Room 2.6.0 / Dagger 2.48.1 cannot read Kotlin 2.2 metadata** | Kept **Kotlin 1.9.24**, excluded `androidx.databinding:databinding-ktx`, and forced `kotlin-stdlib` to the compiler version |
| 4 | (not mentioned) | In a composite build the **included** build resolves the Android SDK separately | Created [ExoPlayer/local.properties](file:///home/rsiebert/projects/ExoPlayer/local.properties) with `sdk.dir` |

> [!CAUTION]
> Deviation #3 is a compromise, not a clean fix. This project sits on a dependency set where AGP 8.13
> requires Kotlin 2.2 while Room 2.6.0 / Dagger 2.48.1 only understand Kotlin ≤ 1.9 metadata. Dropping
> `databinding-ktx` (an optional KTX-helper artifact not referenced by the app) makes the build work, but
> the durable fix is to upgrade Room/Dagger (or align AGP) so the whole toolchain can move to Kotlin 2.x.

## Decisions (as implemented)

| Question | Decision |
| --- | --- |
| FFmpeg extension | `:extension-ffmpeg` is a source module in **Phase 1** (compiles without native `.so`). Native libraries remain **Phase 2** (not done). |
| Kotlin / JVM 21 | `kotlinVersion` `1.8.22` → **`1.9.24`**; bytecode stays at 17 via `kotlinOptions.jvmTarget = "17"`. See deviation #3. |
| Fork working tree | Modified in place at `/home/rsiebert/projects/ExoPlayer` on branch **`tvhclient-local-build`** (build scripts + manifests only). |
| Phase 2 toolchain | Port upstream's latest FFmpeg-extension build tooling (LLVM binutils + CMake) for a modern NDK. Build natively on this Linux/WSL2 host, in place. **(Planned, not executed.)** |
| FFmpeg version | Stay on **release/4.2** (the latest upstream `build_ffmpeg.sh` also builds 4.2, so `ffmpeg_jni.cc` needs no source changes). |

## Changes applied — Phase 1

### ExoPlayer fork (`/home/rsiebert/projects/ExoPlayer`, branch `tvhclient-local-build`)

Modernised for AGP 8.13 / Gradle 8.13 and trimmed to only the modules TVHClient needs.

> [!NOTE]
> In a Gradle composite build the **including** build's Gradle version (TVHClient's 8.13) drives every
> included build, so the fork's own wrapper is ignored — the fork only needed to be *AGP*-compatible.

#### [MODIFY] [build.gradle](file:///home/rsiebert/projects/ExoPlayer/build.gradle) (root)
- AGP `3.5.3` → `8.13.0`; dropped the `com.novoda:bintray-release` and `strict-version-matcher` classpaths.
- Repositories reduced to `google()` + `mavenCentral()` (`jcenter()` is dead).
- Dropped the trailing `apply from: 'javadoc_combined.gradle'` (used the removed `android.libraryVariants`).

#### [MODIFY] [settings.gradle](file:///home/rsiebert/projects/ExoPlayer/settings.gradle)
- `rootProject.name = 'exoplayer'`; includes only `:library-core`, `:library-dash`, `:library-ui`,
  `:extension-ffmpeg`, `:testutils`. The demos, playbacktests, other extensions and `library-all` were
  dropped (they reference SDKs that do not exist locally and would fail at configuration time).

#### [MODIFY] [gradle.properties](file:///home/rsiebert/projects/ExoPlayer/gradle.properties)
- Removed `android.enableUnitTestBinaryResources` (removed from AGP). `buildDir=buildout` kept.

#### [MODIFY] [constants.gradle](file:///home/rsiebert/projects/ExoPlayer/constants.gradle)
- `compileSdkVersion` 29 → **34**; `minSdkVersion` 16 → **19**; `appTargetSdkVersion` 29 → **34**.

#### [MODIFY] 5 × module build script
[core](file:///home/rsiebert/projects/ExoPlayer/library/core/build.gradle),
[dash](file:///home/rsiebert/projects/ExoPlayer/library/dash/build.gradle),
[ui](file:///home/rsiebert/projects/ExoPlayer/library/ui/build.gradle),
[ffmpeg](file:///home/rsiebert/projects/ExoPlayer/extensions/ffmpeg/build.gradle),
[testutils](file:///home/rsiebert/projects/ExoPlayer/testutils/build.gradle)
- Added the mandatory AGP 8 `namespace` (matching the value formerly in each manifest, so generated
  `R`/`BuildConfig` package names are unchanged):

  | Module | `namespace` |
  | --- | --- |
  | `:library-core` | `com.google.android.exoplayer2.core` |
  | `:library-dash` | `com.google.android.exoplayer2.source.dash` |
  | `:library-ui` | `com.google.android.exoplayer2.ui` |
  | `:extension-ffmpeg` | `com.google.android.exoplayer2.ext.ffmpeg` |
  | `:testutils` | `com.google.android.exoplayer2.testutil` |

- Removed the `buildTypes { debug { testCoverageEnabled = true } }` blocks (removed in AGP 8).
- Removed the `apply from: '.../javadoc_library.gradle'` and `'.../publish.gradle'` lines.
- ffmpeg module: `jniLibs.srcDir 'src/main/libs'` kept for Phase 2. `jni.srcDirs = []` was **accepted** by
  AGP 8.13, so it was left in place.

#### [MODIFY] 5 × AndroidManifest.xml
[core](file:///home/rsiebert/projects/ExoPlayer/library/core/src/main/AndroidManifest.xml),
[dash](file:///home/rsiebert/projects/ExoPlayer/library/dash/src/main/AndroidManifest.xml),
[ui](file:///home/rsiebert/projects/ExoPlayer/library/ui/src/main/AndroidManifest.xml),
[ffmpeg](file:///home/rsiebert/projects/ExoPlayer/extensions/ffmpeg/src/main/AndroidManifest.xml),
[testutils](file:///home/rsiebert/projects/ExoPlayer/testutils/src/main/AndroidManifest.xml)
- Removed the `package="…"` attribute (AGP 8 rejects it when `namespace` is set).

#### [NEW] [local.properties](file:///home/rsiebert/projects/ExoPlayer/local.properties)
- `sdk.dir=/home/rsiebert/Android/Sdk`. Required because the included build resolves the Android SDK from
  its **own** directory when the IDE-launched Gradle daemon has no `ANDROID_HOME`. (Machine-specific;
  normally git-ignored.)

---

### TVHClient

#### [MODIFY] [settings.gradle](file:///home/rsiebert/projects/TVHClient/settings.gradle)
```groovy
include ':app', ':data', ':htsp'

includeBuild('../ExoPlayer') {
    dependencySubstitution {
        substitute module('org.tvheadend.tvhclient.exoplayer:exoplayer-core')   using project(':library-core')
        substitute module('org.tvheadend.tvhclient.exoplayer:exoplayer-dash')   using project(':library-dash')
        substitute module('org.tvheadend.tvhclient.exoplayer:exoplayer-ui')     using project(':library-ui')
        substitute module('org.tvheadend.tvhclient.exoplayer:extension-ffmpeg') using project(':extension-ffmpeg')
    }
}
```
`substitute module(...)` without a version matches any requested version, so `2.11.7` is satisfied locally.

#### [MODIFY] [build.gradle](file:///home/rsiebert/projects/TVHClient/build.gradle) (root)
- `kotlinVersion` `1.8.22` → **`1.9.24`**.
- Added to the existing `allprojects { }` block (deviation #3):

```groovy
configurations.all {
    exclude group: 'androidx.databinding', module: 'databinding-ktx'
    resolutionStrategy {
        force "org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion"
    }
}
```

> [!NOTE]
> **`app/build.gradle` was intentionally left untouched** — no coordinate changes, no `project(...)` swaps.
> No duplicate-classes problem surfaced, so no `exclude group: 'com.google.android.exoplayer'` was needed.

---

### Machine-local environment changes (outside the repositories)

| File | Change |
| --- | --- |
| `~/.gradle/gradle.properties` | **NEW** — `org.gradle.java.home=/usr/lib/jvm/java-21-openjdk-amd64` |
| [.gradle/config.properties](file:///home/rsiebert/projects/TVHClient/.gradle/config.properties) | `java.home` repointed from `C:\Users\Robert\.jdks\azul-21.0.1` to the Linux JDK |
| [.idea/gradle.xml](file:///home/rsiebert/projects/TVHClient/.idea/gradle.xml) | **NEW** — `gradleJvm=#GRADLE_LOCAL_JAVA_HOME` (resolves via the `gradle.properties` above) |
| `/usr/lib/jvm/java-21-openjdk-amd64` | **NEW** — `openjdk-21-jdk-headless` installed via apt |

## Verification results

### Automated

| Check | Result |
| --- | --- |
| `./gradlew :app:assembleDebug` (shell, `JAVA_HOME` = JDK 21) | ✅ **BUILD SUCCESSFUL** |
| `gradle_build(":app:assembleDebug")` (IDE) | ✅ **Build finished successfully** |
| `gradle_sync()` | ✅ **Sync finished successfully** |
| `:app:dependencies --configuration debugRuntimeClasspath` | ✅ resolved locally (below) |
| APK produced | ✅ `app/build/outputs/apk/debug/org.tvheadend.tvhclient-2.4.8-241-debug.apk` (16.3 MB) |

Substitution proof — the published coordinates resolve to the local fork, not to an external module:

```
org.tvheadend.tvhclient.exoplayer:exoplayer-ui:2.11.7      -> project :ExoPlayer:library-ui
org.tvheadend.tvhclient.exoplayer:exoplayer-core:2.11.7    -> project :ExoPlayer:library-core
org.tvheadend.tvhclient.exoplayer:exoplayer-dash:2.11.7    -> project :ExoPlayer:library-dash
org.tvheadend.tvhclient.exoplayer:extension-ffmpeg:2.11.7  -> project :ExoPlayer:extension-ffmpeg
```

### Not yet done

- **Manual on-device verification** (launch + play a live/recorded stream) has not been performed.
- **Phase 2** — see below.

## Phase 2 — Native FFmpeg libraries (planned, NOT executed)

Independent of Phase 1: nothing in TVHClient instantiates `FfmpegAudioRenderer`, so this phase changes
neither build nor runtime behaviour today. It exists so the extension's native codecs are actually present.

> [!NOTE]
> **Why the extension's Java/JNI code must not be replaced.** Two hard constraints verified in the fork:
> - [FfmpegLibrary.java](file:///home/rsiebert/projects/ExoPlayer/extensions/ffmpeg/src/main/java/com/google/android/exoplayer2/ext/ffmpeg/FfmpegLibrary.java#L38) loads
>   `LibraryLoader("avutil", "swresample", "avcodec", "ffmpeg")`, so the JNI wrapper **must** stay
>   `libffmpeg.so`. Upstream's modern CMake names that target `ffmpegJNI`, so the port must rename it.
> - [ffmpeg_jni.cc](file:///home/rsiebert/projects/ExoPlayer/extensions/ffmpeg/src/main/jni/ffmpeg_jni.cc#L36-L49) binds its symbols to
>   this module's class names (`…_FfmpegDecoder_…`, `…_FfmpegLibrary_…`). Newer revisions renamed the
>   decoder class, so a wholesale drop-in of the newest `.cc` would fail to resolve at runtime.

### Planned [MODIFY] [build_ffmpeg.sh](file:///home/rsiebert/projects/ExoPlayer/extensions/ffmpeg/src/main/jni/build_ffmpeg.sh)

| # | Change | Why |
| --- | --- | --- |
| 1 | Clone from `https://git.ffmpeg.org/ffmpeg.git` (or the GitHub mirror), keep `git checkout release/4.2` | `git://source.ffmpeg.org/ffmpeg` is retired; the script aborts today |
| 2 | Binutils → `llvm-nm`, `llvm-ar`, `llvm-ranlib`, `llvm-strip` from `$TOOLCHAIN_PREFIX/bin` | NDK r23+ removed GNU binutils (all four confirmed present in NDK r30) |
| 3 | Cross-prefixes → `armv7a-linux-androideabi21-`, `i686-linux-android21-`, `aarch64-linux-android21-`, `x86_64-linux-android21-` | modern NDKs require API ≥ 21; the current `-android16-` wrappers no longer exist |
| 4 | Add `-Wl,-Bsymbolic` to the `arm64-v8a` `--extra-ldflags` | upstream's fix for NDK 23+ on arm64 |
| 5 | `HOST_PLATFORM=linux-x86_64` | matches the installed NDK's only `prebuilt/` folder |
| 6 | `ENABLED_DECODERS` → `aac mp3 ac3 eac3 dca truehd flac vorbis opus alac pcm_mulaw pcm_alaw` | exactly the codecs [`FfmpegLibrary.getCodecName()`](file:///home/rsiebert/projects/ExoPlayer/extensions/ffmpeg/src/main/java/com/google/android/exoplayer2/ext/ffmpeg/FfmpegLibrary.java#L85-L122) maps to |

Produces `src/main/jni/ffmpeg/android-libs/<abi>/{libavcodec,libswresample,libavutil}.so`.

### Planned [NEW] [CMakeLists.txt](file:///home/rsiebert/projects/ExoPlayer/extensions/ffmpeg/src/main/jni/CMakeLists.txt)

Upstream's modern replacement for the deprecated `Android.mk` + `ndk-build` path:
- `cmake_minimum_required(VERSION 3.21.0 FATAL_ERROR)`;
- import `avutil` / `swresample` / `avcodec` from `ffmpeg/android-libs/${ANDROID_ABI}/`;
- build `ffmpeg_jni.cc` into a shared library named **`ffmpeg`** (not upstream's `ffmpegJNI`);
- link `android` + `log`.
- Keep `Android.mk` / `Application.mk` as a documented fallback (`ndk-build` still ships in NDK r30, but
  needs `APP_PLATFORM := android-21` instead of the current `android-9`).

### Planned invocation

```bash
cd /home/rsiebert/projects/ExoPlayer/extensions/ffmpeg/src/main/jni
chmod +x build_ffmpeg.sh
FFMPEG_EXT_PATH="$(pwd)"
NDK_PATH="/home/rsiebert/Android/Sdk/ndk/30.0.16248370"
HOST_PLATFORM="linux-x86_64"
ENABLED_DECODERS=(aac mp3 ac3 eac3 dca truehd flac vorbis opus alac pcm_mulaw pcm_alaw)
./build_ffmpeg.sh "${FFMPEG_EXT_PATH}" "${NDK_PATH}" "${HOST_PLATFORM}" "${ENABLED_DECODERS[@]}"
```

> [!TIP]
> Verify LF line endings on the script before running
> (`git -C /home/rsiebert/projects/ExoPlayer check-attr -a extensions/ffmpeg/src/main/jni/build_ffmpeg.sh`)
> — CRLF in a `.sh` file is the classic Windows→Linux build breaker.

Results must land in `extensions/ffmpeg/src/main/libs/<abi>/`, the directory the module already wires up via
`jniLibs.srcDir 'src/main/libs'`, so **no Gradle change and no `externalNativeBuild` is needed**. Per ABI the
output is 4 libraries: `libavutil.so`, `libswresample.so`, `libavcodec.so`, `libffmpeg.so`.

### Planned Phase 2 verification

Inspect the packaged APK and confirm `lib/<abi>/` contains all four libraries for each built ABI; record the
APK size delta. Runtime behaviour is unchanged because the app never instantiates `FfmpegAudioRenderer`
(wiring it in via `DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON` would be a separate follow-up).

## Rollback

- ExoPlayer clone: `git -C /home/rsiebert/projects/ExoPlayer checkout release-v2` (work is on `tvhclient-local-build`).
- TVHClient: `git checkout -- settings.gradle build.gradle` reverts the two changed files.
- Remove the machine-local additions if desired: `.idea/gradle.xml`, the `org.gradle.java.home` line in
  `~/.gradle/gradle.properties`, `ExoPlayer/local.properties`, and the `.gradle/config.properties` fix.
- Phase 2 (if later run) creates only untracked artefacts (`src/main/jni/ffmpeg/`, `src/main/libs/`),
  removable by deleting those directories.

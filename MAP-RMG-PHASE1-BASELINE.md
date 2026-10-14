# MAP-RMG: Phase 1 baseline report

**Date:** 2026-10-14 · **Work branch:** `arena/39229671-map-rmg` (from `d7b88c6`) ·
**Upstream project:** Vela Maps (GPL-3.0, `PimpinPumpkin/Vela`) · **My commits so far:** `5a6ed52`

---

## 1. Verdict

| Item | Status |
|---|---|
| Project structure / Gradle configuration inspected | ✅ done |
| Source, resource and manifest inventory | ✅ done |
| Baseline build (`./gradlew :app:assembleRelease`) | ❌ **not executable in this environment** , see §4 |
| Baseline unit tests (`:core:test`, `:app:testDebugUnitTest`) | ❌ not executable (needs JDK + Gradle + Maven) |
| APK produced | ❌ **none.** No APK was built, and I am not claiming one was. |
| Repo defect found and fixed | ⚠️ yes , the branding commit had truncated the English string base (§5), restored in `5a6ed52` |
| Every device-free check the repo ships | ✅ passes (§6) |

Two separate problems exist, and only one of them is ours to fix:

1. **Environmental (this sandbox):** no JDK, no Android SDK, and every host that serves the Gradle
   distribution, the Android SDK and Maven artifacts is blocked by the network policy. No Android
   build of any kind can run here. Details and proof in §4.
2. **Repository (yours, now fixed):** the branch could not have compiled *anywhere* , the branding
   commit left `app/src/main/res/values/strings.xml` with 31 of its 1,354 keys, so 1,260 `R.string`
   references had no definition. Restored in `5a6ed52`, keeping the MAP-RMG name.

---

## 2. Repository and branch state

- Repo `sajjad1566/MAP-RMG` is a **fork of `PimpinPumpkin/Vela`**. Default branch `main`
  (`3c8fc20`). The other remote branch is `map-rmg-branding-phase1` (`d7b88c6`).
- `NOVA-GPS-PRO` was **never accessed, listed or modified** , this checkout only knows the
  `sajjad1566/MAP-RMG` remote. `main` was not touched: everything is committed on
  `arena/39229671-map-rmg`.
- This session is pinned to `arena/39229671-map-rmg` (the branch Arena tracks for this task); I
  cannot create or push to another branch. Its content is identical to `map-rmg-branding-phase1`
  plus my commits, so a PR from it into `map-rmg-branding-phase1` (or `main`, with your approval)
  carries exactly the phase work.
- Different from `main` (`git diff --stat main..HEAD`): `ui/theme/Color.kt` (your palette) and
  `values/strings.xml` (your `app_name`, now that the truncation is repaired). Nothing else -
  the branch is `main` + branding, which is a clean starting point for Phases 2–6.
- No releases and no CI runs exist on the fork; the GitHub Actions tab reports zero workflows, so
  the repo's own `ci.yml` has never run here (see §7 for how we can still get an APK).

## 3. Project structure and stack

Four Gradle modules (`settings.gradle.kts`):

| Module | Role | Size |
|---|---|---|
| `:app` | Compose UI shell over MapLibre, Android Auto/car UI, services, downloads | 245 Kotlin files, ~72 k lines |
| `:core` | UI-free engine: Google interop, routing, obf reader, navigation, voice | 335 Kotlin files, ~31 k lines |
| `:baselineprofile` | Macrobenchmark profile generator (baseline perf) | small |
| `:osmand-shaded` | OsmAnd router jar with protobuf relocated | small |

199 unit-test files (`app/src/test`, `core/src/test`). 22 translated locale folders plus
`values-night`; **no Arabic locale exists yet** (`values-ar` absent).

Toolchain (`gradle.properties`, `gradle/libs.versions.toml`, `docs/BUILDING.md`):
JDK 17 · Gradle **9.8.0** (wrapper) · AGP **9.4.1** · Kotlin **2.4.20** · KSP 2.3.12 ·
`compileSdk 37`, `targetSdk 35`, `minSdk 26` · namespace `app.vela`, `applicationId app.vela`.

Key dependencies the app actually ships: MapLibre Native `android-sdk-opengl` 13.6.1 (renderer),
Compose BOM 2025.01.00 + Material3 1.4.0-alpha06, Navigation-Compose 2.8.5, Hilt 2.60.1,
DataStore 1.1.1, Coil 2.7.0, OkHttp 5.5.0, Mozilla Rhino (remote style transforms), AndroidX Car
App 1.4.0, core-splashscreen 1.2.0, profileinstaller, commons-compress 1.28.0.

**Not in git** (gitignored prebuilt binaries a fresh clone must fetch, per `docs/BUILDING.md` and
`.github/workflows/ci.yml`):

| File | Purpose | Size | Present here |
|---|---|---|---|
| `app/libs/sherpa-onnx-1.13.3.aar` | neural TTS runtime (Kokoro/Piper voices) | ~57 MB | ❌ |
| `app/libs/cronet-<155.0.8059.16>.aar` | Chromium network stack for Google requests | ~15 MB | ❌ |
| `core/libs/osmand-java.jar` | offline router | >10 MB | ❌ |
| `core/libs/osmand-shared-jvm.jar`, `gnu-trove-osmand.jar`, `kxml2-vela.jar` | obf reader et al. | - | ❌ |

`:app` cannot be packaged without the first two and `:core` cannot compile without the jars, so
these are a hard prerequisite for any APK, here or on any machine.

All essential Vela features are intact in the tree at this point (online maps, light/dark styles,
GPS, routing, blue route line, visual turn guidance, search, saved places, offline area/country
downloads, offline routing). Nothing was removed, renamed or downgraded in Phase 1 , the only
source edit is the repair described in §5.

## 4. Baseline build: the exact blockers

What I ran, and what came back:

```
$ java -version                      → bash: java: command not found     (no JDK; /usr/lib/jvm absent)
$ apt-get download openjdk-17-jdk-headless
                                     → E: Unable to locate package openjdk-17-jdk-headless
$ echo $ANDROID_HOME                 → (empty; no Android SDK, no sdkmanager, no adb)
$ ./gradlew --version                → would need services.gradle.org (blocked, see table)
```

Network reachability from this sandbox (HTTP status / curl error, checked live):

| Host | Needed for | Result |
|---|---|---|
| `github.com`, `api.github.com`, `codeload.github.com` | git, `gh` | ✅ 200 / 301 |
| `registry.npmjs.org`, `pypi.org` | tooling | ✅ 200 / 404 |
| `services.gradle.org` | the Gradle 9.8.0 wrapper distribution | ❌ no connection |
| `dl.google.com` | Android SDK platform 37, build-tools, **Google Maven** (AGP, AndroidX, Compose, MapLibre's peers) | ❌ no connection |
| `repo.maven.apache.org`, `repo1.maven.org`, `plugins.gradle.org`, `jitpack.io` | every Maven dependency | ❌ no connection |
| `adoptium.net`, `api.adoptium.net` | installing a JDK | ❌ no connection |
| `release-assets.githubusercontent.com` | **GitHub release assets** , where the vendored AAR/JAR above live | ❌ `curl: (35) OpenSSL SSL_connect: SSL_ERROR_SYSCALL` |

Consequences, stated plainly:

- `./gradlew` cannot even bootstrap (no distribution, no JDK).
- Dependency resolution (AGP, Kotlin, Compose, Hilt, MapLibre, OkHttp, …) cannot happen.
- `compileSdk 37` cannot be installed, so no resource compilation (`aapt2`) either.
- The vendored native binaries cannot be downloaded here, because GitHub *release assets* redirect
  to a blocked host even though the GitHub API works.
- Even with a toolchain, this box has **2 vCPU / 3.8 GB RAM**; a Vela release build (R8 +
  resource shrinking + baseline profile + MapLibre + sherpa `.so`) realistically needs 8 GB+ and
  would likely OOM.

**So: no `assembleDebug`, no `assembleRelease`, no unit tests, no lint, and no APK can run in this
environment.** That is a capability limit of the sandbox, not a judgment about the project.

## 5. Defect found and fixed (commit `5a6ed52`)

`d7b88c6` ("Adopt MAP-RMG royal-blue and gold palette") was meant to change colours and the app
name. It also , certainly by accident , replaced `app/src/main/res/values/strings.xml` with a
35-line stub:

| | before `d7b88c6` (= `main`) | after `d7b88c6` | now |
|---|---|---|---|
| `<string>` keys in `values/strings.xml` | 1,354 | **31** | 1,354 |
| `R.string` references in `:app`/`:core` with no default value | 0 | **1,260** | 0 |
| `R.*` name with no definition in *any* locale (uncompilable) | 0 | 37 | 0 |

The translated locales (`values-de`, `values-es`, … 1,250+ keys each) were untouched, so the app
would have compiled in most cases but shown **blank/English-fallback text for ~1,260 UI strings**,
and failed outright on the 37 keys absent everywhere. My fix restores the file from `main` and
re-applies exactly what the branding commit intended - `app_name` and `app_name_generic` =
`MAP-RMG` , and nothing else. Your palette commit and app-name choice are preserved verbatim; no
branding was reverted.

## 6. Verification actually run in Phase 1 (and results)

Because compilation is off the table here, I ran every device-free and toolchain-free check the
repo ships, plus a resource-reference check I wrote for this purpose
(`/home/user/.work/res_check.py`):

| Check | Result |
|---|---|
| `python3 tools/check-translations.py` (real CI step) | ✅ `Translation check OK: 1371 translatable keys, no placeholder drift` (gaps listed for some locales are by design: English falls back until translators fill them) |
| `bash dpad_test_suite/audit_static.sh` (real CI step) | ✅ `VIOLATIONS: none` (12 triage notes, pre-existing) |
| `res_check.py` , parse **every** XML resource + `AndroidManifest.xml` | ✅ all well-formed |
| `res_check.py` , resolve **every** `R.<type>.<name>` in Kotlin/Java | ✅ all resolve (1,354 strings, 22 plurals, 18 drawables, 1 font, 1 mipmap, 5 xml, 1 style, 2 dimens) |
| `git diff main..HEAD` review | ✅ exactly 2 files: `Color.kt` (palette) + `strings.xml` (branding + repair) |

I will re-run this same triple after every later phase, and I will run the real Gradle commands the
moment a toolchain is available (§7).

## 7. Branding inventory: what already exists vs. what Phases 2–6 must add

**Already committed (kept, not duplicated):**

- `ui/theme/Color.kt`: `VelaTeal = 0xFF2457D6` (royal blue), `VelaTealDark = 0xFF1A44B0`,
  `VelaTealLight = 0xFF6595F6`, `VelaAmber = 0xFFD4AF37` (gold accents).
- `app_name` / `app_name_generic` = `MAP-RMG` (`translatable="false"`).

**Still to do:**

| Phase | Missing piece | Where it will go |
|---|---|---|
| 2 | Unique `applicationId` (currently `app.vela`), Kotlin namespaces stay `app.vela*` | `app/build.gradle.kts` |
| 2 | MAP-RMG launcher icon (adaptive + legacy densities, monochrome) | `res/mipmap-*`, `res/drawable/ic_launcher_*` , today these are still Vela's mark, and only the `anydpi-v26` adaptive XML exists (no per-density legacy assets) |
| 2 | Splash screen carrying the logo + the name | `values/themes.xml` + `installSplashScreen()` in `MainActivity.kt` (already wired for the default AOSP splash) |
| 2 | `#0B1C3D` dark blue and the gold `#D4AF37` are defined but barely used yet | theme |
| 3 | Arabic: `values-ar` (1,354 keys + 22 plurals), Arabic nav/turn strings, RTL polish, Arabic as the default language | `res/values-ar`, `AppLocale`, `:core` `NavStrings` |
| 4 | White + gold Settings cards; hide About / Voice / Navigation / Diagnostics from the hub **and** from settings search | `settings/SettingsHub.kt`, `SettingsRoutes.kt`, `sections/*` |
| 5 | The three animated onboarding screens (التالية) + first-launch-only completion flag | `ui/WelcomeScreen.kt` / `ui/Onboarding.kt` area |
| 6 | Spoken directions off (visual guidance and routing untouched), regression pass, APK | `:core` voice config |

Note: `android:supportsRtl="true"` is already set in the manifest, and `AppLocale` already
supports an in-app language override - Arabic slots into both without redesign.

## 8. How we get a real APK (pick one; §9 asks you)

| Option | What it needs | Who does it |
|---|---|---|
| **A. GitHub Actions on your fork** | Enable Actions on `sajjad1566/MAP-RMG` (fork, currently 0 workflows). I add a workflow on this branch that fetches the vendored binaries (with the upstream-fallback URLs `ci.yml` already uses) and runs `:app:assembleRelease`, uploading the APK as a build artifact. | me + one click from you |
| **B. Local Android Studio / CLI** | JDK 17 + Android SDK 37 + the four vendored files from `docs/BUILDING.md`, then `./gradlew :app:assembleRelease`. | you |
| **C. A build sandbox with network** | A machine (or permission to use one) where `dl.google.com`, Maven Central and GitHub release assets are reachable. | you / infra |

In all three cases the code must first be correct , which is what Phases 2–6 produce, each verified
with the §6 triple plus a full diff review.

## 9. Decisions I need before Phase 2

1. **Build/APK route** - Option A, B or C above (or a combination).
2. **The logo artwork** - I need the exact original MAP-RMG file (PNG/SVG, ideally ≥1024 px square).
   Nothing in the repo or this workspace carries it; today's launcher assets are Vela's. Without it
   I cannot satisfy "do not redesign the artwork".
3. **Application ID** , a unique ID is required; the Kotlin namespaces stay `app.vela*` regardless.

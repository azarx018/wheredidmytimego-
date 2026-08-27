# TimeTrace

Privacy-first Android app that shows you where your phone time goes.
Kotlin + Jetpack Compose + Material 3. No account, no cloud, no analytics,
no internet permission at all — fully offline.

## ⚠️ Build status: UNVERIFIED

This project has **no local build environment** — by design, since the
developer has no PC. All build/test/lint verification happens on
**GitHub Actions** (`.github/workflows/android-build.yml`). I have not yet
seen a real, green run of that workflow, because doing so requires the code
to actually live in a GitHub repository, which only you can create.

Everything below marked with a version number has been checked against
current official documentation (Android Developers, Kotlin, Gradle) as of
**Aug 2026**, but the *only* real proof this all fits together is a live CI
run. Treat the first few runs as a debugging session, not a done deal —
see "What to do if the build fails" below.

## How to get this building (no PC required)

You have two good options:

**Option A — GitHub web upload (simplest)**
1. Create a new repository on github.com (from your phone/tablet browser is fine).
2. Use "Add file → Upload files" and drag in the whole `TimeTrace` folder
   from this download. GitHub's web uploader supports nested folders.
3. Commit directly to `main`.
4. Go to the **Actions** tab — the `Android Build` workflow starts
   automatically. Watch it run.

**Option B — GitHub Codespaces (a real dev environment in your browser)**
1. Create an empty repository on github.com.
2. Open it, click **Code → Codespaces → Create codespace on main**. This
   gives you a full Linux VM with a terminal, accessible from any browser —
   effectively a temporary "PC" for pushing code.
3. Upload/copy this project's files into the codespace, then:
   ```bash
   git add -A
   git commit -m "Initial TimeTrace skeleton"
   git push
   ```
4. Go to the **Actions** tab to watch the build.

Either way, once the workflow finishes:
- Open the workflow run → **Summary** → **Artifacts** →
  `timetrace-debug-apk` to download a real, installable debug APK.
- Sideload it onto an Android 12+ device to try the app.

## What to do if the build fails

Send me (Claude) the **Actions log** — specifically, open the failing step
and copy the red error text. Gradle/AGP/KSP version mismatches are the most
likely first failure (this ecosystem moved fast in 2026 — see below), and
they're normally a one-line version bump to fix once I can see the actual
error. I'll update the Gradle files and you re-push.

## Toolchain versions (and why)

AGP 9 shipped **built-in Kotlin support** in Jan 2026, which is a genuinely
breaking change from the AGP 8.x setup most tutorials still show:

| Component | Version used | Why |
|---|---|---|
| Android Gradle Plugin | 9.3.0 | Latest stable per [developer.android.com/build/releases/agp-9-3-0-release-notes](https://developer.android.com/build/releases/agp-9-3-0-release-notes) (July 2026). Built-in Kotlin means `org.jetbrains.kotlin.android` is **not** applied — applying it alongside AGP 9 fails the build. |
| Kotlin (Compose compiler plugin) | 2.4.0 | Compose now needs its own `org.jetbrains.kotlin.plugin.compose` Gradle plugin even with built-in Kotlin, since the Compose compiler is a separate Kotlin compiler plugin, not part of AGP. |
| KSP (for Room) | 2.4.0-2.0.4 | KSP supports AGP 9 built-in Kotlin from 2.3.1+. Needed to generate Room's DAOs/database. |
| Gradle | 9.5.1 | Latest stable; AGP 9.x requires Gradle 9.1.0+. |
| JDK | 21 (Temurin) | Per your instructions; AGP 9 requires JDK 17+, 21 is current LTS. |
| `compileSdk` / `targetSdk` | 36 | Matches build-tools already preinstalled on `ubuntu-latest` runners (avoids an extra SDK download step). |
| `minSdk` | 31 | Your earlier choice (Android 12+, smallest/most modern baseline). |

No Gradle Wrapper (`gradlew`) is committed — I can't generate the wrapper's
binary launcher jar in the sandboxed environment this project was written
in. The workflow uses `gradle/actions/setup-gradle` to install Gradle 9.5.1
directly instead, which is a supported, documented alternative to the
wrapper for CI. If you'd like a committed wrapper for local/Codespaces use
later, that's a one-time `gradle wrapper --gradle-version 9.5.1` run inside
a Codespace, then commit the result.

## Status: Phase 1–6 done (per brief's MVP order) + crash-hardening + visual polish pass

All of Phases 1–5 (previous section, unchanged) plus:

- ✅ **Export data**: Settings → Export data lets the user pick a save location (Storage Access Framework - no broad storage permission needed) and writes a JSON snapshot of categories, goals, coding-app flags, and a 30-day *aggregated* daily-total history. Deliberately exports totals, not raw per-session data - a portable report, not a full activity dump.
- ✅ **Visual/animation polish**, styled after three references you gave:
  - **Digital Wellbeing** (structure): Dashboard's plain number replaced with a category-colored donut ring (Canvas-drawn, sweeps in on load) with the day's total centered inside - same structural idea as Digital Wellbeing's dashboard.
  - **Daylio** (personality/history): category rows in Statistics are now colored, rounded chips (tinted background + accent color) instead of a plain dot+text row; list items across Dashboard/Apps/Statistics stagger in with a fade+slide instead of popping in all at once.
  - **Toggl Track** (time tracking): progress bars (app rows, goals) animate smoothly to their value instead of snapping; a pulsing "Coding session running" banner appears on the Dashboard whenever a session is active, tappable straight through to the live timer - so a running session is never out of sight, Toggl-style.
  - Screen-to-screen navigation now has a subtle fade+slide transition instead of an instant cut.
  - **Full coverage**: Categories (colored tint per assigned category, Daylio-style), Coding apps (row tints to the "on" color when marked as a coding app), and Coding Session (a Toggl-style rotating "recording" ring around the live timer instead of a static number) now match the same treatment as Dashboard/Apps/Timeline/App Detail/Statistics/Goals.

## Crash fix (Aug 2026 field report: Timeline crashed on a real Android 14 device)

Root-caused via code review (no stack trace was available - the tester has no PC/logcat access):

1. **`resolveAppLabel` only caught `NameNotFoundException`.** Android 11+ package-visibility restrictions can make `PackageManager` throw `SecurityException` or other transient exceptions for certain packages; those went uncaught and crashed the whole app. Broadened to catch `Exception` generally.
2. **Timeline showed raw, unfiltered sessions** — including background/system components (keyboard, launcher, SystemUI) that Apps/Dashboard already filter out via `resolveAppLabel`. Timeline now goes through the same filter, both for correctness and to reduce exposure to bug #1.
3. **`UsageStatsProvider.getSessions()` had no try/catch** around the raw event-log walk. Now wrapped - a flaky `system_server` returns an empty list instead of crashing.
4. **No ViewModel anywhere caught exceptions from repository calls.** A bare `viewModelScope.launch { }` lets any uncaught exception kill the process. Added a `safeLaunch` helper (`util/SafeLaunch.kt`) used by every ViewModel, paired with a reusable `ErrorState` composable ("Something went wrong — Retry") so any *future* unexpected failure degrades gracefully instead of crashing.

If a crash happens again, the most useful thing you can send is the actual stack trace - check if your phone's "App keeps stopping" dialog has a "Show details" / "Send feedback" option, since that usually includes it.

## Phase 1–5 recap (unchanged from before this polish pass)

- Project setup, Gradle config tuned for a small APK, CI-buildable
- Material 3 theme (dark-first), theme override (System/Light/Dark) via Settings
- Bottom nav + Navigation Compose scaffold, Onboarding / Usage Access flow
- `UsageStatsManager` integration — reads real session data, no mock data
- Dashboard, Apps, App Detail, Timeline, Statistics, Categories, Settings — all functional
- Goals, Coding Session, Notifications, Replay My Day — all functional
- Every screen's data loading is exception-safe (`safeLaunch` + `ErrorState`)

## Why this should build into a small, light APK

| Decision | Effect |
|---|---|
| `minSdk = 31` | No AndroidX backport/compat code paths |
| No DI framework (Hilt/Dagger) | Skips annotation-processor-generated code and its runtime |
| `material-icons-core` only, not `-extended` | Saves the thousands of unused icons in the extended pack |
| Vector drawables for the launcher icon | No bitmap mipmaps to ship per density |
| Platform Splash Screen API (`android:windowSplashScreen*`) | No `androidx.core:core-splashscreen` dependency |
| No charting library | Statistics screen (Phase 4) will use Canvas directly |
| `isMinifyEnabled` + `isShrinkResources` on release | R8 strips unused code/resources |
| ABI splits configured | Per-architecture APKs instead of one fat universal APK |
| No `INTERNET` permission | Also a visible privacy signal, not just smaller manifest |
| Usage data reconstructed on demand from `UsageStatsManager`, not duplicated into Room | Smaller database, always in sync with the OS source of truth |
| No polling — data loads on screen entry / explicit refresh only | Lower CPU/battery, per brief section 21 |

## Package layout

```
data/local/      Room database, entities, DAOs (user config only)
data/repository/ UsageRepository - combines UsageStatsManager + PackageManager + Room
data/usage/      UsageStatsProvider - raw UsageStatsManager wrapper
domain/model/    Plain Kotlin models used by the UI layer
ui/navigation/   NavHost, destinations, bottom bar
ui/screens/      One package per screen (dashboard, apps, timeline, ...)
ui/components/   Small reusable composables
ui/theme/        Color, typography, Material 3 theme
util/            Formatting + permission helpers
```

## Fixes since the first green build

- **KSP plugin version**: `2.4.0-2.0.4` doesn't exist — KSP's versioning decoupled from Kotlin's as of 2.3.x (no more `kotlinVersion-kspVersion` suffix). Now pinned to `2.3.11`, the latest plain release on Maven Central.
- **Icon pack**: switched from `material-icons-core` to `material-icons-extended`, since a few icons used across screens (`Schedule`, `BarChart`, `ChevronRight`, `Android`) aren't confirmed to be in the small core set, and guessing wrong breaks the build. R8 still strips unused icons from the release APK, so this only affects debug artifact size.
- **`ExperimentalMaterial3Api` opt-in**: `SegmentedButton`/`SingleChoiceSegmentedButtonRow` and `DatePicker` are still marked experimental in this Compose BOM. Opted in once at the module level in `app/build.gradle.kts` rather than annotating every screen file.
- **Timeline crash on Android 14** — see the dedicated section above.

## Fixes since the polish pass (compile errors from the next CI run)

- **Missing `getValue` import**: `by animateFloatAsState(...)` in `AppUsageRow.kt` and `AppListItem.kt` needs `import androidx.compose.runtime.getValue` for the `by` delegate to resolve against `State<Float>`. Added.
- **`MaterialTheme` called inside `Canvas { }`**: `UsageRingChart.kt` read `MaterialTheme.colorScheme.primary` inside the Canvas draw lambda (a `DrawScope`, not a `@Composable` context) as a fallback color. Moved that read outside the `Canvas` block, into the enclosing composable, before it's used inside the lambda.
- Scanned the rest of the codebase for both patterns afterward - no other occurrences.
- **Timeline and App Detail now match the Dashboard/Apps/Statistics polish**: Timeline gained a Daylio-style connecting line behind each entry's icon (using `Modifier.height(IntrinsicSize.Min)` on the row so the connector can safely `fillMaxHeight()` without an infinite-constraints crash) plus staggered entrance; App Detail's session list now staggers in the same way.

## Remaining known gaps

- No unit/instrumented tests yet beyond what `gradle testDebugUnitTest` runs by default (i.e. none written) - the CI workflow runs the task, but there's nothing there to catch regressions before a human hits them.
- Notification icon is a placeholder system drawable (`android.R.drawable.ic_dialog_info`), not a custom TimeTrace icon.
- Polish now covers every screen: Dashboard, Apps, Timeline, App Detail, Statistics, Categories, Coding apps, Coding Session, Goals, plus global navigation transitions. Replay already had its own animated reveal from Phase 5.
- The brief's MVP Definition of Done (section 25) is now functionally complete end-to-end, but **still UNVERIFIED on a real green CI run with all of this code** - the last confirmed-green run predates Phase 3 onward.

Once you've had a chance to rebuild and test on-device again, let me know how it goes and whether the visual direction lands the way you pictured it.

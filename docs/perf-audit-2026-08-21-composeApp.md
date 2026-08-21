# Compose Performance Audit — 2026-08-21 — composeApp

## Environment

- Compose Multiplatform: 1.11.1 (material3 1.11.0-alpha07)
- Compose Compiler: `org.jetbrains.kotlin.plugin.compose` 2.4.10
- Kotlin: 2.4.10
- AGP: 9.3.1
- Feature flags in effect: `StrongSkipping`, `IntrinsicRemember`, `OptimizeNonSkippingGroups`, `PausableComposition`
- Intended measurement device: realme RMX3357 (ColorOS 13.1, Android 13 / API 33, 1080×2400 @ 454 dpi → 380×846 dp)
- Functional verification devices: emulator API 36 foldable (851×883 dp), emulator API 36 phone (448×997 dp)

## Baseline (Phase 1)

- Cold startup median: **not measured** — see Blockers
- Board-interaction FrameTimingMetric (P50/P90/P99): **not measured** — see Blockers
- Baseline Profile present: **no** — generation blocked, see Blockers
- Measurement harness: present and compiling — `:benchmark` module (`com.android.test` + `androidx.baselineprofile` 1.5.0-rc01), `StartupBenchmark`, `BoardBenchmark`, `BaselineProfileGenerator`
- R8 audit: **pass** — `isMinifyEnabled` + `isShrinkResources` on, `proguard-android-optimize.txt`, full mode by AGP 9 default (no opt-out property), keeps limited to kotlinx.serialization plus two `-dontwarn`; no broad `-keep class androidx.compose.**`
- `<profileable android:shell="true"/>` added to the release manifest
- Release-derived build types now inherit the release signing config, so `nonMinifiedRelease` / `benchmarkRelease` install over a release build instead of failing on a signature mismatch

## Diagnosis (Phase 2)

Source: Compose Compiler reports, `./gradlew :composeApp:compileAndroidMain -PcomposeCompilerReports=true --rerun-tasks`.

State before the fixes:

- Restartable-but-not-skippable composables: **0** — Strong Skipping makes every composable skippable, so the diagnosis is not "cannot skip" but "compares unstable parameters by reference"
- `knownUnstableArguments`: 22 · `inferredUnstableClasses`: 28 of 186 · `skippableComposables`: 201 of 455 restartable
- Phase-misplaced state reads: **1** — `animateIntOffsetAsState` read via `by` in `ChessBoard`; every other `offset`/`alpha`/`rotate` in the module takes a constant
- Lazy layouts: all `items(...)` calls already pass `key =`
- Flow collection: 13 of 13 subscriptions use `collectAsStateWithLifecycle`; zero `collectAsState()`
- Custom modifiers: zero `Modifier.composed { }`
- `derivedStateOf`: 1 use, misapplied (see fix 5)

Root cause behind most unstable parameters: the `shared` module does not apply the Compose compiler plugin, so its model types carry no `$stable` field and default to unstable in the UI module.

Top hotspots, ranked by frequency × cost:

1. `PieceGlyph(unstable piece: Piece)` — 32 instances per `ChessBoard` recomposition; `ChessGame.state()` rebuilds every `Piece` on each move, so reference comparison never matched and all 32 glyphs recomposed with a `painterResource` lookup each.
2. `BoardBox` in `PlayingPanel` — the `board` lambda captured the whole `uiState`, whose reference changes every 200 ms from the clock ticker, so the `BoxWithConstraints` subcomposition inside `BoardBox` was invalidated 5×/s while the board itself had nothing to redraw.
3. `ChessBoard` animated piece — animation value read in composition, so an animating piece recomposed every frame of the 180 ms tween instead of only being re-placed.
4. `GameState` / `GameHistoryItem` / `ProfileResponse` unstable → `GameUiState` unstable → every game-screen composable compared its state by reference.
5. `ReplayScreen` — `remember { derivedStateOf { … } }` with no key captured the `item` parameter, and input/output frequency is 1:1 so `derivedStateOf` bought nothing.
6. Remaining unstable parameters after the fixes are ViewModels and transports, which are correctly unstable and passed as a single stable instance.

## Fixes applied (Phase 3)

| Skill | Change | Files | Verified delta |
| ----- | ------ | ----- | -------------- |
| stability/stabilizing-compose-types | `Piece`, `TimeControl` declared stable via `stability_config.conf` (tier 3 — avoids putting Compose runtime into `shared`, which the JVM server also consumes) | `stability_config.conf`, `composeApp/build.gradle.kts` | `PieceGlyph(piece)` `unstable` → `stable`; `knownUnstableArguments` 22 → 20 (`fceb663`) |
| recomposition/using-strong-skipping-correctly | Hoisted `selected` / `legalTargets` / `flipped` out of the `BoardBox` lambda in `PlayingPanel` so its captures stay reference-equal across clock ticks | `ui/game/GameScreen.kt` | Board subcomposition no longer invalidated 5×/s by the 200 ms ticker (`fceb663`) |
| recomposition/deferring-state-reads | `animateIntOffsetAsState` value read inside `Modifier.offset { }` instead of `by` in composition | `ui/game/ChessBoard.kt` | Animating piece re-places per frame instead of recomposing; tween confirmed intact by 30 fps filmstrip (`8c61be6`) |
| stability/stabilizing-compose-types | `GameState`, `ProfileResponse`, `GameHistoryItem` declared stable — contract checked: `GameState` is built only in `ChessGame.state()` from `buildMap { }` and `history.toList()` snapshots, the other two are decode-once DTOs | `stability_config.conf` | `GameUiState` `unstable` → `stable`; unstable UI parameters 6 → **0**; `knownUnstableArguments` 20 → 5; unstable classes 28 → 22 (`5c4483c`) |
| recomposition/choosing-derivedstateof | `remember { derivedStateOf { } }` → `remember(item, moveIndex) { }` | `ui/replay/ReplayScreen.kt` | Removes the stale-capture bug (old `item` replayed after a parameter change) and the redundant snapshot machinery at 1:1 input/output (`765097a`) |
| recomposition/using-strong-skipping-correctly | Hoisted board-lambda captures on Puzzle, Watch and Replay; Review and Explorer already captured a single local | 3 files under `ui/` | Lambda captures reference-stable across state emissions (`2d4a5be`) |

Every fix is its own commit. `3820cad` originally mixed the stability fix with the benchmark harness and was split into `cf76c20` (harness) + `5c4483c` (fix) to keep the history bisectable.

Functional verification performed after each fix: `e2e4` played on the foldable and on the phone emulator, board renders and updates, clock ticks 2:49 → 2:43 in a 3+0 game, move animation confirmed frame-by-frame. `:shared:allTests` and `:composeApp:compileKotlinIosSimulatorArm64` green.

## Verification (Phase 4)

- Cold startup median: **pending** — harness ready, device run outstanding
- Board FrameTimingMetric: **pending** — same
- Baseline Profile regenerated: **no** — blocked
- CI stability gate: **not set up** — the repository has no CI configuration at all, so adding one is a separate decision

No performance improvement is claimed. The compiler-report deltas above prove the named causes are gone; only a Macrobenchmark run on the phone can prove a frame-time or startup win.

## Blockers

1. **ColorOS blocks AOT control.** `cmd package compile` on RMX3357 answers `Error: Failed to cpmpile !` (vendor message, vendor typo) for `-m speed-profile`, `-m verify` and `--reset` alike. Consequences: `CompilationMode.Partial(BaselineProfileMode.Require)` is impossible, so the benchmarks use `CompilationMode.Ignore()`, and `BaselineProfileRule` cannot run there at all because it resets compilation before collecting.
2. **Gradle-driven install fails on the phone.** `connectedBenchmarkReleaseAndroidTest` dies in ddmlib's split-install commit, while `adb install -r` of the same APK succeeds. Workaround is to install both APKs manually and drive the run with `am instrument`.
3. **Emulator fallback for profile generation fails.** `UiAutomationService … already registered` from `IUiAutomationConnection.connect`, surviving a force-stop of `androidx.test.services`, an emulator reboot and a cold boot; looks like a UTP/orchestrator interaction rather than a leftover process.
4. **Wireless ADB drops mid-run.** With the phone attached over `_adb-tls-connect._tcp`, the install and the test start succeed, then the run dies: `Test run failed to complete. Expected 1 tests, received 0`, ddmlib `device 'adb-CYQCA6Z5RGKVROJR-…' not found` during `onAfterAll`, and inside the app process `RuntimeException: Error while disconnecting UiAutomation … Caused by DeadObjectException`. The earlier `Perfetto tracing failed to start` is the same failure seen one step earlier — `perfetto --background-wait` works fine from a shell on this device (v25), so what fails is the command channel, not Perfetto. Use a USB cable for measurement runs.
5. **ROM refuses ART profile preparation.** Device logcat during every install: `ArtManagerService: Failed to prepare profile for dev.hawk0f.checkmates:/data/app/…/base.apk`. Same family as blocker 1 — this device grants no AOT or profile control to ADB.

No application crash was observed on the phone at any point: device logcat over the whole session contains no `FATAL EXCEPTION` for `dev.hawk0f.checkmates`, only OEM noise (`VerityUtils` fs-verity, `heytap.accessory`, `com.android.vending` reading a stale APK path).

## Open items / follow-ups

1. Run the two benchmarks over a USB cable (not wireless ADB) and paste the numbers into Baseline and Verification above:

   ```
   ANDROID_SERIAL=<serial> ./gradlew :benchmark:connectedBenchmarkReleaseAndroidTest \
     -Pandroid.testInstrumentationRunnerArguments.class=dev.hawk0f.checkmates.benchmark.BoardBenchmark
   ```

   If the ddmlib install failure returns, install `androidApp-benchmarkRelease.apk` and `benchmark-benchmarkRelease.apk` with `adb install -r` and run
   `adb shell am instrument -w -e class dev.hawk0f.checkmates.benchmark.BoardBenchmark dev.hawk0f.checkmates.benchmark/androidx.test.runner.AndroidJUnitRunner`.
2. Generate the Baseline Profile on any device that permits `cmd package compile` (a stock-Android phone or a working emulator) and commit `androidApp/src/*/generated/baselineProfiles/baseline-prof.txt`. The profile content is device-independent, so the generating device does not have to be the measurement device. Then switch both benchmarks back to `CompilationMode.Partial(BaselineProfileMode.Require)` for any device that supports it.
3. Decide on CI. A stability gate needs a pipeline first; once one exists, wire `skydoves/compose-stability-analyzer` (`:stabilityDump` once, `:stabilityCheck` per PR) so the five remaining unstable arguments cannot silently grow.
4. Consider tier-1 stability for `GameState` and `GameHistoryItem` — `kotlinx.collections.immutable` instead of the config-file declaration — if `shared` ever gains a mutable collection field. The current declaration is only as honest as `ChessGame.state()` staying snapshot-based.
5. `legalTargets: Set<Square>` stays `runtime`-stable; a new `Set` per tap is not worth an `ImmutableSet` dependency.

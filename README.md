# CheckMates

Kotlin Multiplatform chess app — pass & play, Bluetooth LE, computer play, puzzles and online play
through an own Ktor server. Android and iOS share one Compose
Multiplatform UI; the rules engine and the wire protocol are shared with the server too.

## Modules

| Module | What lives there |
| --- | --- |
| `shared` | Rules engine (`ChessGame` over kchesslib), SAN formatting, the `GameMessage` protocol, shared utils. Used by the clients *and* the server. |
| `composeApp` | The whole UI and all client logic: screens, view models, BLE transport and REST/WebSocket clients. `androidMain` + `iosMain` for platform bits. |
| `androidApp` | Android application shell: `MainActivity`, deep links, FCM service, R8/baseline-profile setup. |
| `iosApp` | Xcode project that hosts the shared Compose UI. |
| `server` | Ktor server: accounts, game history, room registry, WebSocket relay, landing page, `.well-known` app links. |
| `benchmark` | Macrobenchmark module; also generates the baseline profile consumed by `androidApp`. |
| `deploy` | Dockerfile, compose files and the Caddy reverse-proxy config for the VPS. |

## Requirements

- JDK 21 (`brew install openjdk@21`) — the server module and the Gradle toolchain both target 21.
- Android SDK with API 37 (`android-compileSdk` in `gradle/libs.versions.toml`), min SDK 31.
- Xcode 16+ for the iOS target.

`local.properties` needs `sdk.dir` pointing at the Android SDK.

## Build and run

Android (debug):

```bash
./gradlew :androidApp:installDebug
```

Android (release, R8 + baseline profile — signing is optional, see below):

```bash
./gradlew :androidApp:assembleRelease
```

Server, locally on port 8080:

```bash
./gradlew :server:run
```

iOS: open `iosApp/iosApp.xcodeproj` in Xcode and run, or just compile the shared framework:

```bash
./gradlew :composeApp:compileKotlinIosSimulatorArm64
```

To point the app at a local server instead of production, set
`ServerConfig.baseUrl = ServerConfig.LOCAL_URL` (see
[ServerConfig.kt](composeApp/src/commonMain/kotlin/dev/hawk0f/checkmates/net/ServerConfig.kt)).
On the Android emulator use `http://10.0.2.2:8080`, not `localhost`.

## Tests and checks

```bash
./gradlew ktlintCheck :shared:jvmTest :server:test :composeApp:testAndroidHostTest
```

- `:shared:jvmTest` — rules engine: castling, promotion, draws, SAN.
- `:server:test` — account API, session expiry, the game-record trust boundary.
- `:composeApp:testAndroidHostTest` — view-model tests (board selection, castling by rook tap,
  promotion, takeback).
- `ktlintCheck` — code style; `./gradlew ktlintFormat` fixes what it can. Style knobs live in
  [.editorconfig](.editorconfig) (IntelliJ code style, no forced trailing commas,
  `function-naming` off for `@Composable`s).

### Component previews

`ui/preview` holds a catalog of `PreviewSpec`s — one per UI element, board state, dialog or screen —
rendered both by `@Preview` functions (Android Studio, Fleet) and by Roborazzi screenshot tests, so a
preview and its golden never drift apart. Goldens live in
[composeApp/screenshots](composeApp/screenshots), one per spec per theme.

Every screen is in the catalog. Screens whose state comes from a network-backed view model are split
into a thin `XScreen(...)` that owns the view model and a stateless `XContent(uiState, callbacks)`
that the preview drives with fixtures — lobby, friends, leaderboard, nearby and profile.
Screens with a local view model (`GameScreen`, opening drill, puzzles, board editor)
take the view model as a parameter instead, so a preview passes one it built itself.

Replay is the one spec previewed without a golden — it prints a locale- and timezone-formatted date,
so its image differs per machine.

The catalog also runs on a device: the debug build ships a `PreviewGalleryActivity` that lists every
spec, renders it full screen and switches light/dark on the spot.

```bash
adb shell am start -n dev.hawk0f.checkmates/.PreviewGalleryActivity
```

`--es spec <id>` opens one spec directly and `--es mode system|light|dark` picks the theme, which makes
the gallery scriptable as a crash smoke test over the whole catalog.

```bash
adb shell am start -n dev.hawk0f.checkmates/.PreviewGalleryActivity --es spec screen-profile-account --es mode dark
```

```bash
./gradlew :composeApp:testAndroidHostTest -Proborazzi.test.verify=true
./gradlew :composeApp:testAndroidHostTest -Proborazzi.test.record=true
```

The first compares against the goldens, the second rewrites them after an intended visual change.
Each screenshot class pins one palette and one dark-mode setting: switching the theme between
captures inside a single Robolectric class leaves later captures a frame short, which drops the
board's pieces from the image.

CI runs the same set plus a release build and the iOS compile — see
[ci.yml](.github/workflows/ci.yml).

## Training

Completed games can be analysed locally in Replay. For inaccuracies, mistakes and blunders the
review can reopen the position before the move and ask the player to find the engine suggestion on
the board. The same position can be saved as a personal puzzle; personal puzzles join the bundled
set and use the same rating and spaced-repetition scheduler.

Each replay move can also carry a local text annotation. Notes are stored per game and ply and are
included as PGN comments when the game is shared, so an annotated review can be reopened in other
chess software without losing the player's ideas.

The opening trainer includes built-in lines and custom PGN main lines. Use **Import PGN line** on
the Openings screen, choose the trained colour and paste a legal PGN. The imported line is stored on
the device and can be drilled with the same automatic opponent and mistake tracking as built-in
lines.

Rematches form a best-of-three series. The game-over panel keeps wins and draws across games,
announces the series winner after two victories and resets the score when the next rematch starts.
Computer games offer balanced, attacking and positional styles independently from engine strength.

The client keeps the latest 50 completed games on the device, including pass-and-play, computer,
Bluetooth and online games. This archive is available without an account on Home and Profile and
opens in the same replay screen. The dedicated archive can search either player's name and filter by
local or remote mode and by personal result. After sign-in it is merged with server history; matching
local and server copies completed within one minute are shown once, with the server record taking precedence.

Signed-in friends can also play correspondence games. Positions and move history live in the
server database, every submitted move is replayed through the shared rules engine, and only the
player whose turn it is may move. The client lists parallel games, opens an interactive board and
sends a push notification to the opponent after each accepted move.

## Server configuration

All configuration is environment variables:

| Variable | Default | Meaning |
| --- | --- | --- |
| `PORT` | `8080` | HTTP port. |
| `PUBLIC_BASE_URL` | `http://localhost:8080` | Used for invite links and the landing page. |
| `DB_PATH` | `data/chess.db` | SQLite file; the directory is created on boot, WAL is enabled. |
| `FCM_CREDENTIALS` | `/app/fcm-key.json` | Service-account JSON for push notifications. |
| `ANDROID_CERT_FINGERPRINTS` | — | Comma-separated SHA-256 signing fingerprints for `assetlinks.json`. |
| `APPLE_TEAM_ID` | `TEAMID_PLACEHOLDER` | For `apple-app-site-association`. |

The schema migrates itself on startup (`schema_version` table, `PRAGMA table_info` checks), so
deploying a newer jar over an existing database is safe.

Sessions live 30 days and slide forward when used within 7 days of expiry; expired ones are
purged by a background loop.

### Trust boundary

`POST /api/me/games` only accepts client-owned modes (`hotseat`, `computer`, `ble`) and replays the whole
move list through the engine before storing it — a declared result that the replay contradicts
is rejected with `400 BAD_RECORD`. Online games are written by the server itself when the room
finishes, so clients cannot forge them.

Rate limits: 10/min on auth, 30/min on room creation, 60/min on uploads; request bodies are
capped at 256 KB and WebSocket frames at 64 KB.

## Deployment

The VPS runs the fat jar behind Caddy:

```bash
./gradlew :server:buildFatJar
scp server/build/libs/server-all.jar <host>:/opt/chess/
ssh <host> 'cd /opt/chess && docker compose -f docker-compose.yml up -d --force-recreate chess-server'
```

The production `/opt/chess/docker-compose.yml` mounts the jar, `data/` and `fcm-key.json`, and publishes only
on `127.0.0.1:8090`; Caddy terminates TLS for `chess.hawk0f.icu`.

## Signing

`androidApp/keystore.properties` is git-ignored and optional — without it the release build is
simply unsigned, which is what CI does. With it:

```properties
storeFile=/absolute/path/to/release.jks
storePassword=...
keyAlias=...
keyPassword=...
```

## Premoves

In remote games (online and Bluetooth) you can click moves while the opponent is thinking.
[PremovePlanner](shared/src/commonMain/kotlin/dev/hawk0f/checkmates/shared/domain/PremovePlanner.kt)
projects the queue by replaying it on the live position with the side-to-move flipped after each
entry, so up to `MAX_PREMOVES` of your own moves can be planned in a row and the board shows the
planned position with the premoved squares tinted. When the turn arrives the head of the queue is
replayed against the live board: legal moves are sent immediately, and a move the real position
rejects drops the whole queue. Takebacks, resyncs and game end drop it too.

Who executes the queue depends on the opponent:

| Game kind | Executed by | Clock cost |
| --- | --- | --- |
| `online` (own server) | server, via `setPremoves` | a flat `GameRoom.PREMOVE_ELAPSED_MILLIS` (100 ms) instead of real elapsed time, and the increment is still granted |
| `ble` | BLE host | 100 ms; the host owns and synchronizes both clocks |

For online games the client ships the whole queue to the server with `setPremoves` whenever it
changes. [GameRoom](server/src/main/kotlin/dev/hawk0f/checkmates/server/GameRoom.kt) validates the
plan against the projected position, stores it per colour and drains it inside the same lock that
applied the opponent's move, so a premove costs a flat 100 ms instead of a network round trip and
cascades if both players have one queued. A queue sent while it is already your own turn is not a
premove: its head is played through the normal move path and charged real elapsed time, so
`setPremoves` cannot be used to move for free. An invalid plan is answered with `premovesDropped("INVALID_PLAN")`, a head the live
board rejects with `premovesDropped("ILLEGAL_MOVE")`, and the client clears its queue on either.
A server that predates this protocol answers `protocolError` instead, and the client silently falls
back to executing premoves itself — so **online premoves only become instant after the server jar is
redeployed** (see [Deployment](#deployment)).

Bluetooth hosts can choose no clock, 3+2, 5+0 or 10+0 before advertising a game. The host sends the
control and exact millisecond snapshots through the compact BLE protocol, applies increments and
declares timeout. It also receives the pending premove queue in advance, so Bluetooth latency does
not count against a premove: each accepted premove costs exactly 100 ms.

## Copy and localization

All user-facing copy in `composeApp` lives in
[strings.xml](composeApp/src/commonMain/composeResources/values/strings.xml) and is read through
Compose resources (`stringResource(Res.string.…)`); adding a language means dropping a
`values-<lang>/strings.xml` next to it. Format arguments are positional (`%1$s`, `%1$d`), so word
order stays translatable.

Deliberately *not* resourced: chess notation, clock formats, API field names and endpoint labels
that the design shows on purpose, and palette names. Still hardcoded English: the error and status
messages produced inside view models — those need a `UiText` wrapper (raw string or
`StringResource` + args) in the UI state before they can be translated.

Board squares expose accessibility labels ("e4, white pawn") plus a state description (selected,
legal move, last move, king in check); piece images stay decorative so a screen reader announces
each square once. Icon-only buttons take a `contentDescription` through `CircleButton`.

## License

See [LICENSE](LICENSE).

# CheckMates

Kotlin Multiplatform chess app — pass & play, Bluetooth LE, online play through an own Ktor
server, and a Lichess client built on the Board API. Android and iOS share one Compose
Multiplatform UI; the rules engine and the wire protocol are shared with the server too.

## Modules

| Module | What lives there |
| --- | --- |
| `shared` | Rules engine (`ChessGame` over kchesslib), SAN formatting, the `GameMessage` protocol, shared utils. Used by the clients *and* the server. |
| `composeApp` | The whole UI and all client logic: screens, view models, BLE transport, REST/WebSocket clients, Lichess integration. `androidMain` + `iosMain` for platform bits. |
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

CI runs the same set plus a release build and the iOS compile — see
[ci.yml](.github/workflows/ci.yml).

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

`POST /api/me/games` only accepts client-owned modes (`hotseat`, `ble`) and replays the whole
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
ssh <host> 'cd /opt/chess && docker compose -f docker-compose.prod.yml up -d --force-recreate'
```

`deploy/docker-compose.prod.yml` mounts the jar, `data/` and `fcm-key.json`, and publishes only
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

In remote games (online, Bluetooth, Lichess) you can click moves while the opponent is thinking.
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
| `lichess` | client, one move per turn | one network round trip, charged by Lichess |
| `ble` | client | no clocks in Bluetooth games |

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

## Lichess integration

OAuth is PKCE with no client secret: client id `dev.hawk0f.checkmates`, redirect
`dev.hawk0f.checkmates://lichess-auth`, scopes `board:play challenge:read challenge:write
puzzle:read tournament:write follow:read`. The token is stored with multiplatform-settings, and
sign-in happens in the system browser, so no password ever reaches the app.

Rate limits are handled in [LichessRateLimit.kt](composeApp/src/commonMain/kotlin/dev/hawk0f/checkmates/net/lichess/LichessRateLimit.kt):
a 429 starts a cooldown (from `Retry-After`, else one minute, capped at ten) that every later
request waits out, and `HttpRequestRetry` retries 429/5xx up to three times.

Note the Board API forbids any engine assistance during a live game. That is why the in-game
panel shows a plain material balance instead of a cloud evaluation; cloud eval is only used in
post-game review.

## License

See [LICENSE](LICENSE).

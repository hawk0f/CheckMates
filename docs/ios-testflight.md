# Shipping CheckMates to TestFlight

The iOS app is the same Compose Multiplatform UI as Android, wrapped in `iosApp`. Everything below is a
one-time setup plus a single command (or one GitHub Actions run) per build.

## What you need once

1. An Apple Developer Program membership (99 USD/year) — required for TestFlight.
2. An App Store Connect record for the bundle id `dev.hawk0f.checkmates`.
   The Xcode project reads the id from `iosApp/Configuration/Config.xcconfig`, so keep the two in sync.
3. An App Store Connect API key (Users and Access → Integrations → App Store Connect API):
   - `ASC_KEY_ID` — the key id
   - `ASC_ISSUER_ID` — the issuer id shown above the key list
   - `ASC_KEY_CONTENT` — the downloaded `.p8` file, base64 encoded: `base64 -i AuthKey_XXXX.p8 | pbcopy`
4. Your team id in `iosApp/Configuration/Config.xcconfig`:

   ```
   TEAM_ID=ABCDE12345
   BUNDLE_ID=dev.hawk0f.checkmates
   APP_NAME=CheckMates
   ```

   `Config.xcconfig` is local configuration — do not commit a real team id.

## Building locally

```bash
cd iosApp && fastlane beta
```

The `beta` lane links the release framework with Gradle (`:composeApp:linkReleaseFrameworkIosArm64`),
bumps the build number to one above the newest TestFlight build, archives with
`-allowProvisioningUpdates`, and uploads through the API key. Set `CHANGELOG="…"` to change the note
testers see.

To check that the project still compiles without any signing material:

```bash
cd iosApp && fastlane build
```

## Building from CI

`.github/workflows/ios-testflight.yml` runs the same lane on `macos-latest` and is triggered manually
(Actions → iOS TestFlight → Run workflow). Add these repository secrets first:

| Secret | Value |
| --- | --- |
| `IOS_TEAM_ID` | Apple Developer team id |
| `IOS_APPLE_ID` | Apple ID email that owns the app record |
| `ASC_KEY_ID` | App Store Connect API key id |
| `ASC_ISSUER_ID` | App Store Connect issuer id |
| `ASC_KEY_CONTENT` | base64 of the `.p8` key |

The workflow uses `github.run_number` as the build number, so builds are always increasing.

## Version numbers

- Marketing version lives in `iosApp/iosApp/Info.plist` (`CFBundleShortVersionString`); bump it for a
  user-visible release.
- Build number is set by the lane, so it does not need to be committed.

## Known limits

- Push notifications need an APNs key registered in the same Apple team plus the Firebase iOS app;
  without it the app still runs, it just never receives a challenge notification.
- Bluetooth play requires a physical device — the simulator has no BLE stack.

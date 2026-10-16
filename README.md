# BlueCon

An Android BLE scanner and proximity viewer backed by a separate Kotlin Multiplatform SDK. Android API 24+ is the active platform. App copy, permission explanations, and diagnostics are in English. Light and dark themes follow the device setting.

## Modules

```text
androidApp
  Android entry point, permission UI, composition root, Room persistence
      -> shared
           Compose screens, ViewModel, use cases, repository contracts
               -> bluetooth-sdk
                    Portable models and signal processing, Android BLE adapter
```

- `bluetooth-sdk/commonMain`: scanner contract, `BleDevice`, advertising models, EMA, bounded RSSI history, proximity ranges, cache expiry.
- `bluetooth-sdk/androidMain`: Android scanner, advertising mapper, version-aware permission policy, adapter readiness, cleanup.
- `shared/domain/model`: history models and scanner sort options.
- `shared/domain/repository`: scanner and history contracts implemented by the data layer.
- `shared/domain/usecase`: individual scan, observation, filtering, sorting, and history operations.
- `shared/data`: SDK adapter implementing the application scanner repository.
- `shared/presentation`: ViewModel, screens, reusable controls, and semantic theme colors.
- `androidApp`: constructor injection through `AppContainer`, Activity Result permission flow, and Kotlin Room entities/DAO/database processed with KSP2.

The SDK has no Compose, Room, Activity, or DI framework dependency. iOS targets retain portable code, but no iOS BLE backend is implemented; the iOS entry point shows an availability message.

## Domain use cases

Each use case has one public `operator fun invoke` entry point and its own file. They run in `commonMain` without Android, Room, Compose, or ViewModel dependencies.

| Package | Use case | Responsibility |
| --- | --- | --- |
| `usecase/scanner` | `StartScanUseCase` | Start a foreground session with the two-minute application policy. |
| `usecase/scanner` | `StopScanUseCase` | Stop with the user-requested reason. |
| `usecase/scanner` | `StopScanOnBackgroundUseCase` | Stop when the host leaves the foreground. |
| `usecase/scanner` | `ObserveScannedDevicesUseCase` | Expose the current device observation stream. |
| `usecase/scanner` | `ObserveScanStateUseCase` | Expose scan status and blockers. |
| `usecase/scanner` | `FilterScannedDevicesUseCase` | Match name/address and minimum RSSI without reordering. |
| `usecase/scanner` | `SortScannedDevicesUseCase` | Apply the chosen ordering, stale-last policy, and address tie-breaker. |
| `usecase/history` | `ObserveDeviceHistoryUseCase` | Expose persisted history and storage errors. |
| `usecase/history` | `SearchDeviceHistoryUseCase` | Match saved name/address while preserving recency order. |
| `usecase/history` | `SaveDeviceHistoryUseCase` | Submit an observation snapshot for persistence. |
| `usecase/history` | `ClearDeviceHistoryUseCase` | Clear persisted observations. |
| `usecase/history` | `SyncScanHistoryUseCase` | Batch the latest snapshot every two seconds during scanning and flush on stop. |

`ScannerViewModel` receives its use cases through constructor injection. It maintains selection, queries, and screen state; use cases produce filtered and sorted results before they reach Compose. `ScannerUiState` holds the visible lists without running domain operations in property getters.

`AppContainer` constructs dependencies and launches one history synchronization job in application scope. Scheduling policy lives in `SyncScanHistoryUseCase`; the container also delegates the explicit background flush to `SaveDeviceHistoryUseCase`. Native permission dialogs and readiness checks remain in the Android host, while scan commands go through the ViewModel's use cases.

This separates reasons to change: scan policy, ordering, and persistence coordination live in different classes. Use cases depend on focused repository interfaces, and data adapters implement those interfaces. New operations can be added as use cases without expanding a shared facade. Small concrete use cases are injected directly; interfaces are used at repository boundaries where implementations vary. The SDK's portable BLE models remain the shared contract rather than being duplicated in the application domain.

## Screens

### Scanner

Start/stop a shared foreground scan. Search by name or observed address, filter minimum RSSI, and choose named-first, strongest-signal, alphabetical, or most-recent sorting. Stale devices stay below recent reports. Named-first is the default.

Rows show the advertised name when available, address, raw RSSI, and an observation/proximity label. Sorting and navigation do not restart scanning.

### Device details

Open a scanner row to inspect:

- A five-band proximity radar with a numbered range legend, current-band highlight, raw RSSI, and last-observed timestamp. The marker sits on a fixed axis and disappears when the signal is stale, scanning stops, or no range is available. Ring spacing is schematic; it does not represent bearing or a linear distance scale.
- Separate raw and smoothed RSSI values. Radar bands use the SDK's raw-RSSI classification, matching the displayed range.
- A timestamped chart of up to 48 recent reports. Pause the chart without stopping the scan, or expand the report values as text.
- First/last observed timestamps and accepted report count.
- Connectability, legacy/extended advertising, TX power, primary/secondary PHY, advertising SID, data completeness, and AD flags when reported.
- Advertised service UUIDs, company identifiers and manufacturer payloads, service payloads, and raw advertising hex.

Raw payload sections are expandable. Addresses, UUIDs, and payloads support text selection. Unavailable fields remain labeled as not reported/advertised. The selected snapshot is retained after cache eviction and marked stale, so its details remain inspectable without claiming a live signal.

### History

Search saved names or addresses independently of scanner search. Open an entry to inspect its saved RSSI and timestamp. Current advertising details appear if that address is present in the scan cache; otherwise the screen offers scanning. Clear all history with confirmation. The database keeps up to 2,000 addresses.

Each observed MAC address is the primary key of one history entry. A rescan that receives a newer observation for the same address replaces its stored name, RSSI, and last-seen time instead of adding a duplicate. History stores the latest observation per address rather than a separate record for every scan session. A device that rotates its private address can appear as a new entry.

## AI-use disclosure

OpenAI Codex was used as a development assistant on this project. Its contribution included generating and editing implementation code, researching platform behavior, refactoring, authoring test sources, and writing documentation.

| Area | Assistance provided |
| --- | --- |
| Research and technical decisions | Reviewed Android BLE permissions across OS versions, advertising fields, RSSI limitations, and resource-management options using platform documentation and scanner references linked below. |
| Architecture and SDK | Helped separate the reusable Bluetooth SDK from the app, define repository contracts, and refactor application operations into individual MVVM/Clean Architecture use cases. |
| Android implementation and persistence | Generated and revised scanner lifecycle/error handling, advertising mapping, bounded signal/cache processing, permission flow, and Kotlin Room/KSP persistence. |
| UI and copy | Implemented and revised Compose scanner, history, device details, proximity radar, search/sorting, accessibility descriptions, English copy, and system light/dark themes. The UI UX Pro Max skill provided design guidance as described under UI decisions. |
| Validation and documentation | Authored and updated test sources, inspected imports and layer boundaries, checked source diffs/whitespace, prepared documentation/release notes, and configured the tag-triggered GitHub Actions APK build. |

The maintainer supplied the requirements and directed revisions through feedback, including the Android focus, separate SDK, Kotlin persistence, individual use cases, and UI changes. AI-produced code still requires maintainer review and validation on the target devices.

The initial implementation was reviewed with source/static checks, without local builds, test runners, or emulators, following the maintainer's instruction. The maintainer subsequently authorized APK builds on GitHub Actions. Workflow runs record their own compilation results; they do not establish runtime correctness or device compatibility. Pending manual checks are listed under Build and validation.

AI assistance and Python tooling were used during development only. The shipped application and SDK use Kotlin and platform BLE APIs; they do not call an AI service or embed a Python or NDK optimization runtime.

## BLE indicator research

The detail view uses measurements and advertising fields available from Android's [ScanResult](https://developer.android.com/reference/android/bluetooth/le/ScanResult) and [ScanRecord](https://developer.android.com/reference/android/bluetooth/le/ScanRecord), with scanner workflows informed by [nRF Connect for Android](https://github.com/nordicsemi/Android-nRF-Connect).

| Indicator | What this app can show | Interpretation |
| --- | --- | --- |
| Identity | Advertised name and observed address | Private addresses can rotate. Neither field verifies permanent identity. |
| Signal | Raw RSSI, EMA, recent trend and range | A stronger RSSI is generally closer, but obstacles, orientation, and hardware affect it. |
| Observation | First/last seen and accepted report count | Counts are Android-delivered reports, not all transmitted packets or packet-loss measurements. |
| Radio | Connectability, format, TX power, PHY, SID, completeness | API 26+ fields are guarded. Connectable does not mean connected. |
| Advertising | Services, company IDs, manufacturer/service payloads, raw record | Advertised UUIDs are not a complete GATT service discovery. A company ID does not verify the product manufacturer. |
| Battery/firmware | Not inferred | Requires a connection or an implemented device-specific advertising protocol. |
| MTU, pairing, connection state | Not supplied by this scan-only SDK | Requires the appropriate connection/bonding operations. |
| Advertising interval | Not estimated | Callback spacing can miss packets and is not the configured peripheral interval. |

The proximity ranges follow the study-case thresholds. Overlapping boundaries go to the stronger range: -30, -50, -70, -80, and -90 dBm. The UI describes ranges as estimates; it does not present bearing, coordinates, or calibrated distance.

## UI decisions

The requested [UI UX Pro Max skill](https://github.com/nextlevelbuilder/ui-ux-pro-max-skill/blob/main/.claude/skills/ui-ux-pro-max/SKILL.md) was consulted using a task-local copy. Its Compose semantics/lazy-list guidance and time-series chart recommendations informed the implementation. The design-system search returned web-oriented layouts; the native layout instead follows the skill's general mobile guidance.

- Material typography, system text scaling, an 840 dp content-width cap, and scrollable screens.
- Semantic light/dark color tokens, consistent outline icons, 32 dp navigation icons, labeled controls, and Android-sized touch targets.
- Expandable technical sections, selectable identifiers, and a textual alternative to the chart.
- Preserved scanner/history scroll positions while navigating. Scanner and history queries remain separate.
- No authored looping animation. Live chart updates can be paused. Status text accompanies color.
- Short functional labels such as Scanner, History, Device details, Start scan, and Last observed.

## Lifecycle and resource limits

- One scan instance owned by the application container. Flow collection does not create another scan.
- Two-minute foreground sessions; entering the background stops scanning. Returning requires an explicit start. Rotation retains the ViewModel and scan session.
- Snapshots every 300 ms, stale after eight seconds, eviction after 60 seconds, and a maximum of 500 cached devices.
- EMA alpha 0.25 and a maximum of 48 signal samples per cached device. Stale gaps reset smoothing and the signal window.
- Monotonic timestamps control expiry, chart time, and persistence deduplication. Calendar timestamps are for display and saved history.
- Legacy scanning on older/unsupported devices. Extended advertising and supported PHYs are enabled only on API 26+ with controller support.
- Batched Room writes every two seconds while scanning, with a flush on stop/background. No idle persistence polling loop.
- Room stores identity/RSSI/time only. Advertising payloads and signal histories are session data, not persisted.
- No GATT, pairing, Bluetooth Classic, background service, or embedded NDK/Python runtime. `SignalProcessor` remains an extension point for measured future work.

## Permissions

The example host uses a conservative physical-proximity policy and does not declare `neverForLocation`.

| Android version | Foreground runtime permissions |
| --- | --- |
| API 24-30 | `ACCESS_FINE_LOCATION` |
| API 31+ | `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`, `ACCESS_COARSE_LOCATION`, `ACCESS_FINE_LOCATION` |

Legacy Bluetooth manifest permissions are capped at API 30. CONNECT is used to read adapter state; no GATT connection is made. Coarse/fine location are requested together on modern Android so the user can select precise location. Location services must be enabled under this policy.

The host owns rationale, runtime dialogs, settings navigation, and the Bluetooth enable request. The SDK checks readiness and handles permission changes during scanning/cleanup. No background location, advertising, notification, or foreground-service permission is requested.

Consumers selecting `derivesPhysicalLocation=false` must align their actual data use and manifest with `neverForLocation`, including its beacon-filtering tradeoff. See [SDK integration](bluetooth-sdk/README.md).

## Build and validation

Open the project in Android Studio supporting the configured AGP/Kotlin versions. Use a compatible JDK and the Android SDK version in `gradle/libs.versions.toml` (currently 37). Set the SDK path in `local.properties`. Test BLE on a physical API 24+ device with an advertising peripheral.

Run these yourself:

```powershell
.\gradlew.bat :androidApp:assembleDebug
.\gradlew.bat :bluetooth-sdk:testAndroidHostTest :shared:testAndroidHostTest
```

The debug APK is produced under `androidApp/build/outputs/apk/debug/`. Local builds and test execution remain under maintainer control. Tagged APK builds run on GitHub Actions as described below. No device screenshots or runtime-layout verification are claimed.

Test sources cover RSSI boundaries, silent-device expiry, clock changes, delayed reports, bounded history/cache behavior, metadata propagation, payload byte formatting, permission policy, individual use cases, and ViewModel selection/history behavior. Domain tests include two-minute scan policy, distinct stop reasons, filtering independent of ordering, deterministic sorting, history search/clear, and virtual-time persistence batching, stop/background flushes, restart timing, and cancellation.

Manual checks to run:

- Permission grants, denials, permanent denial, approximate-only location, and revocation.
- Bluetooth/location changes during a session, scan timeouts, and repeated starts.
- Rotation, background/foreground, tab/back navigation, and retained search/scroll state.
- Peripheral disappearance/recovery, radar range/legend agreement, stale/stopped marker removal, raw/EMA readings, chart pause/resume, and report-value expansion.
- Legacy/extended advertisements, absent TX power, empty payloads, and truncated records on supported hardware.
- Persistence across app restarts, searching history, opening saved details, and clearing history.
- Light/dark themes, a small phone around 375 dp, landscape, tablet width, largest system text, reduced motion, and TalkBack labels.

## Tagged APK builds on GitHub

The [Build tagged APK workflow](.github/workflows/build-apk.yml) runs when a version tag such as `v1.0.1` or `v1.1.0-rc.1` is pushed. It checks out that exact tag, sets up Java 17 and the compile SDK from the version catalog, validates the Gradle wrapper, and runs `:androidApp:assembleDebug` on GitHub's Ubuntu runner.

After a successful build it uploads `BlueCon-<tag>-debug.apk` and its `.sha256` checksum to the matching GitHub Release, creating the release if needed. Tags with a prerelease suffix produce prereleases. Build artifacts are also available from the Actions run for 30 days; release assets are separate from that retention setting. Failed builds do not publish an APK.

To publish a new version, update `versionName` and increase `versionCode` in `androidApp/build.gradle.kts`, commit the changes, push the branch, then create and push a matching tag. For example, after committing version `1.0.2`:

```powershell
git push origin master
git tag -a v1.0.2 -m "BlueCon v1.0.2"
git push origin v1.0.2
```

You can also open **Actions → Build tagged APK → Run workflow** on the default branch and enter an existing tag. This builds the original tagged source; it does not move the tag. Reruns replace APK/checksum assets with the same names while preserving existing release notes.

These APKs use Android debug signing for testing and require no signing secrets. Each fresh runner can generate a different debug key, so installing over a build from another run may require uninstalling the old app. Production distribution needs a maintained release keystore and a release-signing workflow. This workflow builds only the APK; it does not run tests or validate Bluetooth behavior on hardware.

The setup follows the official [Android Gradle plugin compatibility requirements](https://developer.android.com/build/releases/agp-9-1-0-release-notes), [Gradle Actions](https://github.com/gradle/actions/tree/main/setup-gradle), and [GitHub artifact documentation](https://github.com/actions/upload-artifact).

## Limitations

RSSI ranges are not hardware-calibrated. Rotating private addresses can create multiple history records or prevent reacquisition of an old address. Android/OEM scanning policies can limit observations, particularly with the screen off. Force-stopping the process can lose the last unflushed persistence batch. A bounded cache is not a packet archive. Database schema version 1 is unchanged; future structural changes require a migration.

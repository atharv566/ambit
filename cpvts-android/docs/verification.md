# Verification and integration checklist

## Executed checks

Verification was performed on Windows with JDK 17, Gradle 9.6.0, AGP 9.4.1, SDK Platform 37.2 and Build Tools 37.0.0.

- Gradle wrapper generation and distribution checksum verification: passed.
- Kotlin application compilation (`compileDebugKotlin`) and generated Java compilation: passed.
- Debug manifest merge: passed. The app requests only `INTERNET` and `ACCESS_NETWORK_STATE`; AndroidX's unused dynamic-receiver permission is explicitly removed because the app registers no dynamic broadcast receivers.
- Debug signing validation: passed using an isolated local Android preferences directory. No signing key is distributed.
- `assembleDebug`: **not completed**. Both the AGP-bundled AAPT2 and SDK Build Tools AAPT2 crash during resource linking. A standalone minimal manifest, containing no app resources, also reproduces native exit `-1073741819` (`0xC0000005`) against both SDK 37.2 and the installed SDK 36. This is a tool/runtime failure observed in this environment; it does not establish that APK assembly works on another machine. No APK is supplied or claimed to be verified.
- The compiled app classes were exercised by an isolated JVM JUnit harness: **21 tests passed** (`RepositoryTest`, `HttpContractTest`, `ParkingViewModelTest`). The normal Gradle unit-test task could not complete because AGP schedules resource processing before its test-class bundling task, and that resource step is the AAPT2 failure above.
- Lint analysis reached the app and reports the existing untracked `local.properties` Windows path formatting error plus Android API warnings for platform theme attributes; the distributable project excludes `local.properties`. Dependency-update notices are informational. Lint did not produce a clean final report because resource processing crashed.

Validation redirected Gradle/Android caches and Java temporary files into the workspace. Kotlin compilation used `-Pkotlin.compiler.execution.strategy=in-process -Pkotlin.incremental=false` because the sandbox's compiler-daemon temporary files were not reliable across processes. These environment-specific flags are not forced on the distributed project. Build commands in the README use the normal Gradle workflow.

The Android contract was compared with `docs/api-contract.md` in the existing CPVTS web project and the user's copy under `D:\ambit\cpvts`. The migration in that copy has the same SHA-256 as the migration already applied to Supabase earlier in this work: `598b26dc6dbfcb3e990a4aa62ac1f3f6c420be5971faa91d46e7f222cf06d4cf`. No schema change is required for this Android client.

## Tests included

| Suite | Coverage |
| --- | --- |
| `RepositoryTest` | Confirmed check-in/checkout values; normalized vehicle and slot; full zone; duplicate; not found; server field validation; read vs write network failures/timeouts; unconfigured API; cancellation |
| `HttpContractTest` | Retrofit paths and request JSON; unused checkout identifier omitted; JSON content type; no auth header; all read routes; no POST retry after 503/Retry-After; no redirect replay; malformed success body |
| `ParkingViewModelTest` | Invalid forms block calls; double taps submit once; input/result/receipt restoration; no replay of interrupted writes; explicit verification after unknown result; slot identifier exclusivity; full-zone input preservation |

The tests use isolated fake/mock data and do not create records in Supabase. Tests that reconstruct SavedStateHandle verify the application's restoration logic; they do not replace on-device Android process recreation testing.

## Device and API checks still to perform

Run these after configuring an API URL reachable from the target device. Use a test backend/database or coordinate test vehicle identifiers with the parking operator.

1. Launch with the API available; compare both zone counts with the web dashboard. Refresh while the web app checks a vehicle in/out and confirm shared state.
2. Check in a student two-wheeler; verify the returned assigned space matches the web record. Try the same active vehicle again and verify the duplicate response. In an isolated test dataset, fill a zone and verify the full-zone response.
3. Check out using the vehicle number, then a different test session using space ID. Verify server duration, free hours, rate and fee, and the released space on the web dashboard. Try exit before entry, missing vehicle and out-of-range hour.
4. Search for a parked vehicle, then for its number after checkout. Verify active/not-found behavior and the prefilled check-out shortcut.
5. Compare the summary with the web app for today and another day, including sessions whose entry and checkout fall on different IST calendar dates. Fees must be assessed values, not payment confirmations.
6. Disable connectivity for a read. Restore it and use the retry action. Interrupt a write in a controlled test backend: confirm unknown-outcome wording, disabled resubmission, verification navigation and no automatic replay.
7. Rotate each screen with a partly completed form and after receiving a result. Put the app in the background, terminate its process using Android Studio/ADB, then restore its retained task. Verify saved input/results and interrupted-write handling. Do not confuse force-stop or clearing app data with normal saved-state recreation.
8. Use a small phone in portrait/landscape and a tablet. Set font size to 200%, enable TalkBack and open the keyboard in each form. Check all fields, long errors, receipt text, navigation labels/roles, announced results, scroll reachability and the submit action above the keyboard.
9. Check Android API 26 and the current target platform. On Android 17, use a controlled HTTPS endpoint/tunnel rather than direct LAN access because the specified two-permission limit excludes the new local-network runtime permission. For release, verify its certificate chain and confirm HTTP URLs fail configuration. Configure your signing key only when preparing your own release.

## Scope limits

No development or production CPVTS API URL was supplied. Placeholder builds intentionally show a configuration state instead of mock parking data. No emulator session, physical-device session, Android-to-live-API request, TalkBack session or screenshot-based layout review has been performed. No app store publication or deployment is part of this deliverable.

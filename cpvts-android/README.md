# CPVTS Android

Native Kotlin + Jetpack Compose client for the existing CPVTS Node REST API. Single activity, single `:app` module, Material 3, MVVM, StateFlow, SavedStateHandle, coroutines and manual dependency wiring. This project contains no backend, database driver, local parking database, Supabase client, analytics or authentication feature.

## Open and run

1. Extract the source archive and open **the `cpvts-android` folder** in Android Studio.
2. Use an Android Studio version compatible with **Android Gradle Plugin 9.4.1**, **Gradle 9.6.0** and **JDK 17**. Select JDK 17 in Settings → Build Tools → Gradle → Gradle JDK. Older Android Studio versions may require an update.
3. Install Android SDK Platform **37.2** and Build Tools **37.0.0** through SDK Manager. The app uses **compileSdk 37, minor API 2; targetSdk 37; minSdk 26**. Only stable dependencies are used; exact versions are in `gradle/libs.versions.toml`.
4. Let Android Studio create `local.properties` with your `sdk.dir`. Add your reachable CPVTS Node API URL as described below.
5. Sync Gradle, select the `app` configuration and a device or emulator running Android 8.0/API 26 or newer, then Run. Select the `debug` build variant for local HTTP development.

The default configuration uses an intentionally unconfigured API address. It shows a configuration message until rebuilt with your API URL. It contains no database password or API secret. **No APK is bundled:** native AAPT2 resource linking crashed in the execution environment, including for a minimal unrelated manifest. See the exact verification limits in `docs/verification.md` and build locally using the commands below.

## API configuration

The one configuration location is **`app/build.gradle.kts`**, which sets `BuildConfig.API_BASE_URL` separately for debug and release. Only `data/Network.kt` reads that constant. Overrides may be placed in your untracked `local.properties` or passed as Gradle project properties:

```properties
CPVTS_DEV_API_BASE_URL=https://your-reachable-development-host/api/v1/
CPVTS_PROD_API_BASE_URL=https://your-production-host/api/v1/
```

Replace those example hosts; they are not services provisioned by this project. URLs must end in `/api/v1/`, with no credentials, query or fragment. The default named placeholders are `cpvts-dev-api-host.invalid` and `cpvts-production-api-host.invalid`; the repository detects them and does not attempt requests. Production **must use HTTPS**. Debug alone supports HTTP through its own network security configuration. No certificate verification bypass is included.

For the Android emulator, if the existing web project's Node API is running on the host machine on its documented port 5173, the corresponding example is `http://10.0.2.2:5173/api/v1/`. This is an emulator routing example, not a verified endpoint for your environment. A physical device needs a reachable host LAN address, matching server bind/firewall configuration, or an HTTPS tunnel. `localhost` on a device refers to that device. Test the API's `/health` route from the target environment before using parking operations.

**Android 17 local-network restriction:** apps targeting API 37 need an additional runtime permission for direct LAN connections. This project preserves the brief's two-permission limit (`INTERNET` and `ACCESS_NETWORK_STATE`), so use a controlled HTTPS API endpoint/tunnel reachable without direct LAN access when testing on Android 17. The local-address examples apply to older device platforms where local-network protection is not enforced. Adding Android 17 LAN support would require changing the permission requirement and implementing its runtime grant flow. See [Android local-network permission](https://developer.android.com/privacy-and-security/local-network-permission).

**The Supabase project URL, REST Data API URL and Postgres pooler are not the CPVTS Node API.** Keep the database connection, CA certificate, SQL migration and Supabase server key in the web backend. The Android app neither applies `001_parking.sql` nor connects directly to Postgres. It consumes the same server data as the web app. No mobile migration is needed for these workflows.

The supplied contract defines no client authentication protocol. This client sends JSON with `Content-Type: application/json` and no invented authorization header. An authenticated/public deployment requires a documented server authentication contract and corresponding client integration before use outside the existing trusted environment.

## Screens and endpoint mapping

All paths below are relative to `/api/v1/`. Kotlin API functions are declared in `data/ParkingApi.kt` and accessed by `ApiParkingRepository`.

| Screen/action | Method and path | Kotlin API function |
| --- | --- | --- |
| Dashboard, Refresh, refresh after successful write | `GET dashboard` | `dashboard()` |
| Check-in, submit arrival | `POST check-ins` | `checkIn(request)` |
| Check-out, create receipt | `POST check-outs` | `checkOut(request)` |
| Find Vehicle, search | `GET vehicles/{vehicleNumber}` | `find(vehicleNumber)` |
| Daily Summary, choose date/load, refresh after successful write | `GET summary?date=YYYY-MM-DD` | `summary(date)` |
| Contract support, available but not requested by a screen | `GET slots` | `slots()` |
| Contract support, available but not requested by a screen | `GET health` | `health()` |

- **Dashboard:** current total, occupied and available spaces in both server-provided zones; reporting-day arrival count and assessed fees; active vehicles; loading, empty, error and refresh states. Failed refreshes retain figures with an explicit last-known label.
- **Check-in:** uppercase vehicle input, vehicle type, owner category, integer entry hour, validation, first-available server space, full-zone and duplicate responses. Staff/student/visitor rates are informational; final amounts always come from the API.
- **Check-out:** exactly one vehicle number or space ID, integer exit hour, server validation and receipt with server duration, free/billable hours, hourly rate and amount due. Successful responses refresh dashboard and summary. No payment claim.
- **Find Vehicle:** active server record with space, entry hour and status; empty/not-found/error states; shortcut to prefill check-out. Saved search results are explicitly described as the last server result.
- **Daily Summary:** valid ISO reporting date, server-provided arrival/check-out totals and assessed fees, completed sessions filtered by checkout timestamp in `Asia/Kolkata`, and empty/loading/error/retry states.

Full request and response definitions are included in `docs/api-contract.md`, copied from the existing web project.

## Reliability and state

The backend assigns spaces and calculates fees. The app displays its responses without locally allocating spaces or calculating a final fee. Input validation uses the documented vehicle normalization and integer hours 0–23; server messages and field errors remain visible. Overnight rollover is not supported by the contract.

Writes are **never automatically retried or queued**. The OkHttp client disables connection retries and redirects; POST bodies are one-shot to prevent HTTP retry follow-ups. It uses 10-second connection, 15-second read/write and 25-second overall call limits. Debug logs contain only method and response code, with no URLs, vehicle identifiers, headers, bodies or secrets. Release does not install the logger.

During a write, inputs and submit are disabled. The app records an uncertainty marker before sending. Timeout, connection failure, ambiguous server error or restoration of an interrupted write produces an **unknown outcome** state, with links to Vehicle Finder and Dashboard. Submission remains disabled until the user explicitly acknowledges checking the current status. The app never replays an interrupted write. Reads can be retried by the user.

Form inputs, last confirmed check-in, receipt and search result survive activity recreation using SavedStateHandle. Only those small UI values are saved; parking data is not stored in a database. Dashboard/summary reload when the ViewModel is recreated. Android saved-state restoration applies when Android retains the task; it is not durable storage after clearing app data or force-stopping/removing the task.

## Design system

The user's **light-only** requirement takes precedence over the generic Android brief's dark-theme sentence. There is no dynamic color or dark-mode derivation.

| Purpose | Exact colors |
| --- | --- |
| Canvas, navigation, cards, borders | `#F9F7F2`, `#F4EFE6`, `#FFFFFF`, `#E6DFD5` |
| Main and secondary text | `#231F20`, `#736B63` |
| Primary actions and selected states | `#FFB300`, `#FFF8E1` |
| Supporting actions and information | `#1E88E5`, `#E3F2FD` |
| Active, available and confirmed states | `#2E7D32`, `#E8F5E9` |

`ui/Theme.kt` defines the tokens. Error messages use charcoal text, an amber-tinted surface and explicit wording. Action labels use charcoal on amber or blue tint for readable contrast. Cards have warm borders and no elevation. Layouts are scrollable, centered with a 900dp maximum content width, and use 48dp minimum actions. Sticky form submission remains above the keyboard; navigation hides while the keyboard is open. Navigation icons retain full TalkBack labels when larger font scales hide short visual captions. Detail rows stack vertically and text wraps. Actual TalkBack, large-font, landscape and device layout testing remains required; see `docs/verification.md`.

## Build and tests

Windows PowerShell:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
```

macOS/Linux:

```sh
chmod +x gradlew
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lintDebug
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`. Gradle uses the standard local Android debug signing key, which is not distributed. Release is unsigned until you configure your own signing process; release minification is disabled.

Unit tests use a fake API, a fake repository, coroutine test dispatchers and MockWebServer. They cover success, full zone, duplicate vehicle, checkout receipt, not found, server validation, offline/timeout handling, JSON/path compatibility, no auth headers, no write retries, malformed write responses, double taps, input preservation and saved-state recovery. Tests do not modify the shared parking database.

## Project files

```text
cpvts-android/
  README.md
  SOURCE_FILES.md
  .gitignore
  settings.gradle.kts
  build.gradle.kts
  gradle.properties
  gradlew
  gradlew.bat
  gradle/
    libs.versions.toml
    wrapper/
      gradle-wrapper.jar
      gradle-wrapper.properties
  docs/
    api-contract.md
    verification.md
  app/
    build.gradle.kts
    src/
      main/
        AndroidManifest.xml
        java/com/example/cpvts/
          CpvtsApplication.kt
          MainActivity.kt
          data/
            Models.kt
            Network.kt
            ParkingApi.kt
            ParkingRepository.kt
          ui/
            ParkingViewModel.kt
            Screens.kt
            Theme.kt
        res/
          drawable/ic_parking.xml
          values/strings.xml
          values/styles.xml
      debug/
        AndroidManifest.xml
        res/xml/debug_network_security_config.xml
      test/java/com/example/cpvts/
        RepositoryTest.kt
        HttpContractTest.kt
        ParkingViewModelTest.kt
```

`SOURCE_FILES.md` contains complete contents for every distributable text file under its full relative path. The wrapper JAR is supplied as the actual binary file, with its digest in that listing. Machine-specific `local.properties`, downloaded SDK/Gradle caches, generated build files and signing keys are excluded from the source archive.

## Remaining integration

Development API URL and production API URL were **not provided**. Default package is `com.example.cpvts` (`com.example.cpvts.debug` for debug). Currency INR, IST reporting and rate information follow the supplied PDF/API contract. All required parking endpoints exist; runtime authentication and payment confirmation are not defined by that contract and have not been invented.

Provide the appropriate reachable Node URL, rebuild, then follow the device integration checklist in `docs/verification.md`. No Play Store publication or deployment has been performed.

Toolchain references: [Android Gradle Plugin 9.4](https://developer.android.com/build/releases/agp-9-4-0-release-notes), [Android 17 setup](https://developer.android.com/about/versions/17/setup-sdk), [Android build configuration](https://developer.android.com/build).

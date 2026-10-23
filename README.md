# ClinicAI — native Android app

A native Kotlin/XML Android app for the existing **ClinicAI** web platform
(<https://clinicai-555888.vercel.app>). It talks to the same backend and database as the website —
there is no WebView and no separate local database.

* Patients: find doctors, see real availability, book appointments, get a **booking confirmation
  bill (PDF)**, manage visits, ask the AI receptionist, join the live walk-in queue.
* Clinic staff: overview, doctors, live queue, analytics, staff invites, clinic settings.

Docs: [authentication & the sign-in fix](docs/AUTHENTICATION.md) · [architecture](docs/ARCHITECTURE.md) ·
[website → app feature map](docs/FEATURE_MAPPING.md) · [backend changes you must deploy](docs/BACKEND_CHANGES.md) ·
[verification report](docs/VERIFICATION_REPORT.md)

## Open and run (Android Studio)

1. Clone the repository, then open **Android Studio** → *Open* → choose the cloned project folder. Let Gradle sync finish
   (needs internet the first time).
2. Pick a device: an emulator (e.g. *Medium_Phone*) or a USB phone.
3. Press **Run ▶**. The debug build talks to the **deployed** backend by default.
4. Command line alternative: `./gradlew assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`.

### Using a local backend instead (emulator only)

```bash
cd <path-to-clinicai-backend> && npm run dev        # http://localhost:3000
cd <path-to-this-repo> && ./gradlew installDebug -Pclinicai.env=dev
```
`-Pclinicai.env=dev` points the debug app at `http://10.0.2.2:3000/` (the emulator's alias for your
Mac). Plain HTTP is allowed **only** for `10.0.2.2`/`localhost` and **only** in debug builds
(`app/src/debug/res/xml/network_security_config.xml`). Release builds are HTTPS-only and always use
production (`ApiConfig`).

## Tests

```bash
./gradlew testDebugUnitTest            # 168 JVM tests (validation, receipts, networking, booking, auth rules, distance, contacts)
./gradlew connectedDebugAndroidTest    # 27 on-device tests (PDF, navigation, forms, backend reads); needs a running emulator
```

### Live tests (real Clerk sign-in/registration/password reset + real booking)

These create throw-away users in your Clerk **development** instance using Clerk's `+clerk_test` addresses (no e-mail is sent,
the code is always `424242`) and book against a backend the emulator can reach. They are skipped unless you opt in.

```bash
# 1. a local backend on a throw-away database (see docs/BACKEND_CHANGES.md), reachable from the emulator at 10.0.2.2:3000
# 2. install the app built for it
./gradlew installDebug installDebugAndroidTest -Pclinicai.env=dev
# 3. run (creates the test accounts, runs 14 tests, ~2 minutes)
LOCAL_DATABASE_URL=postgresql://postgres@127.0.0.1:5433/clinicai_test tools/live-tests/run-live-tests.sh
```
Delete the leftover `qa.*+clerk_test@example.com` users from the Clerk dashboard afterwards (search `clerk_test`).

Backend-side checks (local only; they refuse anything else) live in the backend repository's `scripts/` folder.

## One-time setup that only you can do

| Step | Why |
|---|---|
| Deploy the backend changes in the ClinicAI backend repository and run its migration (see [BACKEND_CHANGES](docs/BACKEND_CHANGES.md)) | Race-safe booking (409 on a taken slot), automatic patient records for new accounts, appointment history, doctor-by-id and the staff screens. Without it the app still works, with graceful fallbacks. |
| Replace the placeholder clinic e-mails/phones in the database with real ones | The app deliberately shows "Not available" for placeholders (`info.demo@….example`, `+91-22-9999-0000`) |
| Keep **Native API** enabled in the Clerk dashboard (it appears to be: sign-in attempts from the app reach Clerk and return normal "account not found" errors) | Lets the app sign users in with the same accounts as the website. If sign-in ever reports it is disabled, switch it on under *Clerk dashboard → Native applications*. |
| Sign in with a real account to try booking | The booking endpoint requires a Clerk session; the app never fakes one. |

## Project layout

```
app/src/main/java/com/example/clinicai/
  config/        ApiConfig (dev/prod base URLs), constants
  data/          model/ (DTOs) · remote/ (Retrofit, errors) · repository/ · local/ (DataStore, receipt cache) · auth/ (Clerk)
  domain/        receipt building/formatting, slot grouping, filters, ports (interfaces)
  pdf/           PDF generator + FileProvider sharing
  ui/            home · doctors · booking · chat · appointments · checkin · faq · profile · auth · staff · common
  util/          validators, date/currency formats, privacy masking, QR
```

## Requirements

* Android Studio with the Android SDK (`compileSdk` 37) and a JDK supported by the bundled Gradle wrapper.
* Android Studio creates `local.properties` (SDK path) for you; it is machine-specific and is not committed.

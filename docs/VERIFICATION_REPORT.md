# Verification report — round 2

Everything below was actually run on this machine (macOS, Gradle 9.5, AGP 9.3.3, Kotlin 2.4.21, API 36 emulator,
Node 24, PostgreSQL 16). "Live" means the **real Clerk dev instance** with a **local copy of the backend on a throw-away
local database** (production data was not written). Where something could not be verified, it is said plainly.

## Results at a glance

| Check | Result |
|---|---|
| `./gradlew assembleDebug` / `assembleRelease` | ✅ — debug APK `app/build/outputs/apk/debug/app-debug.apk` (production backend) |
| `./gradlew lintDebug` | ✅ **0 errors**, 70 style warnings, 0 unused resources |
| Unit tests (JVM) | ✅ **168 passed**, 0 failed |
| On-device tests, standard run | ✅ **27 passed**, 0 failed (15 live tests skip themselves without the opt-in flag) |
| On-device **live** tests (real Clerk + local backend) | ✅ **14 passed**: account flows 9, booking flows 4, staff area 1 |
| Backend `tsc --noEmit` and `next build` | ✅ |
| `scripts/verify-booking-concurrency.ts` | ✅ 6/6 (original code let 10 of 10 simultaneous bookings win; fixed code lets exactly 1) |
| `scripts/e2e-multiuser-booking.mjs` (two real Clerk users) | ✅ 15/15 |
| `scripts/e2e-staff-endpoints.mjs` | ✅ 12/12 |

## The 13 requested changes

| # | Requirement | Status | Evidence |
|---|---|---|---|
| 1 | Homepage "Your clinic's front desk — in your pocket." | ✅ | Screenshot; patient-focused tone throughout (tagline, PDF header too) |
| 2 | Remove the "ClinicAI handles … front of them" line | ✅ | Sentence deleted from the hero; no reference remains (grep) |
| 3 | "Designed around your healthcare needs." | ✅ | Screenshot |
| 4 | "Available 24/7" | ✅ | Screenshot; no other occurrence |
| 5 | "Speak your language" | ✅ | Body says English / Hindi / Hinglish — **verified against production chat**, which replies in each (Devanagari, Latin-script Hindi, English) |
| 6 | Show 4–5 clinics + See more | ✅ | 5 shown + "See 10 more ⌄" → all 15 + "Show less ⌃"; real data; loading/empty/error states |
| 7 | Distance from the user's location | ✅ with a caveat | Math checked against an independent haversine to ±0.005 km; all 4 on-screen distances fit one position within display rounding (0.03 km); permission explained first; Denied / unavailable / missing-coordinates → "Distance unavailable"; one-shot, in-memory only, no background tracking, no logging; 12 s timeout. **Caveat:** if the user grants only *Approximate* location, Android itself blurs the position by ~2 km, so the label then reads "**About** 2.4 km". The prompt lets users pick Precise. I could not steer the emulator's GPS precisely (Google Play services serves a cached, blurred fix there), so the end-to-end *accuracy* of a precise fix is covered by the unit tests, not by the emulator |
| 8 | Invalid e-mails / phones on the map screens | ✅ root cause fixed | **Root cause: every clinic e-mail in the database is a placeholder** (`info.demo@….example`) and one phone is `+91-22-9999-0000`. The app no longer shows or offers actions for them → "Not available", no Call/Email button. Directions/pin use the real coordinates (correct), falling back to the address. No Places API / no scraping / no API key is involved. Real values must be entered in the database |
| 9 | No sign-in message on the review screen when signed in | ✅ | Root cause: the screen sampled the session once, but Clerk restores it asynchronously after launch. Now reactive. Live test: open review signed-out → session becomes active → the screen switches to "Confirm appointment" by itself, and back on sign-out. The server independently rejects unauthenticated bookings (401, tested) |
| 10/12 | Forgot password with e-mail OTP | ✅ | Live test through the real screens: e-mail → code (wrong code rejected) → new password (mismatch / policy validation) → Clerk confirms → notice on sign-in → **sign in with the new password**. Unknown e-mails get the identical screen. Resend cooldown shown |
| 11 | Register → verify → book; no double-booking | ✅ | Live tests + two-user script (below) |
| — | Login error from the screenshot | ✅ root cause fixed | `needs_client_trust` was unhandled — see `AUTHENTICATION.md`. Live test signs an existing account in through the new "Verify it's you" screen |

### Multi-user scenario (Phase 9), `scripts/e2e-multiuser-booking.mjs`, all passing
User A registers (Clerk e-mail code) and gets a patient record → A books slot 1 (201, server id) → B refreshes
availability: slot 1 is gone → B submits slot 1 directly: **409 `SLOT_CONFLICT`**, no appointment created → B's list
omits A's booking; B reading A's appointment → **404 with nothing leaked**; B cancelling it → 404 → **A and B race
for slot 2 at the same instant: exactly one 201 and one 409** → cancelling frees the slot again. Signed-out booking → 401.

### Booking through the app (Phase 10), live tests, all passing
Real doctor → real availability → review (shows the signed-in patient, **no** sign-in prompt) → confirm → confirmation
shows the server's booking id → receipt shows the same record → the slot disappears from availability. A slot taken while
the user was reviewing → rejected by the server, "Please pick another time", **no confirmation and no receipt**, exactly one
appointment holds the slot, and the doctor screen's slot list is refreshed. The receipt PDF tests (valid PDF, rendered page,
share/view via FileProvider, overwrite-safe save) are unchanged and still pass.

### Failure handling (Phase 12 #17)
Unit-tested: offline, timeout, 4xx/5xx, HTML 404/405 (route not deployed), malformed JSON, conflict, rate limit.
On the emulator: with networking switched off the doctor list shows "You appear to be offline…" with **Try again**;
restoring the network and tapping it loads the list.

## What was NOT verified, and why

| Not verified | Reason |
|---|---|
| Anything against the **deployed production backend's new behaviour** | It isn't deployed yet. The app was verified against the same code running locally. Today, production still answers 405/404 for the new routes (the app degrades gracefully) and 400 for taken slots |
| **Real e-mail delivery** of Clerk's codes | Test addresses (`+clerk_test`) deliberately send nothing. Delivery uses Clerk's own e-mail service, which I did not touch |
| Sign-in with the **user's own accounts** | I don't have their passwords. The failing state was reproduced with test accounts |
| An MFA / authenticator-app challenge | This Clerk instance has no MFA enabled; that branch is implemented but untested |
| Razorpay deposit | Needs a real payment; unchanged this round |
| Precise-GPS distance accuracy, camera QR scan, different screen sizes / dark mode | Emulator limits (see #7); only a 1080×2400 phone was run |
| Google Maps data shown by the Maps *app* itself | That data belongs to Google, not to ClinicAI; the app passes only real coordinates |

## Round 3 — staff Overview "needs a backend update"

**Root cause (verified against production with unauthenticated requests):** the deployed site returns an HTML 404 for
`/api/dashboard/overview`, `/api/dashboard/analytics` and `/api/staff`, and 405 for `GET /api/appointments`. Those routes exist only in the
local backend working tree and were never deployed. The app's message was accurate for a missing route.

**App fix:** the Overview uses `GET /api/dashboard/overview`; if the server answers "route missing" it assembles the same figures from routes
production already has (`/api/clinics/me`, `/api/doctors/search?clinicId=`, `/api/queue/{id}/status`), never showing partial or invented numbers
(average wait stays empty). The upcoming list shows exactly why it is unavailable when `GET /api/appointments` is missing. Added All / Scheduled / Walk-in
tabs, search, a guard against duplicate loads, and a greeting that does not print a stray comma.

| Check | Result |
|---|---|
| Unit tests (Gradle) | 179 run, 0 failed (11 new: fallback composition, failure propagation, tab/search, retry, duplicate-load guard) |
| `e2e-cross-clinic.mjs` (local) | 15/15 |
| `e2e-staff-endpoints.mjs` (local) | 12/12 |
| `LiveStaffAreaTest` on emulator vs local backend | pass (Overview, tabs, Doctors, Queue, Analytics, Staff, Settings) |
| `tsc --noEmit`, `next build` (backend) | pass |
| `assembleDebug`, `lintDebug` (app) | pass |
| Deployed production backend | **not verified — routes still missing until the backend is deployed** |

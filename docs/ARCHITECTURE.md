# Architecture

**Pattern:** single-module MVVM with a repository layer and manual dependency injection.

```
Fragment/Activity (XML + ViewBinding)
   │ observes StateFlow / SharedFlow
ViewModel  ── talks only to repositories (or small interfaces for testability)
   │ suspend calls
Repository ── safeApiCall { ApiService.… }   (Retrofit + OkHttp + Gson)
   │
ClinicAI backend (Next.js on Vercel, Prisma/Postgres, Clerk auth)
```

| Concern | Where | Notes |
|---|---|---|
| Base URL / environments | `config/ApiConfig` | dev = `http://10.0.2.2:3000/`, prod = `https://clinicai-555888.vercel.app/`. Production must be HTTPS and must not be an emulator/localhost URL (enforced + unit-tested). |
| API contract | `data/remote/ApiService` | One typed method per real route in `src/app/api`. |
| Error handling | `data/remote/SafeApiCall`, `AppError` | Success envelope, error envelope, HTML 404/405 (route not deployed), malformed JSON, timeout, offline → one sealed type. Nothing throws into the UI. |
| Authentication | `data/auth/ClerkAuthManager` + `AuthInterceptor` | Clerk Android SDK signs the user in; the session JWT is sent as `Authorization: Bearer …`, which the backend's Clerk middleware accepts. Credentials go straight to Clerk and are never stored by the app. |
| Auth state & flows | `data/auth/ClerkAuthManager`, `ui/auth/*` | State machine over Clerk's sign-in states (new-device code, MFA, forced password change, e-mail verification, password reset); reactive `authState` (`Initializing`/`SignedOut`/`SignedIn`) observed by every screen. See `AUTHENTICATION.md`. |
| Distance | `data/location/LocationRepository`, `domain/GeoDistance`, `domain/DistanceResolver` | One-shot approximate location, in memory only; haversine distance from real clinic coordinates; explicit "unavailable" states. |
| Contact details | `domain/ContactValidator` | Phones/e-mails are shown (and Call/Email offered) only if they pass validation; placeholders such as `*.example` are "Not available". |
| Booking | `ui/booking/BookingViewModel` | `Confirmed` is emitted **only** after the server returns a created appointment. One request in flight at a time; timeouts are reported as "outcome unknown". |
| Receipt | `domain/ReceiptBuilder → ReceiptFormatter → pdf/ReceiptPdfGenerator` | Built only from the confirmed appointment + real doctor/clinic data; no invented fees, taxes or payments. Same text drives the on-screen bill and the PDF. |
| Persistence | `data/local` | DataStore for non-sensitive prefs; per-user JSON cache of server-confirmed receipts (app-private, excluded from backup). Not a replacement database. |
| Navigation | Jetpack Navigation (`nav_main`, `nav_auth`, `nav_staff`) | Website links `/checkin/{token}` and `/doctors/{id}` open native screens. |
| Staff area | `ui/staff` | Separate activity; only unlocked after `/api/me` returns a STAFF profile. Authorization is still enforced by the backend. |

## Security & privacy

* HTTPS only in release; cleartext limited to `10.0.2.2`/`localhost` in debug.
* No secrets in the app: the Clerk *publishable* key is public by design; Razorpay uses the key id the server returns per order.
* No network logging interceptor; tokens and patient data are never logged.
* `allowBackup=false`, backup/extraction rules exclude all app data.
* Permissions: `INTERNET`, `ACCESS_NETWORK_STATE`, and `ACCESS_COARSE_LOCATION` (requested only when tapping *Near me*).
* Receipts mask e-mail and phone; the QR scanner uses Google's code scanner (no camera permission needed).

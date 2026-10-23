# Website → Android feature map

Source of truth: the website's pages and `src/app/api` routes (inspected from source and the live deployment).

| Website | Route / API | Android | Status |
|---|---|---|---|
| Home, partner clinics | `/` (clinics derived from `GET /api/doctors/search`) | Home | ✅ verified on emulator |
| Find a doctor: search, specialty chips | `/find`, `GET /api/doctors/search` | Find a doctor | ✅ verified (real data) |
| Near me | `GET /api/doctors/nearby` | "Near me" (coarse location) | ✅ live API tested; permission flow built, not exercised |
| Doctor profile | `/doctors/[id]` | Doctor screen | ✅ (uses new `GET /api/doctors/{id}`, falls back to search until deployed) |
| Date strip + slots | `GET /api/doctors/{id}/availability` | Date strip, grouped slots | ✅ verified (real slots) |
| Book appointment | `POST /api/appointments` (Clerk) | Review → confirm → confirmation, conflict handling | ✅ verified live against a local copy of the backend + real Clerk; not executed against production (new race-safe booking isn't deployed) |
| Symptom notes → booking | localStorage note | DataStore note prefilled | ✅ unit tested |
| Optional deposit | Razorpay create-order / verify | Razorpay Android SDK | ⚠️ built; not exercised (needs login + payment) |
| My appointments, cancel | `/my-appointments`, `PATCH …/cancel` | Visits, Appointment detail | ⚠️ cancel API existing; **history needs new `GET /api/appointments`** |
| Review a completed visit | `POST /api/reviews` | Review dialog | ⚠️ built; needs login |
| Ask ClinicAI | `POST /api/chat` | Chat (+ suggested doctors cards) | ✅ verified end-to-end (live) |
| Walk-in check-in + live queue | `/checkin/[token]`, `GET /api/clinics/qr/{token}`, `POST /api/queue/join`, `GET /api/queue/{id}/status` | Check-in (QR / code / link), ticket | ✅ lookup + status verified live; join needs login |
| Sign in / sign up / forgot password | Clerk | Native Clerk screens: new-device e-mail code, MFA, forced password change, e-mail verification, password recovery | ✅ verified end to end with real Clerk test accounts (see AUTHENTICATION.md) |
| Staff dashboard overview | `/dashboard` | Overview | ✅ verified live (local backend, real staff login); needs deployment of `GET /api/dashboard/overview` |
| Doctors (add / deactivate) | `POST /api/doctors`, `POST …/deactivate` | Doctors | ⚠️ built; staff login required |
| Live queue (call/skip/complete) | `POST /api/queue/{id}/{action}` | Live Queue | ⚠️ built; staff login required |
| Analytics + CSV export | `/dashboard/analytics` | Analytics | ⚠️ needs new `GET /api/dashboard/analytics` |
| Staff invite | `POST /api/staff/invite` | Staff (+ list) | ⚠️ list needs new `GET /api/staff` |
| Clinic settings + QR | `PATCH /api/clinics/me` | Clinic Settings (QR drawn on device) | ⚠️ built; staff login required |
| *(API only, no website page)* clinic FAQs | `GET /api/clinics/{id}/faqs` | Clinic FAQs screen | ✅ verified (live) |

**Not implemented (not in the website UI):** doctor leave management (API exists but no page), FAQ editing,
doctor editing. The website has no payment gateway beyond the optional Razorpay deposit, so the bill
distinguishes a *confirmation* from a *deposit receipt* and never invents fees, taxes or discounts.

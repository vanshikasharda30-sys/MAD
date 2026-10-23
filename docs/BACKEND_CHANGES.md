# Backend changes (in the ClinicAI backend repository) — **not yet deployed**

Nothing below is on the live site yet, and nothing was committed or pushed. Review with `git status` / `git diff`
(10 files modified, 14 new; no existing route's response shape was changed, only extended).

## What changed

| Area | Change | Files |
|---|---|---|
| **Double-booking** | Booking now takes a transaction-scoped Postgres **advisory lock** on (doctor, date, time) before checking for a clash, so simultaneous requests are serialised. Slot conflicts return **HTTP 409 `SLOT_CONFLICT`** (was 400). | `availability.repo.ts`, `availability.service.ts`, `lib/errors.ts` |
| **Database backstop** | Partial **unique index** `Appointment_active_slot_key` on `(doctorId, date, startTime)` for `confirmed` / `in_consultation` appointments. The migration **skips itself with a notice if duplicates already exist**, so it can't break deployment. | `prisma/migrations/20261023000000_unique_active_appointment_slot/` |
| **Registration → patient record** | First authenticated request from a new Clerk account **creates the `Patient` row** (or claims a pending staff invite) from the verified session, instead of depending on the `user.created` webhook arriving first. Never takes over an e-mail that belongs to another Clerk user. | `services/provisioning.service.ts`, `middleware/withAuth.ts` |
| Read endpoints (round 1) | `GET /api/appointments`, `GET /api/appointments/{id}`, `GET /api/doctors/{id}`, `GET /api/dashboard/overview`, `GET /api/dashboard/analytics`, `GET /api/staff`; `GET /api/me` also returns `name`, `email`, `phone`. | `api/**/route.ts`, `appointmentQuery.*`, `dashboard.*` |

### Root cause of the double-booking risk
The old code did *check, then insert* inside a default-isolation transaction. I reproduced it: **10 simultaneous
requests for one slot → all 10 succeeded** (2 of 3 runs). Now exactly one succeeds. Run
`scripts/verify-booking-concurrency.ts` to see both behaviours yourself (local databases only).

Scheduling rule respected: one active appointment per doctor per start time (what the availability code already
assumed). Walk-ins (no start time) and cancelled/completed/no-show rows don't block a slot, so a cancelled slot can be rebooked.

## How to deploy (you do this; I did not)

1. `cd <path-to-clinicai-backend> && git diff` — read the changes.
2. Commit and push to the branch Vercel deploys (remote: `lakshay-sketch/clinicai`), or run `vercel --prod`.
3. Apply the migration to the **production** database once: `npx prisma migrate deploy`
   (with production `DATABASE_URL`). If it prints *"Skipped unique index … duplicate active appointments exist"*, run the
   query it shows, cancel the extra rows, then re-run the migration. Booking is already race-safe without the index (advisory lock).
4. Check: `curl -s https://clinicai-555888.vercel.app/api/doctors/doc_01` should now return the doctor (today: HTTP 405).

The Android app works before and after deployment: it falls back gracefully for missing routes and understands both
the old 400 and the new 409 conflict responses.

## Data problems found (not fixed — they need your real data)

* **Every clinic e-mail is a placeholder** (`info.demo@<name>.example`; `.example` can never receive mail) and
  `Lilavati`'s phone is `+91-22-9999-0000`. The app now refuses to show or offer actions for such values ("Not available").
  To fix at the source, enter real values in the database or in the dashboard's *Clinic Settings* (the app's settings
  screen rejects placeholders). Clinic **coordinates are correct** (checked against real Mumbai hospital locations).
* `Patient.phone` is empty for sign-ups (Clerk has no phone attribute). Receipts omit it instead of printing a blank.
* `.env` contains **two different Clerk instances** (`pumped-eft-15` on lines 15–17 and `top-buck-2487` on 20–21); the
  later duplicates win locally, so `npm run dev` verifies tokens against the wrong instance (production uses `pumped-eft-15`).
  Delete the stale pair.
* Doctor/clinic public responses include each clinic's `qrToken`.

## Test footprint (so nothing surprises you)

Testing created Clerk users (all `qa.*+clerk_test@example.com`, 32 in total) in the Clerk **development** instance and
**deleted them afterwards**. Booking/queue/staff tests ran on a **throw-away local Postgres** with a local copy of the
backend — production data was never written by me. One caveat: if your Clerk `user.created` webhook is configured for the
production site, it may have created `Patient` rows for those test users. You can check and remove them with:

```sql
SELECT id, email FROM "Patient" WHERE email LIKE 'qa.%+clerk_test@example.com';
DELETE FROM "Patient" WHERE email LIKE 'qa.%+clerk_test@example.com';   -- only if the rows above exist
```

## Scripts added (all refuse to run against non-local targets)

| Script | Purpose |
|---|---|
| `scripts/verify-booking-concurrency.ts` | 10-way race on one slot, index backstop, cancel/rebook |
| `scripts/e2e-multiuser-booking.mjs` | Two real Clerk users: A books, B sees it gone, B's direct attempt → 409, privacy checks, simultaneous race |
| `scripts/e2e-staff-endpoints.mjs` | Staff invite claim, dashboard/analytics/staff/appointments endpoints, patients refused (403) |
| `scripts/e2e-cross-clinic.mjs` | Second clinic in the LOCAL db: staff A/B each see only their own clinic; query/header `clinicId`/`role` ignored; cross-clinic deactivate and queue calls refused (404) |
| `scripts/e2e-staff-appointments.mjs` | Real patient booking → staff of that clinic sees it (scopes, fields, no contact data); other clinic sees none; empty clinic = confirmed empty list; clinic-local day boundary; 401/400 handling; refresh after a second booking |
| `scripts/verify-clinic-time.ts` | Pure checks of the clinic-local day helper (`src/server/lib/clinicTime.ts`) |

**Time zone:** `Appointment.date` / `QueueEntry.queueDate` are `@db.Date` holding a calendar day as UTC midnight. "Today" is now the clinic's day
(`Asia/Kolkata`, or `CLINIC_TIME_ZONE`), not the server's UTC day — used by the appointment lists, dashboard, queue and the website dashboard page.

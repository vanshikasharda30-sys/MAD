# Authentication — how sign-in, registration and password recovery work

The app uses **Clerk**, the same provider as the website (instance `pumped-eft-15`, a *development* instance).
The Clerk Android SDK signs the user in; the app then sends the session token to the backend as
`Authorization: Bearer <token>`. The backend verifies it with the same Clerk instance. The app never
sees or stores a password beyond handing it to Clerk, and holds no secret keys (the publishable key is public by design).

## Root cause of "Your account needs an additional step that the app can't complete yet"

Reproduced against the real Clerk instance with an ordinary existing account:

```
sign in with the correct e-mail + password from a device Clerk hasn't seen before
  → Clerk answers  status = "needs_client_trust"      (no session is created yet)
  → supported second factor: email_code
```

**Client Trust** is a Clerk security feature: a correct password from a *new device* is not enough; the
owner must also enter a one-time code sent to their e-mail. The old app only understood `complete`
and mapped every other status to that generic message, so the user could never finish signing in.

**Fix:** `ClerkAuthManager` is now a small state machine that handles every state Clerk can return:

| Clerk status | What the app does |
|---|---|
| `complete` | activates the session |
| `needs_client_trust` / `needs_second_factor` | e-mails (or texts) a code, shows **"Verify it's you"**, verifies it, then activates the session. Authenticator-app (TOTP) codes are supported too |
| `needs_new_password` | shows **"Choose a new password"** (Clerk forces this e.g. when a password appears in a data breach — this instance has `enforce_hibp_on_sign_in` on) |
| `needs_first_factor` | falls back to an e-mailed sign-in code |
| anything else | a message that names Clerk's raw status, instead of a silent dead end |

No session exists until the challenge is completed; nothing is faked or skipped.

## Other facts found in the Clerk configuration

* **Passwords must be 15+ characters** (and not found in a data breach). The app previously allowed 8.
  It now reads the rules from Clerk's public environment endpoint, so the sign-up and reset screens always
  show and enforce the instance's real policy.
* E-mail + password sign-in, with e-mail codes enabled. No social login, no phone, no MFA required.
* **Native API is enabled** — required for app sign-in.
* Rate limiting is real (`429 too_many_requests`); the app shows "Too many attempts, wait a minute".

## Screens and flows

```
Sign in ─┬─ correct credentials ──────────────► [code required?] ─► Verify it's you ─► signed in
         ├─ "Forgot password?" ─► e-mail ─► code ─► new password ─► back to Sign in (with notice)
         └─ "Create an account" ─► form ─► Verify your email ─► signed in (patient record created)
```

* **Registration** collects first name, last name, e-mail, password; Clerk e-mails a 6-digit code; on success the
  session is active. The backend creates the patient record on the first authenticated request (see
  `BACKEND_CHANGES.md`), so the account is immediately usable for booking without waiting for a webhook.
* **Forgot password** — Clerk's own reset flow: code e-mailed by Clerk → verified → new password (confirm field,
  policy checked) → Clerk confirms → the app **signs the user out and asks them to sign in with the new password**.
  It shows "Password updated" only after Clerk confirmed the change. The wording never reveals whether an e-mail is
  registered: an unknown address gets the same "If an account exists…" screen, and no code ever works for it.
  Resend has a 30-second cooldown plus Clerk's own rate limit. Codes are single-use and expire (Clerk enforces both).
* **Errors** handled with specific messages: wrong credentials, wrong/expired code, too many attempts, rate limit,
  existing account, weak/breached password, expired flow, no network, cancelled challenge.
* **State** — `ClerkAuthManager.authState` (`Initializing` / `SignedOut` / `SignedIn`) is observed by every screen.
  `Initializing` matters: the SDK restores a saved session *asynchronously* after launch, and the old code
  treated that moment as "signed out". A signed-in user opening *Review your booking* right after launch therefore
  saw the sign-in prompt, and it never went away. Screens now wait for the SDK and update live.
  Opening the sign-in screen while already signed in returns immediately.

## What you need to configure

Nothing new. Keep **Native API** enabled (Clerk dashboard → *Native applications*). Optional: *Customization →
Emails* controls the wording of the e-mails Clerk sends; delivery from a development instance uses Clerk's shared sender.

## Testing without a real inbox

Clerk development instances accept addresses containing `+clerk_test` (for example `qa.me+clerk_test@example.com`):
**no e-mail is sent and the code is always `424242`**. All automated tests use these. See
`tools/live-tests/` and the "Live tests" section of the README. The helper deletes nothing by itself —
the test users it creates can be removed from the Clerk dashboard (search `clerk_test`).

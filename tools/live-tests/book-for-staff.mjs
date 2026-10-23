// Registers a throw-away patient (Clerk `+clerk_test` address) and books one real appointment at clinic_01
// through POST /api/appointments, exactly like the app does. Prints shell exports so a staff test can look for it.
// SAFETY: refuses anything but a local backend, because it creates a user and an appointment.
import { Client, short } from "./clerk-frontend.mjs";

const BASE = process.env.BASE_URL ?? "http://localhost:3000";
if (!/^https?:\/\/(localhost|127\.0\.0\.1)(:|\/|$)/.test(BASE)) { console.error("Refusing: BASE_URL must be local."); process.exit(2); }
const rand = () => Math.random().toString(36).slice(2, 10);
const api = async (token, method, path, body) => {
  const res = await fetch(BASE + path, { method, headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}), ...(body ? { "Content-Type": "application/json" } : {}) }, body: body ? JSON.stringify(body) : undefined });
  return { status: res.status, json: await res.json().catch(() => ({})) };
};

const c = new Client();
let r = await c.signUp(`qa.booker.${rand()}+clerk_test@example.com`, `Clinic-QA-${rand()}${rand()}-Zq`, "QA", "Booker");
if (!r.resp?.id) throw new Error(`sign-up failed: ${short(r.errors)}`);
await c.prepareSignUp(r.resp.id);
r = await c.attemptSignUp(r.resp.id, "424242");
const token = await c.jwt(r.resp.created_session_id);

const docs = (await api(null, "GET", "/api/doctors/search?clinicId=clinic_01&pageSize=50")).json.data;
for (const d of docs) {
  for (let i = 1; i <= 14; i++) {
    const date = new Date(Date.now() + i * 86400000).toISOString().slice(0, 10);
    const slots = (await api(null, "GET", `/api/doctors/${d.id}/availability?date=${date}`)).json.data?.slots ?? [];
    if (!slots.length) continue;
    const res = await api(token, "POST", "/api/appointments", { doctorId: d.id, date, startTime: slots[0], notes: "QA live staff-visibility booking" });
    if (res.status !== 201) throw new Error(`booking failed: ${res.status} ${JSON.stringify(res.json)}`);
    const id = res.json.data.id;
    await c.signOut(r.resp.created_session_id);
    // Single words only: `adb shell am instrument -e key value` breaks on spaces.
    console.log(`export BOOKED_PATIENT='Booker' BOOKED_DOCTOR='${d.name.split(" ").pop()}' BOOKED_REF='${id.slice(-6).toUpperCase()}'`);
    process.exit(0);
  }
}
throw new Error("no free slot at clinic_01");

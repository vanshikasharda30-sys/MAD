// Creates a Clerk test user whose e-mail matches a pending StaffUser invite in a LOCAL database.
// Prints shell exports. Refuses non-local databases.
import { execFileSync } from "node:child_process";
import { Client } from "./clerk-frontend.mjs";

const DB = process.env.DATABASE_URL ?? "";
if (!/@(localhost|127\.0\.0\.1)[:/]/.test(DB)) { console.error("Refusing: DATABASE_URL must be local."); process.exit(2); }
const rand = () => Math.random().toString(36).slice(2, 10);
const email = `qa.staff.${rand()}+clerk_test@example.com`;
const password = `Clinic-QA-${rand()}${rand()}-Zq`;
execFileSync("psql", [DB, "-tAc",
  `INSERT INTO "StaffUser"(id,"clinicId",name,email,role,"isActive","createdAt") VALUES ('qa_${rand()}','clinic_01','QA Admin','${email}','ADMIN',true,now())`]);
const c = new Client();
let r = await c.signUp(email, password, "QA", "Admin");
await c.prepareSignUp(r.resp.id);
r = await c.attemptSignUp(r.resp.id, "424242");
await c.signOut(r.resp.created_session_id);
console.log(`export STAFF_EMAIL='${email}' STAFF_PASSWORD='${password}'`);

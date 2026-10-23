// Creates the two throw-away Clerk accounts the live tests need, using Clerk's `+clerk_test`
// addresses (no e-mail is sent; the verification code is always 424242). Prints shell exports.
import { Client, short } from "./clerk-frontend.mjs";

const rand = () => Math.random().toString(36).slice(2, 10);
const password = () => `Clinic-QA-${rand()}${rand()}-Zq`;

async function create(tag) {
  const email = `qa.${tag}.${rand()}+clerk_test@example.com`;
  const pw = password();
  const c = new Client();
  let r = await c.signUp(email, pw, "QA", `${tag[0].toUpperCase()}${tag.slice(1)}`);
  if (!r.resp?.id) throw new Error(`sign-up failed: ${short(r.errors)}`);
  await c.prepareSignUp(r.resp.id);
  r = await c.attemptSignUp(r.resp.id, "424242");
  if (r.resp?.status !== "complete") throw new Error(`verification failed: ${short(r.errors)}`);
  await c.signOut(r.resp.created_session_id);
  return { email, pw };
}

const qa = await create("existing");
await new Promise((res) => setTimeout(res, 2500)); // stay under Clerk's rate limit
const reset = await create("reset");
console.log(`export QA_EMAIL='${qa.email}' QA_PASSWORD='${qa.pw}'`);
console.log(`export RESET_EMAIL='${reset.email}' RESET_OLD_PASSWORD='${reset.pw}' RESET_NEW_PASSWORD='${password()}'`);

// Minimal Clerk Frontend API client mirroring the Android (native) SDK flows. Test helper only.
const HOST = "https://pumped-eft-15.clerk.accounts.dev";
export class Client {
  constructor() { this.token = null; }
  async call(method, path, form) {
    const url = `${HOST}${path}${path.includes("?") ? "&" : "?"}_is_native=1`;
    const headers = { "Content-Type": "application/x-www-form-urlencoded" };
    if (this.token) headers.Authorization = this.token;
    const res = await fetch(url, { method, headers, body: form ? new URLSearchParams(form).toString() : undefined });
    const auth = res.headers.get("authorization");
    if (auth) this.token = auth;
    const json = await res.json().catch(() => ({}));
    return { status: res.status, json, resp: json.response, errors: json.errors };
  }
  signUp(email, password, first, last) { return this.call("POST", "/v1/client/sign_ups", { email_address: email, password, first_name: first, last_name: last }); }
  prepareSignUp(id) { return this.call("POST", `/v1/client/sign_ups/${id}/prepare_verification`, { strategy: "email_code" }); }
  attemptSignUp(id, code) { return this.call("POST", `/v1/client/sign_ups/${id}/attempt_verification`, { strategy: "email_code", code }); }
  signIn(email, password) { return this.call("POST", "/v1/client/sign_ins", { identifier: email, password, strategy: "password" }); }
  signInCreate(email) { return this.call("POST", "/v1/client/sign_ins", { identifier: email }); }
  prepareFirst(id, strategy, extra) { return this.call("POST", `/v1/client/sign_ins/${id}/prepare_first_factor`, { strategy, ...extra }); }
  attemptFirst(id, strategy, code) { return this.call("POST", `/v1/client/sign_ins/${id}/attempt_first_factor`, { strategy, code }); }
  prepareSecond(id, strategy, extra) { return this.call("POST", `/v1/client/sign_ins/${id}/prepare_second_factor`, { strategy, ...extra }); }
  attemptSecond(id, strategy, code) { return this.call("POST", `/v1/client/sign_ins/${id}/attempt_second_factor`, { strategy, code }); }
  resetPassword(id, password) { return this.call("POST", `/v1/client/sign_ins/${id}/reset_password`, { password }); }
  async jwt(sessionId) { const r = await this.call("POST", `/v1/client/sessions/${sessionId}/tokens`); return r.json.jwt; }
  signOut(sessionId) { return this.call("POST", `/v1/client/sessions/${sessionId}/remove`); }
}
export const short = (e) => (e || []).map((x) => `${x.code}: ${x.long_message || x.message}`).join(" | ");

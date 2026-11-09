// Islamic Azkar — syncs the azkar record between a person's devices (iPhone, iPad, Mac, Android,
// website). Cloudflare Pages Function at islamicazkar.net/sync:
//   POST /sync/new  {days?}        → {code, days}  creates a private 12-character sync code
//   POST /sync      {code, days}   → {days}        merges this device's record and returns the whole
//   POST /sync/delete {code}       → {deleted}     erases the record kept for that code (islamicazkar.net/delete)
// No account: only the record (day → completed readings) is kept, in KV under a hash of the code.

const CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";   // no 0/O, 1/I/L
const DAY_RE = /^\d{4}-\d{2}-\d{2}$/;
const READING_RE = /^(morning|evening|c\d{1,4})$/;
const ORIGINS = /^https:\/\/((www\.)?islamicazkar\.net|([a-z0-9-]+\.)?islamic-azkar\.pages\.dev)$|^http:\/\/localhost(:\d+)?$/;

function cors(request) {
  const origin = request.headers.get("origin") || "";
  return ORIGINS.test(origin)
    ? { "access-control-allow-origin": origin, "access-control-allow-methods": "POST, OPTIONS", "access-control-allow-headers": "content-type", vary: "origin" }
    : {};
}
const json = (body, status, headers) =>
  new Response(JSON.stringify(body), { status, headers: { "content-type": "application/json", "cache-control": "no-store", ...headers } });

const normalizeCode = c => String(c || "").toUpperCase().replace(/[^A-Z0-9]/g, "");
async function storageKey(code) {
  const bytes = new Uint8Array(await crypto.subtle.digest("SHA-256", new TextEncoder().encode(`islamic-azkar:${code}`)));
  return "sync:" + [...bytes].map(b => b.toString(16).padStart(2, "0")).join("");
}
function newCode() {
  const r = crypto.getRandomValues(new Uint8Array(12));
  return [...r].map(b => CODE_ALPHABET[b % CODE_ALPHABET.length]).join("");
}
/** Keeps only well-formed days and readings (at most ~5 years, 60 readings a day). */
function cleanDays(days) {
  const out = {};
  if (!days || typeof days !== "object" || Array.isArray(days)) return out;
  for (const [day, list] of Object.entries(days).slice(-2000)) {
    if (!DAY_RE.test(day) || !Array.isArray(list)) continue;
    const ok = [...new Set(list.filter(k => typeof k === "string" && READING_RE.test(k)))].slice(0, 60);
    if (ok.length) out[day] = ok;
  }
  return out;
}
function mergeDays(a, b) {
  const out = { ...a };
  let changed = false;
  for (const [day, list] of Object.entries(b)) {
    const have = out[day] || [];
    const add = list.filter(k => !have.includes(k));
    if (add.length) { out[day] = [...have, ...add]; changed = true; }
  }
  return { days: out, changed };
}

async function create(request, env, headers) {
  // Light throttle on new codes: at most 10 per IP per hour.
  const ip = request.headers.get("cf-connecting-ip") || "unknown";
  const rateKey = `rate:sync:${ip}`;
  const hits = Number(await env.SYNC.get(rateKey)) || 0;
  if (hits >= 10) return json({ error: "rate_limited" }, 429, headers);
  await env.SYNC.put(rateKey, String(hits + 1), { expirationTtl: 3600 });

  let body = {};
  try { body = await request.json(); } catch { /* no body */ }
  const code = newCode();
  const days = cleanDays(body.days);
  await env.SYNC.put(await storageKey(code), JSON.stringify({ days, updated: Date.now() }));
  return json({ code, days }, 200, headers);
}

async function merge(request, env, headers) {
  let body;
  try { body = await request.json(); } catch { return json({ error: "bad_request" }, 400, headers); }
  const code = normalizeCode(body.code);
  if (code.length !== 12) return json({ error: "unknown_code" }, 404, headers);
  const key = await storageKey(code);
  const stored = await env.SYNC.get(key, "json");
  if (!stored) return json({ error: "unknown_code" }, 404, headers);
  const { days, changed } = mergeDays(stored.days || {}, cleanDays(body.days));
  // KV writes are limited: only write when this device brought something new.
  if (changed) await env.SYNC.put(key, JSON.stringify({ days, updated: Date.now() }));
  return json({ days }, 200, headers);
}

async function erase(request, env, headers) {
  let body;
  try { body = await request.json(); } catch { return json({ error: "bad_request" }, 400, headers); }
  const code = normalizeCode(body.code);
  if (code.length !== 12) return json({ error: "unknown_code" }, 404, headers);
  const key = await storageKey(code);
  if (!(await env.SYNC.get(key))) return json({ error: "unknown_code" }, 404, headers);
  await env.SYNC.delete(key);
  return json({ deleted: true }, 200, headers);
}

export async function onRequest({ request, env, params }) {
  const headers = cors(request);
  if (request.method === "OPTIONS") return new Response(null, { status: 204, headers });
  if (request.method !== "POST") return json({ error: "method_not_allowed" }, 405, headers);
  if (!env.SYNC) return json({ error: "not_configured" }, 503, headers);
  const path = (params.path || []).join("/");
  if (path === "new") return create(request, env, headers);
  if (path === "delete") return erase(request, env, headers);
  if (path === "") return merge(request, env, headers);
  return json({ error: "not_found" }, 404, headers);
}

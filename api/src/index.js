// Islamic Azkar — website API.
//  - POST /api/register: tester sign-ups from the website, kept in KV and emailed to the developer.
//  - POST /api/sync/new, POST /api/sync: the azkar record synced between a person's devices
//    (iPhone, iPad, Mac, Android, website). No account: devices share a private sync code; the
//    server keeps only the record (days → completed readings) under a hash of that code.

const ORIGINS = new Set([
  "https://islamicazkar.net",
  "https://www.islamicazkar.net",
  "https://islamic-azkar.pages.dev",
]);
const EMAIL_RE = /^[^\s@]{1,64}@[^\s@]{1,190}\.[^\s@]{2,}$/;

function cors(request) {
  const origin = request.headers.get("origin") || "";
  const ok = ORIGINS.has(origin) || origin.endsWith(".islamic-azkar.pages.dev") || origin.startsWith("http://localhost:");
  return ok ? { "access-control-allow-origin": origin, "access-control-allow-methods": "POST, OPTIONS", "access-control-allow-headers": "content-type", vary: "origin" } : {};
}

const json = (body, status, headers) => new Response(JSON.stringify(body), { status, headers: { "content-type": "application/json", ...headers } });

async function register(request, env) {
  const headers = cors(request);
  let body;
  try { body = await request.json(); } catch { return json({ error: "bad_request" }, 400, headers); }

  // Bots fill the hidden "website" field; pretend success so they move on.
  if (body.website) return json({ ok: true }, 200, headers);

  const email = String(body.email || "").trim().toLowerCase();
  if (email.length > 254 || !EMAIL_RE.test(email)) return json({ error: "invalid_email" }, 400, headers);
  const lang = ["ar", "en", "fr"].includes(body.lang) ? body.lang : "en";

  // Light throttle: at most 5 sign-ups per IP per hour.
  const ip = request.headers.get("cf-connecting-ip") || "unknown";
  const rateKey = `rate:${ip}`;
  const hits = Number(await env.TESTERS.get(rateKey)) || 0;
  if (hits >= 5) return json({ error: "rate_limited" }, 429, headers);
  await env.TESTERS.put(rateKey, String(hits + 1), { expirationTtl: 3600 });

  const key = `tester:${email}`;
  if (await env.TESTERS.get(key)) return json({ ok: true, already: true }, 200, headers);

  const country = request.cf?.country || "";
  const at = new Date().toISOString();
  await env.TESTERS.put(key, JSON.stringify({ email, lang, country, at }));

  const text = [
    "New tester sign-up from islamicazkar.net",
    "",
    `Email:    ${email}`,
    `Language: ${lang}`,
    `Country:  ${country || "unknown"}`,
    `Time:     ${at}`,
    "",
    "Add this address to the \"Mushaf Testers\" list in Play Console (Testing > Closed testing > Testers).",
  ].join("\n");
  try {
    await env.EMAIL.send({
      from: { email: env.FROM_ADDRESS, name: "Islamic Azkar" },
      to: env.NOTIFY_ADDRESS,
      replyTo: email,
      subject: `New tester: ${email}`,
      text,
    });
  } catch (e) {
    // The sign-up is already saved in KV, so the visitor still gets a success reply.
    console.error("email send failed", e.code, e.message);
  }
  return json({ ok: true }, 200, headers);
}

// ───────────── Sync ─────────────
const CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";   // no 0/O, 1/I/L
const DAY_RE = /^\d{4}-\d{2}-\d{2}$/;
const READING_RE = /^(morning|evening|c\d{1,4})$/;

const normalizeCode = c => String(c || "").toUpperCase().replace(/[^A-Z0-9]/g, "");
async function syncKey(code) {
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
  if (!days || typeof days !== "object") return out;
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

async function syncNew(request, env, headers) {
  // Light throttle on creating codes: at most 10 per IP per hour.
  const ip = request.headers.get("cf-connecting-ip") || "unknown";
  const rateKey = `rate:sync:${ip}`;
  const hits = Number(await env.TESTERS.get(rateKey)) || 0;
  if (hits >= 10) return json({ error: "rate_limited" }, 429, headers);
  await env.TESTERS.put(rateKey, String(hits + 1), { expirationTtl: 3600 });

  let body = {};
  try { body = await request.json(); } catch { /* empty body */ }
  const code = newCode();
  const days = cleanDays(body.days);
  await env.TESTERS.put(await syncKey(code), JSON.stringify({ days, updated: Date.now() }));
  return json({ code, days }, 200, headers);
}

async function sync(request, env, headers) {
  let body;
  try { body = await request.json(); } catch { return json({ error: "bad_request" }, 400, headers); }
  const code = normalizeCode(body.code);
  if (code.length !== 12) return json({ error: "unknown_code" }, 404, headers);
  const key = await syncKey(code);
  const stored = await env.TESTERS.get(key, "json");
  if (!stored) return json({ error: "unknown_code" }, 404, headers);
  const { days, changed } = mergeDays(stored.days || {}, cleanDays(body.days));
  // KV writes are limited: only write when this device brought something new.
  if (changed) await env.TESTERS.put(key, JSON.stringify({ days, updated: Date.now() }));
  return json({ days }, 200, headers);
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (url.pathname === "/api/register") {
      if (request.method === "OPTIONS") return new Response(null, { status: 204, headers: cors(request) });
      if (request.method === "POST") return register(request, env);
      return json({ error: "method_not_allowed" }, 405, cors(request));
    }
    if (url.pathname === "/api/sync" || url.pathname === "/api/sync/new") {
      // The apps send no Origin header; the website must be one of ours.
      const headers = cors(request);
      if (request.method === "OPTIONS") return new Response(null, { status: 204, headers });
      if (request.method !== "POST") return json({ error: "method_not_allowed" }, 405, headers);
      return url.pathname === "/api/sync/new" ? syncNew(request, env, headers) : sync(request, env, headers);
    }
    return json({ error: "not_found" }, 404);
  },
};

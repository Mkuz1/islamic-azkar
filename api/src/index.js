// Islamic Azkar — website API. Receives tester sign-ups from the website, keeps them in KV
// and emails each new one to the developer so they can be added to the Play closed test.

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

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (url.pathname === "/api/register") {
      if (request.method === "OPTIONS") return new Response(null, { status: 204, headers: cors(request) });
      if (request.method === "POST") return register(request, env);
      return json({ error: "method_not_allowed" }, 405, cors(request));
    }
    return json({ error: "not_found" }, 404);
  },
};

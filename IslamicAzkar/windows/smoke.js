// Launches the installed Windows app and checks that it works: the azkar load, a reader opens, the
// page can't reach Node, and no security policy error appears. Saves screenshots to shots/.
// Usage: node smoke.js "<path to Islamic Azkar.exe>"
const { _electron } = require("playwright-core");
const fs = require("fs");
(async () => {
  fs.mkdirSync("shots", { recursive: true });
  const app = await _electron.launch({ executablePath: process.argv[2], timeout: 60000 });
  const page = await app.firstWindow();
  const problems = [];
  page.on("console", m => { if (m.type() === "error") problems.push(m.text()); });
  page.on("pageerror", e => problems.push(String(e)));
  await page.waitForSelector("#home-body .tiles a, #home-body .tile", { timeout: 30000 });
  console.log("title:", await page.title(), "| url:", page.url());
  await page.screenshot({ path: "shots/home.png" });
  const nodeAccess = await page.evaluate(() => typeof require !== "undefined" || typeof process !== "undefined");
  console.log(nodeAccess ? "FAIL page can reach Node" : "PASS page has no Node access");
  await page.evaluate(() => { location.hash = "#/read/morning"; });
  await page.waitForSelector(".card, article", { timeout: 15000 });
  await page.screenshot({ path: "shots/reader.png" });
  console.log("PASS morning azkar open:", await page.locator(".card, article").count(), "items");
  // The sync server accepts the app (CORS): an unknown code answers 404, not a network error.
  const sync = await page.evaluate(() => fetch("https://islamicazkar.net/sync", { method: "POST", headers: { "content-type": "application/json" },
    body: JSON.stringify({ code: "AAAAAAAAAAAA", days: {} }) }).then(r => r.status, e => "error " + e));
  console.log(sync === 404 ? "PASS sync server reachable from the app" : `INFO sync answered ${sync} (the server's CORS update deploys with the website)`);
  const csp = problems.filter(p => /Content Security Policy|Refused to/.test(p));
  console.log(csp.length ? "FAIL " + csp.join(" | ") : "PASS no security policy errors");
  if (problems.length) console.log("console errors:", problems);
  await app.close();
  if (nodeAccess || csp.length) process.exit(1);
})().catch(e => { console.error(e); process.exit(1); });

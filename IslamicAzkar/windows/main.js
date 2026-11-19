// Islamic Azkar for Windows: the website's app (azkar, prayer times, tasbih, record and sync) in its
// own window. The files ship inside the app and are served from app://islamic-azkar/; the page runs
// sandboxed, with no access to the computer, and links to other sites open in the browser.
const { app, BrowserWindow, protocol, net, shell, session, dialog, Menu } = require("electron");
const path = require("path");
const { pathToFileURL } = require("url");

const SCHEME = "app", HOST = "islamic-azkar", ORIGIN = `${SCHEME}://${HOST}`;
const WEB = path.join(__dirname, "web");
const RELEASES = "https://api.github.com/repos/Mkuz1/islamic-azkar-releases/releases/latest";
const SETUP_URL = "https://github.com/Mkuz1/islamic-azkar-releases/releases/latest/download/IslamicAzkar-Setup.exe";
const CSP = "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; " +
  "connect-src 'self' https://islamicazkar.net; object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'";
const TYPES = { ".html": "text/html; charset=utf-8", ".js": "text/javascript; charset=utf-8", ".css": "text/css; charset=utf-8",
  ".json": "application/json", ".png": "image/png", ".jpg": "image/jpeg", ".svg": "image/svg+xml", ".ttf": "font/ttf", ".woff2": "font/woff2" };

protocol.registerSchemesAsPrivileged([{ scheme: SCHEME, privileges: { standard: true, secure: true, supportFetchAPI: true, corsEnabled: true } }]);
app.setAppUserModelId("com.koussa.IslamicAzkar");
if (!app.requestSingleInstanceLock()) app.quit();

let win = null;

/** app://islamic-azkar/<path> → a file in web/ (never outside it). */
async function serve(request) {
  const url = new URL(request.url);
  if (url.host !== HOST) return new Response("Not found", { status: 404 });
  let rel = decodeURIComponent(url.pathname);
  if (rel.endsWith("/")) rel += "index.html";
  const file = path.normalize(path.join(WEB, rel));
  if (!file.startsWith(WEB + path.sep)) return new Response("Not found", { status: 404 });
  const res = await net.fetch(pathToFileURL(file).toString()).catch(() => null);
  if (!res || !res.ok) return new Response("Not found", { status: 404 });
  return new Response(res.body, { status: 200, headers: {
    "content-type": TYPES[path.extname(file).toLowerCase()] || "application/octet-stream",
    "content-security-policy": CSP, "x-content-type-options": "nosniff" } });
}

function openOutside(url) {
  try { if (new URL(url).protocol === "https:") shell.openExternal(url); } catch { /* not a URL */ }
}

function createWindow() {
  win = new BrowserWindow({
    width: 1240, height: 860, minWidth: 380, minHeight: 560,
    backgroundColor: "#0a1c15", title: "Islamic Azkar", autoHideMenuBar: true, show: false,
    icon: path.join(__dirname, "web", "img", "icon.png"),
    webPreferences: { contextIsolation: true, sandbox: true, nodeIntegration: false, webviewTag: false, spellcheck: false },
  });
  win.once("ready-to-show", () => win.show());
  // Only the app's own pages load here; anything else (store links, downloads) opens in the browser.
  win.webContents.on("will-navigate", (e, url) => { if (!url.startsWith(ORIGIN + "/")) { e.preventDefault(); openOutside(url); } });
  win.webContents.setWindowOpenHandler(({ url }) => { openOutside(url); return { action: "deny" }; });
  win.loadURL(`${ORIGIN}/index.html`);
}

/** Tells the person when a newer version is on GitHub (once per launch, quietly offline). */
async function checkForUpdate() {
  try {
    const res = await net.fetch(RELEASES, { headers: { "User-Agent": "IslamicAzkar-Windows", Accept: "application/vnd.github+json" } });
    if (!res.ok) return;
    const latest = String((await res.json()).tag_name || "").replace(/^v/, "");
    const newer = (a, b) => { const x = a.split(".").map(Number), y = b.split(".").map(Number);
      for (let i = 0; i < Math.max(x.length, y.length); i++) { if ((x[i] || 0) !== (y[i] || 0)) return (x[i] || 0) > (y[i] || 0); } return false; };
    if (!latest || !newer(latest, app.getVersion())) return;
    const lang = (app.getLocale() || "en").slice(0, 2);
    const t = lang === "ar" ? ["تحديث متاح", `الإصدار ${latest} من أذكار إسلامية متاح.`, "تنزيل", "لاحقًا"]
      : lang === "fr" ? ["Mise à jour disponible", `La version ${latest} d'Islamic Azkar est disponible.`, "Télécharger", "Plus tard"]
      : ["Update available", `Version ${latest} of Islamic Azkar is available.`, "Download", "Later"];
    const { response } = await dialog.showMessageBox(win, { type: "info", title: t[0], message: t[1], buttons: [t[2], t[3]], defaultId: 0, cancelId: 1 });
    if (response === 0) shell.openExternal(SETUP_URL);
  } catch { /* offline */ }
}

app.on("second-instance", () => { if (win) { if (win.isMinimized()) win.restore(); win.focus(); } });
app.on("window-all-closed", () => app.quit());
app.on("web-contents-created", (_, contents) => contents.on("will-attach-webview", e => e.preventDefault()));

app.whenReady().then(() => {
  Menu.setApplicationMenu(null);
  protocol.handle(SCHEME, serve);
  // The page may ask for the location (prayer times) and write to the clipboard (copy a dhikr); nothing else.
  session.defaultSession.setPermissionRequestHandler((wc, permission, done) =>
    done(["geolocation", "clipboard-sanitized-write", "notifications"].includes(permission) && wc.getURL().startsWith(ORIGIN + "/")));
  createWindow();
  setTimeout(checkForUpdate, 5000);
});

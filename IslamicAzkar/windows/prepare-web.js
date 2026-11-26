// Copies the website (../../website) into web/, the files the Windows app shows: the same azkar,
// prayer times, tasbih and sync as islamicazkar.net. Server-only files are left out.
const fs = require("fs"), path = require("path");
const src = path.join(__dirname, "..", "..", "website"), dst = path.join(__dirname, "web");
const skip = new Set(["functions", "wrangler.toml", "_headers", "_redirects", "apple", "dist", "node_modules", "delete"]);
fs.rmSync(dst, { recursive: true, force: true });
(function copy(from, to) {
  fs.mkdirSync(to, { recursive: true });
  for (const e of fs.readdirSync(from, { withFileTypes: true })) {
    if (skip.has(e.name) || e.name.startsWith(".")) continue;
    const a = path.join(from, e.name), b = path.join(to, e.name);
    e.isDirectory() ? copy(a, b) : fs.copyFileSync(a, b);
  }
})(src, dst);
fs.mkdirSync(path.join(__dirname, "build"), { recursive: true });
fs.copyFileSync(path.join(__dirname, "..", "..", "store", "icon_512.png"), path.join(__dirname, "build", "icon.png"));
console.log("web/ ready from", src);

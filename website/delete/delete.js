const f = document.getElementById("f"), msg = document.getElementById("msg"), go = document.getElementById("go");
  f.addEventListener("submit", async e => {
    e.preventDefault();
    const code = f.code.value.toUpperCase().replace(/[^A-Z0-9]/g, "");
    if (code.length !== 12) { msg.className = "err"; msg.textContent = "Enter the 12-character code (e.g. ABCD-EFGH-JKMN)."; return; }
    if (!confirm("Permanently delete the azkar record stored for this code?")) return;
    go.disabled = true; msg.className = ""; msg.textContent = "…";
    try {
      const r = await fetch("/sync/delete", { method: "POST", headers: { "content-type": "application/json" }, body: JSON.stringify({ code }) });
      if (r.status === 404) { msg.className = "err"; msg.textContent = "No data is stored for this code (it may already be deleted)."; }
      else if (!r.ok) throw 0;
      else { msg.className = "ok"; msg.textContent = "Done. Your data has been deleted. · تم حذف بياناتك. · Vos données ont été supprimées."; f.reset(); }
    } catch { msg.className = "err"; msg.textContent = "Couldn't connect. Please try again."; }
    go.disabled = false;
  });

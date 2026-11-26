// islamicazkar.net landing page: scroll reveals, the word by word tagline, the mobile menu and the
// current section in the navigation. Old links to the web app (islamicazkar.net/#/read/...) go to /app/.
(() => {
  if (location.hash.startsWith("#/")) { location.replace("/app/" + location.search + location.hash); return; }
  const reduce = matchMedia("(prefers-reduced-motion: reduce)").matches;

  // Sections below the first screen rise once as they enter it; anything already on screen stays put.
  const io = new IntersectionObserver(es => es.forEach(e => {
    if (e.isIntersecting) { e.target.classList.remove("below"); io.unobserve(e.target); }
  }), { rootMargin: "0px 0px -10% 0px" });
  document.querySelectorAll(".reveal").forEach(el => {
    if (el.getBoundingClientRect().top > innerHeight) { el.classList.add("below"); io.observe(el); }
  });

  // Tagline: each word lights up as it crosses a line two thirds down the screen.
  const tag = document.getElementById("tagline");
  if (tag) {
    tag.innerHTML = tag.textContent.trim().split(/\s+/).map(w => `<span class="w">${w.replace(/[&<>]/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;" }[c]))}</span>`).join(" ");
    const words = [...tag.querySelectorAll(".w")];
    if (reduce) words.forEach(w => w.classList.add("on"));
    else {
      const wio = new IntersectionObserver(es => es.forEach(e => {
        if (e.isIntersecting) { e.target.classList.add("on"); wio.unobserve(e.target); }
      }), { rootMargin: "0px 0px -33% 0px" });
      words.forEach(w => wio.observe(w));
    }
  }

  // Mobile menu: a full height panel.
  const btn = document.getElementById("menu-btn"), menu = document.getElementById("menu"), close = document.getElementById("menu-close");
  const set = open => {
    btn.setAttribute("aria-expanded", open);
    menu.classList.toggle("open", open);
    menu.setAttribute("aria-hidden", !open);
    if (open) close.focus(); else if (menu.contains(document.activeElement)) btn.focus();
  };
  btn.addEventListener("click", () => set(btn.getAttribute("aria-expanded") !== "true"));
  close.addEventListener("click", () => set(false));
  menu.addEventListener("click", e => { if (e.target.closest("a[href^='#']")) set(false); });
  document.addEventListener("keydown", e => { if (e.key === "Escape") set(false); });

  // The section on screen is marked in the navigation.
  const links = [...document.querySelectorAll(".nav-links a")];
  const sio = new IntersectionObserver(es => es.forEach(e => {
    if (e.isIntersecting) links.forEach(a => a.hash === "#" + e.target.id ? a.setAttribute("aria-current", "page") : a.removeAttribute("aria-current"));
  }), { rootMargin: "-45% 0px -50% 0px" });
  ["read", "features", "how", "faq"].forEach(id => { const s = document.getElementById(id); if (s) sio.observe(s); });
})();

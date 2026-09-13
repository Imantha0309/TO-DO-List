// ── landing.js  (dark landing page) ────────────────────────────────
// No mountNav() — the landing has its own static nav in index.html.
// Redirect logged-in users straight to the dashboard.
if (typeof store !== "undefined" && store.getToken()) {
  location.replace("dashboard.html");
}

// ── Sticky nav shadow on scroll ──────────────────────────────────
(function () {
  const nav = document.getElementById("lnNav");
  if (!nav) return;
  function onScroll() {
    nav.classList.toggle("scrolled", window.scrollY > 30);
  }
  window.addEventListener("scroll", onScroll, { passive: true });
  onScroll();
})();

// ── Reveal-on-scroll for .ln-reveal elements ─────────────────────
(function () {
  const els = document.querySelectorAll(".ln-reveal");
  if (!els.length) return;
  const io = new IntersectionObserver(
    (entries) => {
      entries.forEach((e) => {
        if (e.isIntersecting) {
          e.target.classList.add("ln-in");
          io.unobserve(e.target);
        }
      });
    },
    { threshold: 0.06 }
  );
  els.forEach((el) => io.observe(el));
})();

// ── Animated progress ring in the preview card ───────────────────
(function () {
  const ring = document.getElementById("previewRing");
  if (!ring) return;
  const txt = ring.querySelector(".ln-ring-txt");
  const target = 62;
  let val = 0;
  const timer = setInterval(() => {
    val += 1;
    ring.style.setProperty("--val", String(val));
    if (txt) txt.textContent = val + "%";
    if (val >= target) clearInterval(timer);
  }, 22);
})();

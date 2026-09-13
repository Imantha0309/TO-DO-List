function esc(s) {
  if (s === null || s === undefined) return "";
  return String(s)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

function todayISO() {
  return new Date().toISOString().slice(0, 10);
}

function fmtDate(iso) {
  if (!iso) return "—";
  const d = new Date(iso.length === 10 ? iso + "T00:00:00" : iso);
  if (isNaN(d.getTime())) return iso;
  return d.toLocaleDateString(undefined, { month: "short", day: "numeric" });
}

function fmtDateLong(iso) {
  if (!iso) return "—";
  const d = new Date(iso.length === 10 ? iso + "T00:00:00" : iso);
  if (isNaN(d.getTime())) return iso;
  return d.toLocaleDateString(undefined, { weekday: "short", month: "short", day: "numeric", year: "numeric" });
}

function daysUntil(iso) {
  if (!iso) return null;
  const now = new Date();
  now.setHours(0, 0, 0, 0);
  const then = new Date(iso.length === 10 ? iso + "T00:00:00" : iso);
  return Math.round((then - now) / 86400000);
}

function dueLabel(iso) {
  const d = daysUntil(iso);
  if (d === null) return esc(iso);
  if (d === 0) return "Due today";
  if (d === 1) return "Due tomorrow";
  if (d < 0) return `Overdue by ${Math.abs(d)}d`;
  return `In ${d}d`;
}

function initials(name) {
  if (!name) return "?";
  return name
    .split(" ")
    .map((p) => p[0])
    .slice(0, 2)
    .join("")
    .toUpperCase();
}

function toast(message, type = "", ms = 3200) {
  let stack = document.querySelector(".toast-stack");
  if (!stack) {
    stack = document.createElement("div");
    stack.className = "toast-stack";
    document.body.appendChild(stack);
  }
  const t = document.createElement("div");
  t.className = `toast ${type}`;
  t.textContent = message;
  stack.appendChild(t);
  setTimeout(() => {
    t.classList.add("hide");
    setTimeout(() => t.remove(), 350);
  }, ms);
}

function toastAchievement(ach) {
  toast(`Achievement unlocked: ${ach.name}`, "achieve", 5200);
}

function renderNav(active) {
  const user = store.getRawUser();
  const initialsEl = user ? initials(user.name) : "";
  return `
    <nav class="nav">
      <a class="brand" href="${user ? "dashboard.html" : "index.html"}">
        <span class="logo">✦</span> StudyForge
      </a>
      ${
        user
          ? `<div class="nav-links">
              <a href="dashboard.html" class="${active === "dashboard" ? "active" : ""}">Dashboard</a>
              <a href="create.html" class="${active === "create" ? "active" : ""}">New Target</a>
              <a href="achievements.html" class="${active === "achievements" ? "active" : ""}">Achievements</a>
              <button class="ghost-user" onclick="logout()" title="Log out">
                <span class="avatar">${esc(initialsEl)}</span>
                ${esc(user.name.split(" ")[0])} ·
                Log out
              </button>
            </div>`
          : `<div class="nav-links">
              <a href="login.html">Log in</a>
              <a href="signup.html" class="btn btn-brand btn-sm">Get Started</a>
            </div>`
      }
    </nav>`;
}

function mountNav(active) {
  const slot = document.getElementById("nav-slot");
  if (slot) slot.innerHTML = renderNav(active);
}

function logout() {
  store.clear();
  location.href = "index.html";
}

function requireAuth() {
  if (!store.getToken()) {
    location.href = "login.html";
    return false;
  }
  return true;
}

function setupReveal() {
  const io = new IntersectionObserver(
    (entries) => {
      entries.forEach((e) => {
        if (e.isIntersecting) {
          e.target.classList.add("in");
          io.unobserve(e.target);
        }
      });
    },
    { threshold: 0.08 }
  );
  document.querySelectorAll(".reveal").forEach((el) => io.observe(el));
}

function notifyNewly(newly) {
  (newly || []).forEach((a) => toastAchievement(a));
}
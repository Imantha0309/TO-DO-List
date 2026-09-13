if (!requireAuth()) throw new Error("no auth");
mountNav("achievements");

const $ = (id) => document.getElementById(id);

const icons = {
  target: "🎯",
  trophy: "🏆",
  bolt: "⚡",
  rocket: "🚀",
  star: "⭐",
  flag: "🚩",
};

async function load() {
  try {
    const res = await API.achievements();
    const unlocked = res.newlyUnlocked || [];
    unlockSeq(unlocked);

    $("achGrid").innerHTML = res.achievements
      .map(
        (a) => `
        <div class="card ach-card ${a.unlocked ? "" : "locked"}">
          <div class="ach-ico">${icons[a.icon] || "✦"}</div>
          <h3>${esc(a.name)}</h3>
          <p>${esc(a.description)}</p>
          <span style="font-size:11px;color:var(--faint);font-weight:700;">
            ${a.unlocked ? "Unlocked" + (a.unlockedAt ? " · " + new Date(a.unlockedAt).toLocaleDateString() : "") : "Locked"}
          </span>
        </div>`
      )
      .join("");
  } catch (err) {
    toast(err.message, "error");
  }
}

function unlockSeq(list) {
  list.forEach((a, i) => setTimeout(() => toastAchievement(a), i * 700));
}

load();
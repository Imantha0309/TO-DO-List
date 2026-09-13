if (!requireAuth()) throw new Error("no auth");
mountNav("dashboard");

let allTargets = [];

const STATUS_ORDER = { ACTIVE: 0, COMPLETED: 1, ARCHIVED: 2 };

async function load() {
  try {
    const [d, targets] = await Promise.all([API.dashboard(), API.listTargets()]);
    allTargets = targets || [];

    const stats = d.quickStats || [];
    document.getElementById("statGrid").innerHTML = stats
      .map(
        (s) => `
        <div class="card stat-card">
          <div class="num">${s.value}<span style="font-size:16px;">${s.label === "Overall progress" ? "%" : ""}</span></div>
          <div class="lbl">${esc(s.label)}</div>
        </div>`
      )
      .join("");

    const todayEl = document.getElementById("todayList");
    todayEl.innerHTML = d.todayTasks.length
      ? d.todayTasks
          .map(
            (t) => `
          <div class="task-row" style="cursor:pointer" onclick="location.href='target.html?id=${t.targetId}'">
            <span class="t-check"></span>
            <div class="t-info">
              <div class="t-title">${esc(t.title)}</div>
              <div class="t-meta">
                <span>${esc(t.targetTitle)}</span>
                ${t.dueDate ? `<span class="chip">${dueLabel(t.dueDate)}</span>` : ""}
                ${t.assigneeName ? `<span>👤 ${esc(t.assigneeName)}</span>` : ""}
              </div>
            </div>
            ${t.priority ? `<span class="badge b-${esc(t.priority)}">${esc(t.priority)}</span>` : ""}
          </div>`
          )
          .join("")
      : `<div class="empty"><div class="big">🌤</div><h3>Nothing due today</h3><p>Enjoy the calm — or start planning your next project.</p></div>`;

    const dl = document.getElementById("deadlineList");
    dl.innerHTML = d.upcomingDeadlines.length
      ? d.upcomingDeadlines
          .map(
            (de) => `
          <div class="task-row" style="cursor:pointer" onclick="location.href='target.html?id=${de.targetId}'">
            <span class="mini-dot" style="width:34px;height:34px;font-size:14px;">📅</span>
            <div class="t-info">
              <div class="t-title">${esc(de.title)}</div>
              <div class="t-meta"><span>${fmtDate(de.deadline)}</span></div>
            </div>
            <span class="chip ${de.remainingDays <= 3 ? "b-HIGH" : ""}">${de.remainingDays === 0 ? "Today" : `${de.remainingDays}d left`}</span>
          </div>`
          )
          .join("")
      : `<div class="empty"><div class="big">🎈</div><h3>No deadlines in the next two weeks</h3><p>Nice — plenty of runway.</p></div>`;

    renderTargets();
  } catch (err) {
    toast(err.message, "error");
  }
}

function renderTargets() {
  const q = (document.getElementById("searchInput")?.value || "").trim().toLowerCase();
  const status = document.getElementById("statusFilter")?.value || "ALL";
  const sort = document.getElementById("sortSelect")?.value || "updated";

  let list = allTargets
    .filter((t) => status === "ALL" || t.status === status)
    .filter((t) => !q || `${t.title} ${t.courseModule || ""} ${t.ownerName || ""}`.toLowerCase().includes(q));

  if (sort === "deadline") {
    list = list.sort((a, b) => (a.deadline || "9999-99-99").localeCompare(b.deadline || "9999-99-99"));
  } else if (sort === "priority") {
    const rank = { HIGH: 0, MEDIUM: 1, LOW: 2 };
    list = list.sort((a, b) => (rank[a.priority] ?? 1) - (rank[b.priority] ?? 1));
  } else if (sort === "progress") {
    list = list.sort((a, b) => b.stats.progress - a.stats.progress);
  } else {
    list = [...list].sort((a, b) => ((a.updatedAt || "") > (b.updatedAt || "") ? -1 : 1));
  }

  const tl = document.getElementById("targetList");
  tl.innerHTML = list.length
    ? list.map((t) => targetCard(t)).join("")
    : `<div class="card empty" style="border-style:dashed;">
         <div class="big">🔍</div>
         <h3>No targets match</h3>
         <p>${allTargets.length ? "Try a different filter or search." : "Turn your next big assignment into a plan."}</p>
         ${allTargets.length ? "" : `<a class="btn btn-brand" href="create.html">Add New Target</a>`}
       </div>`;
}

function targetCard(t) {
  return `
    <div class="card clickable" style="margin-bottom:14px;" onclick="location.href='target.html?id=${t.id}'">
      <div class="hr-flex" style="margin-bottom:10px;">
        <div class="hr-flex" style="gap:10px;justify-content:flex-start;flex-wrap:wrap;">
          <h3 style="margin:0;font-size:17px;">${esc(t.title)}</h3>
          <span class="badge b-${esc(t.source)}">${esc(t.source)}</span>
          ${t.status === "COMPLETED" ? `<span class="badge b-DONE">Done</span>` : ""}
          ${t.status === "ARCHIVED" ? `<span class="badge b-LOW">Archived</span>` : ""}
          ${t.access === "VIEWER" ? `<span class="badge b-LOW" title="Shared by ${esc(t.ownerName || "owner")}">View-only</span>` : ""}
          ${t.access === "EDITOR" ? `<span class="badge b-IN_PROGRESS" title="Shared by ${esc(t.ownerName || "owner")}">Shared by ${esc(t.ownerName || "owner")}</span>` : ""}
        </div>
        <span style="font-weight:800;color:var(--brand);">${t.stats.progress}%</span>
      </div>
      <div class="progress-track"><div class="progress-fill" style="width:${t.stats.progress}%"></div></div>
      <div class="t-meta" style="margin-top:10px;">
        ${t.courseModule ? `<span>${esc(t.courseModule)}</span>` : ""}
        ${t.deadline ? `<span>📅 ${fmtDate(t.deadline)}</span>` : ""}
        <span>${t.stats.taskDone}/${t.stats.taskTotal} tasks</span>
        <span>${t.stats.milestoneDone}/${t.stats.milestoneTotal} milestones</span>
      </div>
    </div>`;
}

load();
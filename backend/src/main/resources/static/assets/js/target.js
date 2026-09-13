if (!requireAuth()) throw new Error("no auth");
mountNav("dashboard");

const params = new URLSearchParams(location.search);
const targetId = params.get("id");
let T = null;
const me = store.getRawUser() || {};
const isOwner = () => T && (T.access === "OWNER" || !T.access);
const isViewer = () => T && T.access === "VIEWER";
const editable = () => !isViewer();

const $ = (id) => document.getElementById(id);

async function load() {
  try {
    T = await API.getTarget(targetId);
    render();
    renderPrint();
  } catch (err) {
    toast(err.message, "error");
  }
}

function memberNameOptions() {
  return (T.members || []).map((m) => esc(m.name)).join("");
}

function render() {
  $("tTitle").value = T.title || "";
  $("tCourse").value = T.courseModule || "";
  $("tStart").value = T.startDate || "";
  $("tDeadline").value = T.deadline || "";
  $("tPriority").value = T.priority || "MEDIUM";
  $("tDesc").value = T.description || "";
  $("tSourceBadge").textContent = T.source;
  $("tSourceBadge").className = "badge b-" + T.source;
  $("tStatusBadge").textContent = T.status === "COMPLETED" ? "✓ Completed" : "Active";
  $("tStatusBadge").className = "badge " + (T.status === "COMPLETED" ? "b-DONE" : "b-IN_PROGRESS");

  const ring = $("tRing");
  ring.style.setProperty("--val", String(T.stats.progress));
  ring.querySelector(".ring-txt").textContent = T.stats.progress + "%";

  const notes = $("tAiNotes");
  if (T.aiNotes) {
    notes.style.display = "block";
    notes.textContent = "💡 AI note: " + T.aiNotes;
  } else notes.style.display = "none";

  $("viewerBanner").style.display = isViewer() ? "block" : "none";

  if (!document.getElementById("targetMemberDl")) {
    const dl = document.createElement("datalist");
    dl.id = "targetMemberDl";
    document.body.appendChild(dl);
  }
  document.getElementById("targetMemberDl").innerHTML = memberNameOptions();

  $("memberList").innerHTML = (T.members || []).length
    ? T.members
        .map(
          (m) =>
            `<span class="member-tag">👤 ${esc(m.name)} ${m.role ? `<span style="color:var(--faint);font-size:11px;">${esc(m.role)}</span>` : ""}${isOwner() ? `<span class="x" onclick="removeMember('${m.id}')">✕</span>` : ""}</span>`
        )
        .join("")
    : `<span style="color:var(--faint);font-size:13px;">No members yet. Add teammates so the AI can suggest assignments.</span>`;
  $("memberAddBox").style.display = isOwner() ? "" : "none";

  renderSharing();
  renderActivity();

  $("milestoneList").innerHTML = (T.milestones || []).length
    ? (T.milestones || []).map((m, mi) => renderMilestone(m, mi)).join("")
    : `<div class="card empty" style="border-style:dashed;">
         <div class="big">🗺</div><h3>No milestones yet</h3>
         <p>Break this target into milestones to turn it into a plan.${editable() ? "" : " The owner or an editor can build the plan."}</p>
         ${editable() ? `<button class="btn btn-ghost" onclick="addMilestone()">+ Add milestone</button>` : ""}
       </div>`;

  renderExecPanel();
  applyReadOnly();
}

function applyReadOnly() {
  const lock = isViewer();
  document.querySelectorAll(".wrap input, .wrap select").forEach((el) => {
    if (el.id === "collabInput" || el.id === "memberInput") return;
    if (lock) {
      el.setAttribute("readonly", "readonly");
      el.disabled = true;
    } else {
      el.removeAttribute("readonly");
      el.disabled = false;
    }
  });
  $("delBtn").style.display = isOwner() ? "" : "none";
  $("leaveBtn").style.display = isOwner() || !T ? "none" : "";
  $("inviteBox").style.display = isOwner() ? "flex" : "none";
  const mb = $("memberAddBox");
  if (mb) mb.style.display = isOwner() && lock === false ? "" : "none";
  const saveBtn = document.querySelector('.btn-brand[onclick="saveAll()"]');
  if (saveBtn) saveBtn.style.display = lock ? "none" : "";
}

function renderMilestone(m, mi) {
  const done = m.tasks.length ? m.tasks.filter((t) => t.status === "DONE").length : 0;
  const pct = m.tasks.length ? Math.round((done / m.tasks.length) * 100) : 0;
  const editableMilestone = editable();
  return `
    <div class="milestone">
      <div class="milestone-head">
        <span class="m-chev">🚩</span>
        <input class="input m-title-input" value="${esc(m.title)}" data-mid="${m.id}" style="border:none;background:transparent;font-weight:800;font-size:16px;" />
        <div style="flex:1;min-width:140px;">
          <div class="progress-track"><div class="progress-fill" style="width:${pct}%"></div></div>
          <div class="m-sub">${fmtDate(m.endDate)} · ${done}/${m.tasks.length} tasks</div>
        </div>
        <div class="m-actions">
          ${editableMilestone ? `<button class="icon-btn" title="Move up" onclick="moveMilestone(${mi}, -1)">↑</button>
            <button class="icon-btn" title="Move down" onclick="moveMilestone(${mi}, 1)">↓</button>
            <button class="icon-btn" title="Delete" onclick="deleteMilestone(${mi})">✕</button>` : ""}
        </div>
      </div>
      <div class="m-body">
        <div class="t-meta" style="gap:8px;margin-bottom:8px;">
          <label class="label" style="display:inline;margin:0;">due <input class="input m-end-input" type="date" data-mid="${m.id}" value="${esc(m.endDate || "")}" style="width:150px;display:inline-block;font-size:12.5px;" /></label>
          <input class="input m-notes-input" data-mid="${m.id}" placeholder="Notes (optional)" value="${esc(m.notes || "")}" style="width:300px;font-size:12.5px;display:inline-block;" />
        </div>
        ${(m.tasks || []).map((t, ti) => renderTask(m, t, ti)).join("")}
        ${editableMilestone ? `<button class="btn btn-ghost btn-sm" onclick="addTask('${m.id}')" style="margin-top:6px;">+ Add task</button>` : ""}
      </div>
    </div>`;
}

function renderTask(m, t, ti) {
  const done = t.status === "DONE";
  const due = t.dueDate ? `<span class="chip">${dueLabel(t.dueDate)}</span>` : "";
  const priOptions = ["LOW", "MEDIUM", "HIGH"].map((p) => `<option ${t.priority === p ? "selected" : ""}>${p}</option>`).join("");
  const subtasks = (t.subtasks || []).map((s) =>
    `<div class="subtask ${s.done ? "done-sub" : ""}">
       <input type="checkbox" ${s.done ? "checked" : ""} onchange="toggleSubtask('${m.id}','${t.id}','${s.id}',this.checked)" ${editable() ? "" : "disabled"} />
       <input class="input s-body-edit" data-tid="${t.id}" data-sid="${s.id}" value="${esc(s.body)}" style="border:none;background:transparent;width:100%;font-size:13px;" />
     </div>`).join("");
  return `
    <div class="task-row ${done ? "done-row" : ""}" data-tid="${t.id}">
      ${editable() ? `<span class="t-check ${done ? "checked" : ""}" onclick="toggleTask('${m.id}','${t.id}')" title="Complete"></span>` : `<span class="t-check ${done ? "checked" : ""}"></span>`}
      <div class="t-info">
        <div class="t-title">
          <input class="input t-title-input" value="${esc(t.title)}" data-tid="${t.id}" style="border:none;background:transparent;padding:0;font-weight:700;" />
          <span class="badge b-${esc(t.status)}">${esc(t.status)}</span> ${due}
        </div>
        <div class="t-meta">
          <input class="input t-due-input" type="date" data-tid="${t.id}" value="${esc(t.dueDate || "")}" style="width:140px;font-size:12.5px;" />
          <select class="select t-pri-input" data-tid="${t.id}" style="width:110px;font-size:12.5px;">${priOptions}</select>
          <input class="input t-assign-input" data-tid="${t.id}" list="targetMemberDl" value="${esc(t.assigneeName || "")}" placeholder="Assignee" style="width:140px;font-size:12.5px;" />
        </div>
        ${subtasks}
        ${editable() ? `<div class="subtask-list">
          <div class="subtask">
            <input class="input s-body-input" data-tid="${t.id}" placeholder="+ subtask (enter)" style="width:calc(100% - 140px);font-size:12.5px;" onkeydown="subtaskEnter(event,'${m.id}','${t.id}')" />
            <button class="btn btn-ghost btn-sm" onclick="addSubtask('${m.id}','${t.id}')">Add</button>
          </div>
        </div>` : ""}
      </div>
      <div class="t-controls">
        ${editable() ? `<button class="icon-btn" title="Move up" onclick="moveTask('${m.id}','${t.id}',-1)">↑</button>
          <button class="icon-btn" title="Move down" onclick="moveTask('${m.id}','${t.id}',1)">↓</button>
          <button class="icon-btn" title="Delete task" onclick="deleteTask('${m.id}','${t.id}')">✕</button>` : ""}
      </div>
    </div>`;
}

function milestoneById(id) {
  return T.milestones.find((m) => m.id === id);
}

function taskById(m, id) {
  return m.tasks.find((t) => t.id === id);
}

// ---------- execution queue ----------
function renderExecPanel() {
  const panel = $("execPanel");
  const today = todayISO();
  const all = (T.milestones || []).flatMap((m) => (m.tasks || []).map((t) => ({ ...t, milestone: m.title })));
  const overdue = all.filter((t) => t.status !== "DONE" && t.dueDate && t.dueDate < today).sort((a, b) => a.dueDate.localeCompare(b.dueDate));
  const upNext = all
    .filter((t) => t.status !== "DONE" && t.dueDate && t.dueDate >= today)
    .sort((a, b) => a.dueDate.localeCompare(b.dueDate))
    .slice(0, 4);
  if (!overdue.length && !upNext.length) {
    panel.style.display = "none";
    return;
  }
  const chipRow = (list) =>
    list
      .map(
        (t) =>
          `<span class="chip ${t.dueDate < today ? "b-HIGH" : "b-IN_PROGRESS"}" data-tid="${t.id}" onclick="prevFocusRow('${t.id}')" title="Locate in plan">${esc(t.title)}</span>`
      )
      .join(" ");
  panel.style.display = "block";
  panel.innerHTML = `
    <div class="hr-flex" style="margin-bottom:8px;align-items:center;">
      <h2 class="section-title" style="margin:0;font-size:17px;">🎯 Execution queue</h2>
      <span class="t-meta">${overdue.length} overdue · ${all.filter((t) => t.status !== "DONE").length} open</span>
    </div>
    ${overdue.length ? `<div class="t-meta" style="gap:6px;margin-bottom:8px;">${chipRow(overdue)}</div>` : ""}
    ${upNext.length ? `<div class="t-meta" style="gap:6px;"><strong>Up next:</strong> ${chipRow(upNext)}</div>` : ""}`;
}

function prevFocusRow(id) {
  const el = document.querySelector(`.task-row[data-tid="${id}"]`);
  if (el) {
    el.scrollIntoView({ behavior: "smooth", block: "center" });
    el.style.outline = "2px solid var(--brand)";
    setTimeout(() => (el.style.outline = ""), 1800);
  }
}

// ---------- input snapshotting (reads DOM into T before structural ops & save) ----------
function snapshot() {
  T.title = $("tTitle").value.trim() || "Untitled target";
  T.courseModule = $("tCourse").value.trim() || null;
  T.startDate = $("tStart").value || null;
  T.deadline = $("tDeadline").value || null;
  T.priority = $("tPriority").value;
  T.description = $("tDesc").value.trim() || null;

  (T.milestones || []).forEach((m) => {
    const titleEl = document.querySelector(`.m-title-input[data-mid="${m.id}"]`);
    if (titleEl) m.title = titleEl.value.trim() || "Milestone";
    const endEl = document.querySelector(`.m-end-input[data-mid="${m.id}"]`);
    if (endEl) m.endDate = endEl.value || null;
    const notesEl = document.querySelector(`.m-notes-input[data-mid="${m.id}"]`);
    if (notesEl) m.notes = notesEl.value.trim();
    (m.tasks || []).forEach((t) => {
      const q = (sel) => document.querySelector(`.${sel}[data-tid="${t.id}"]`)?.value;
      const tt = document.querySelector(`.t-title-input[data-tid="${t.id}"]`)?.value.trim();
      if (tt != null) t.title = tt || "Task";
      const due = q("t-due-input");
      if (due != null) t.dueDate = due || null;
      const pri = q("t-pri-input");
      if (pri != null) t.priority = pri;
      const asg = q("t-assign-input");
      if (asg != null) t.assigneeName = asg.trim() || null;
      (t.subtasks || []).forEach((s) => {
        document.querySelectorAll(`.s-body-edit[data-tid="${t.id}"][data-sid="${s.id}"]`).forEach((el) => {
          s.body = el.value || s.body;
        });
      });
    });
  });
}

// ---------- structural ops ----------
function addMilestone() {
  if (!editable()) return;
  T.milestones = T.milestones || [];
  T.milestones.push({ id: "tmp" + Math.random().toString(36).slice(2, 7), title: "New milestone", endDate: null, notes: "", tasks: [] });
  render();
}

function deleteMilestone(mi) {
  if (!editable()) return;
  snapshot();
  T.milestones.splice(mi, 1);
  render();
}

function moveMilestone(mi, dir) {
  if (!editable()) return;
  snapshot();
  const j = mi + dir;
  if (j < 0 || j >= T.milestones.length) return;
  [T.milestones[mi], T.milestones[j]] = [T.milestones[j], T.milestones[mi]];
  render();
}

function addTask(mid) {
  if (!editable()) return;
  snapshot();
  const m = milestoneById(mid);
  m.tasks.push({ id: "tmp" + Math.random().toString(36).slice(2, 7), title: "New task", description: "", dueDate: null, priority: "MEDIUM", status: "TODO", assigneeName: null, dependsOn: [], subtasks: [] });
  render();
}

function deleteTask(mid, tid) {
  if (!editable()) return;
  snapshot();
  const m = milestoneById(mid);
  m.tasks = m.tasks.filter((t) => t.id !== tid);
  render();
}

function moveTask(mid, tid, dir) {
  if (!editable()) return;
  snapshot();
  const m = milestoneById(mid);
  const i = m.tasks.findIndex((t) => t.id === tid);
  const j = i + dir;
  if (j < 0 || j >= m.tasks.length) return;
  [m.tasks[i], m.tasks[j]] = [m.tasks[j], m.tasks[i]];
  render();
}

function toggleTask(mid, tid) {
  if (!editable()) return;
  snapshot();
  const t = taskById(milestoneById(mid), tid);
  t.status = t.status === "DONE" ? "TODO" : "DONE";
  render();
}

function addSubtask(mid, tid) {
  if (!editable()) return;
  snapshot();
  const input = document.querySelector(`.s-body-input[data-tid="${tid}"]`) || document.querySelector(".s-body-input");
  const body = input?.value?.trim();
  if (!body) return;
  const t = taskById(milestoneById(mid), tid);
  t.subtasks.push({ id: "tmp" + Math.random().toString(36).slice(2, 7), body, done: false });
  input.value = "";
  render();
}

function subtaskEnter(e, mid, tid) {
  if (e.key === "Enter") {
    e.preventDefault();
    addSubtask(mid, tid);
  }
}

function toggleSubtask(mid, tid, sid, checked) {
  if (!editable()) return;
  snapshot();
  const s = taskById(milestoneById(mid), tid).subtasks.find((x) => x.id === sid);
  if (s) s.done = checked;
  render();
}

// ---------- members ----------
function addMember() {
  if (!isOwner()) return;
  const name = $("memberInput").value.trim();
  if (!name) return;
  API.addMember(targetId, name)
    .then((view) => {
      T = view;
      $("memberInput").value = "";
      render();
      toast(`Added ${name}`);
    })
    .catch((err) => toast(err.message, "error"));
}

function removeMember(memberId) {
  if (!isOwner()) return;
  API.removeMember(targetId, memberId)
    .then((view) => {
      T = view;
      render();
    })
    .catch((err) => toast(err.message, "error"));
}

// ---------- sharing ----------
function renderSharing() {
  const owner = isOwner();
  const badge = $("accessBadge");
  badge.innerHTML = owner
    ? `<span class="badge b-DONE">You own this</span>`
    : `<span class="badge b-${isViewer() ? "LOW" : "IN_PROGRESS"}">${isViewer() ? "View-only access" : "Editing shared target"}</span>`;

  const hint = $("accessHint");
  hint.innerHTML = owner
    ? `<span class="t-meta" style="color:var(--faint);">People you invite can see the plan on their dashboard. Editors can change it; viewers can only watch.</span>`
    : `<span class="t-meta" style="color:var(--faint);">Shared with you by <strong>${esc(T.ownerName || "the owner")}</strong>. You can ${isViewer() ? "view" : "edit"} the plan — the owner can remove you.</span>`;

  const linked = (T.members || []).filter((m) => m.userId);
  const chips = [(T.members || []).find((m) => m.userId === T.ownerId)]
    .filter(Boolean)
    .map((m) => `<span class="member-tag" title="Owner">👑 ${esc(m.name)}</span>`);
  linked
    .filter((m) => m.userId !== T.ownerId)
    .forEach((m) => {
      const isYou = m.userId === me.id;
      const label = isYou ? "you" : m.name;
      const role = m.role && m.role !== "member" ? m.role : "editor";
      const roleSelect = owner
        ? `<select class="select" style="width:88px;font-size:11px;padding:2px 4px;" onchange="setCollabRole('${m.userId}', this.value)" title="Change access">
             <option value="editor" ${role === "editor" ? "selected" : ""}>Editor</option>
             <option value="viewer" ${role === "viewer" ? "selected" : ""}>Viewer</option>
           </select>`
        : `<span style="color:var(--faint);font-size:11px;text-transform:capitalize;">${role}</span>`;
      const remove = owner
        ? `<span class="x" onclick="removeCollaborator('${m.userId}')">✕</span>`
        : "";
      chips.push(`<span class="member-tag">🔗 ${esc(label)} <span style="color:var(--faint);font-size:11px;">${esc(m.email || "")}</span>${roleSelect}${remove}</span>`);
    });
  $("collabList").innerHTML = chips.length
    ? chips.join("")
    : `<span style="color:var(--faint);font-size:13px;">Not shared with anyone yet. Invite a teammate to work together.</span>`;
}

function renderActivity() {
  const list = $("activityList");
  const acts = (T.activities || []).slice().reverse();
  if (!acts.length) {
    list.innerHTML = `<span style="color:var(--faint);font-size:13px;">Nothing here yet.</span>`;
    return;
  }
  list.innerHTML = acts
    .map(
      (a) => `<div class="t-meta" style="gap:8px;padding:6px 0;border-bottom:1px solid rgba(0,0,0,.04);">
        <span>👤</span><span><strong>${esc(a.actorName || "Someone")}</strong> ${esc(a.message || "")}</span>
        <span style="color:var(--faint);font-size:12px;">${fmtWhen(a.at)}</span>
      </div>`
    )
    .join("");
}

function fmtWhen(iso) {
  if (!iso) return "";
  const d = new Date(iso);
  const diff = Date.now() - d.getTime();
  const mins = Math.round(diff / 60000);
  if (mins < 1) return "just now";
  if (mins < 60) return `${mins}m ago`;
  const hours = Math.round(mins / 60);
  if (hours < 24) return `${hours}h ago`;
  return d.toLocaleDateString(undefined, { month: "short", day: "numeric" });
}

function setCollabRole(userId, role) {
  if (!isOwner()) return;
  API.setCollaboratorRole(targetId, userId, role)
    .then((view) => {
      T = view;
      render();
      toast(role === "viewer" ? "Now view-only" : "Now an editor");
    })
    .catch((err) => toast(err.message, "error"));
}

function inviteCollaborator() {
  if (!isOwner()) return;
  const email = $("collabInput").value.trim();
  if (!email) return;
  API.addCollaborator(targetId, email)
    .then((view) => {
      T = view;
      $("collabInput").value = "";
      refreshNotificationBadge();
      render();
      toast(`Invited ${email}`);
    })
    .catch((err) => toast(err.message, "error"));
}

function removeCollaborator(userId) {
  if (!isOwner()) return;
  API.removeCollaborator(targetId, userId)
    .then((view) => {
      T = view;
      render();
      toast("Collaborator removed");
    })
    .catch((err) => toast(err.message, "error"));
}

function leaveTarget() {
  if (!confirm("Leave this shared target? It will be removed from your dashboard.")) return;
  API.removeCollaborator(targetId, me.id)
    .then(() => {
      toast("You left the target");
      setTimeout(() => (location.href = "dashboard.html"), 400);
    })
    .catch((err) => toast(err.message, "error"));
}

// ---------- save / delete ----------
function buildRequest() {
  snapshot();
  return {
    title: T.title,
    description: T.description,
    courseModule: T.courseModule,
    startDate: T.startDate,
    deadline: T.deadline,
    priority: T.priority,
    source: T.source,
    aiNotes: T.aiNotes,
    members: (T.members || []).map((m) => ({ name: m.name, email: m.email || null, role: m.role || null, userId: m.userId || null })),
    milestones: (T.milestones || []).map((m) => ({
      title: m.title,
      endDate: m.endDate,
      notes: m.notes,
      tasks: (m.tasks || []).map((t) => ({
        title: t.title,
        description: t.description || "",
        dueDate: t.dueDate,
        priority: t.priority,
        status: t.status,
        assigneeName: t.assigneeName,
        dependsOn: t.dependsOn || [],
        subtasks: (t.subtasks || []).map((s) => ({ body: s.body, done: s.done })),
      })),
    })),
  };
}

function saveAll() {
  if (!editable()) return;
  const spin = $("saveSpin");
  const btn = spin.closest("button");
  btn.disabled = true;
  spin.style.display = "inline-block";
  API.updateTarget(targetId, buildRequest())
    .then((res) => {
      T = res.target;
      render();
      renderPrint();
      refreshNotificationBadge();
      notifyNewly(res.newlyUnlocked);
      toast("Saved ✓");
    })
    .catch((err) => toast(err.message, "error"))
    .finally(() => {
      btn.disabled = false;
      spin.style.display = "none";
    });
}

function delTarget() {
  if (!isOwner()) return;
  if (!confirm("Delete this target and all its milestones and tasks?")) return;
  API.deleteTarget(targetId)
    .then(() => {
      toast("Target deleted");
      setTimeout(() => (location.href = "dashboard.html"), 400);
    })
    .catch((err) => toast(err.message, "error"));
}

// ---------- AI re-plan ----------
function replanWithAI() {
  if (!editable()) return;
  const btn = $("replanBtn");
  btn.disabled = true;
  btn.textContent = "Planning…";
  API.replanTarget(targetId)
    .then((res) => {
      T = res.target;
      render();
      renderPrint();
      notifyNewly(res.newlyUnlocked);
      toast("Plan rebuilt with AI — review the new schedule");
    })
    .catch((err) => toast(err.message, "error"))
    .finally(() => {
      btn.disabled = false;
      btn.textContent = "✨ Re-plan with AI";
    });
}

// ---------- export ----------
function exportPlan(kind) {
  const file = kind === "csv" ? `plan-${targetId}.csv` : `plan-${targetId}.ics`;
  downloadExport(`/api/targets/${targetId}/export/${kind}`, file)
    .then(() => toast(kind === "csv" ? "CSV downloaded" : "Calendar downloaded"))
    .catch((err) => toast(err.message, "error"));
}

function printPlan() {
  if (!T) return;
  renderPrint();
  window.print();
}

function renderPrint() {
  if (!T) return;
  const today = todayISO();
  const overdue = (T.milestones || []).flatMap((m) => (m.tasks || []).map((t) => ({ ...t, milestone: m.title })))
    .filter((t) => t.status !== "DONE" && t.dueDate && t.dueDate < today).length;
  const html = `
    <div class="print-header">
      <div class="print-brand">✦ StudyForge</div>
      <h1>${esc(T.title)}</h1>
      <p class="print-muted">${esc(T.courseModule || "")} · ${T.status} · ${T.stats.progress}% complete · generated ${new Date().toLocaleString()}</p>
      ${T.description ? `<p>${esc(T.description)}</p>` : ""}
      <p class="print-muted">Start ${fmtDateLong(T.startDate)} · Due ${fmtDateLong(T.deadline)} · ${overdue} overdue · ${T.stats.taskDone}/${T.stats.taskTotal} tasks done</p>
    </div>
    ${(T.milestones || []).map((m) => {
      const done = m.tasks.filter((t) => t.status === "DONE").length;
      return `
      <div class="print-milestone">
        <h2>🚩 ${esc(m.title)} <span class="print-muted">— due ${fmtDateLong(m.endDate)} · ${done}/${m.tasks.length} done</span></h2>
        ${m.notes ? `<p class="print-muted">${esc(m.notes)}</p>` : ""}
        ${m.tasks.length ? `<table class="print-table">
          <thead><tr><th>Task</th><th>Assignment</th><th>Due</th><th>Priority</th><th>Status</th></tr></thead>
          <tbody>${m.tasks.map((t) => `
            <tr class="${t.status === "DONE" ? "print-done" : ""}">
              <td>${esc(t.title)}${(t.subtasks || []).length ? `<div class="print-sub">${t.subtasks.map((s) => `<div>${s.done ? "✔" : "·"} ${esc(s.body)}</div>`).join("")}</div>` : ""}</td>
              <td>${esc(t.assigneeName || "")}</td>
              <td>${fmtDateLong(t.dueDate)}</td>
              <td>${esc(t.priority || "")}</td>
              <td>${esc(t.status || "")}</td>
            </tr>`).join("")}</tbody>
        </table>` : `<p class="print-muted">No tasks.</p>`}
      </div>`;
    }).join("")}
    ${T.aiNotes ? `<div class="print-note">💡 ${esc(T.aiNotes)}</div>` : ""}
  `;
  $("printArea").innerHTML = html;
}

load();
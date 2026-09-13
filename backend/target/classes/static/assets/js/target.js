if (!requireAuth()) throw new Error("no auth");
mountNav("dashboard");

const params = new URLSearchParams(location.search);
const targetId = params.get("id");
let T = null;
const me = store.getRawUser() || {};
const isOwner = () => T && (T.access === "OWNER" || !T.access);

const $ = (id) => document.getElementById(id);

async function load() {
  try {
    T = await API.getTarget(targetId);
    render();
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

  if (!document.getElementById("targetMemberDl")) {
    const dl = document.createElement("datalist");
    dl.id = "targetMemberDl";
    document.body.appendChild(dl);
  }
  document.getElementById("targetMemberDl").innerHTML = memberNameOptions();

  // members
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

  // milestones
  $("milestoneList").innerHTML = (T.milestones || []).map((m, mi) => renderMilestone(m, mi)).join("") ||
    `<div class="card empty" style="border-style:dashed;">
       <div class="big">🗺</div><h3>No milestones yet</h3>
       <p>Break this target into milestones to turn it into a plan.</p>
       <button class="btn btn-ghost" onclick="addMilestone()">+ Add milestone</button>
     </div>`;
}

function renderMilestone(m, mi) {
  const done = m.tasks.length ? m.tasks.filter((t) => t.status === "DONE").length : 0;
  const pct = m.tasks.length ? Math.round((done / m.tasks.length) * 100) : 0;
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
          <button class="icon-btn" title="Move up" onclick="moveMilestone(${mi}, -1)">↑</button>
          <button class="icon-btn" title="Move down" onclick="moveMilestone(${mi}, 1)">↓</button>
          <button class="icon-btn" title="Delete" onclick="deleteMilestone(${mi})">✕</button>
        </div>
      </div>
      <div class="m-body">
        <div class="t-meta" style="gap:8px;margin-bottom:8px;">
          <label class="label" style="display:inline;margin:0;">due <input class="input m-end-input" type="date" data-mid="${m.id}" value="${esc(m.endDate || "")}" style="width:150px;display:inline-block;font-size:12.5px;" /></label>
          <input class="input m-notes-input" data-mid="${m.id}" placeholder="Notes (optional)" value="${esc(m.notes || "")}" style="width:300px;font-size:12.5px;display:inline-block;" />
        </div>
        ${(m.tasks || []).map((t, ti) => renderTask(m, t, ti)).join("")}
        <button class="btn btn-ghost btn-sm" onclick="addTask('${m.id}')" style="margin-top:6px;">+ Add task</button>
      </div>
    </div>`;
}

function renderTask(m, t, ti) {
  const done = t.status === "DONE";
  const due = t.dueDate ? `<span class="chip">${dueLabel(t.dueDate)}</span>` : "";
  const priOptions = ["LOW", "MEDIUM", "HIGH"].map((p) => `<option ${t.priority === p ? "selected" : ""}>${p}</option>`).join("");
  const subtasks = (t.subtasks || []).map((s) =>
    `<div class="subtask ${s.done ? "done-sub" : ""}">
       <input type="checkbox" ${s.done ? "checked" : ""} onchange="toggleSubtask('${m.id}','${t.id}','${s.id}',this.checked)" />
       <input class="input s-body-edit" data-tid="${t.id}" data-sid="${s.id}" value="${esc(s.body)}" style="border:none;background:transparent;width:100%;font-size:13px;" />
     </div>`).join("");
  return `
    <div class="task-row ${done ? "done-row" : ""}" data-tid="${t.id}">
      <span class="t-check ${done ? "checked" : ""}" onclick="toggleTask('${m.id}','${t.id}')" title="Complete"></span>
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
        <div class="subtask-list">
          <div class="subtask">
            <input class="input s-body-input" data-tid="${t.id}" placeholder="+ subtask (enter)" style="width:calc(100% - 140px);font-size:12.5px;" onkeydown="subtaskEnter(event,'${m.id}','${t.id}')" />
            <button class="btn btn-ghost btn-sm" onclick="addSubtask('${m.id}','${t.id}')">Add</button>
          </div>
        </div>
      </div>
      <div class="t-controls">
        <button class="icon-btn" title="Move up" onclick="moveTask('${m.id}','${t.id}',-1)">↑</button>
        <button class="icon-btn" title="Move down" onclick="moveTask('${m.id}','${t.id}',1)">↓</button>
        <button class="icon-btn" title="Delete task" onclick="deleteTask('${m.id}','${t.id}')">✕</button>
      </div>
    </div>`;
}

function milestoneById(id) {
  return T.milestones.find((m) => m.id === id);
}

function taskById(m, id) {
  return m.tasks.find((t) => t.id === id);
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
      const tt = (document.querySelector(`.t-title-input[data-tid="${t.id}"]`))?.value.trim();
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
  T.milestones = T.milestones || [];
  T.milestones.push({ id: "tmp" + Math.random().toString(36).slice(2, 7), title: "New milestone", endDate: null, notes: "", tasks: [] });
  render();
}

function deleteMilestone(mi) {
  snapshot();
  T.milestones.splice(mi, 1);
  render();
}

function moveMilestone(mi, dir) {
  snapshot();
  const j = mi + dir;
  if (j < 0 || j >= T.milestones.length) return;
  [T.milestones[mi], T.milestones[j]] = [T.milestones[j], T.milestones[mi]];
  render();
}

function addTask(mid) {
  snapshot();
  const m = milestoneById(mid);
  m.tasks.push({ id: "tmp" + Math.random().toString(36).slice(2, 7), title: "New task", description: "", dueDate: null, priority: "MEDIUM", status: "TODO", assigneeName: null, dependsOn: [], subtasks: [] });
  render();
}

function deleteTask(mid, tid) {
  snapshot();
  const m = milestoneById(mid);
  m.tasks = m.tasks.filter((t) => t.id !== tid);
  render();
}

function moveTask(mid, tid, dir) {
  snapshot();
  const m = milestoneById(mid);
  const i = m.tasks.findIndex((t) => t.id === tid);
  const j = i + dir;
  if (j < 0 || j >= m.tasks.length) return;
  [m.tasks[i], m.tasks[j]] = [m.tasks[j], m.tasks[i]];
  render();
}

function toggleTask(mid, tid) {
  snapshot();
  const t = taskById(milestoneById(mid), tid);
  t.status = t.status === "DONE" ? "TODO" : "DONE";
  render();
}

function addSubtask(mid, tid) {
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
  snapshot();
  const s = taskById(milestoneById(mid), tid).subtasks.find((x) => x.id === sid);
  if (s) s.done = checked;
  render();
}

// ---------- members ----------
function addMember() {
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
  $("delBtn").style.display = owner ? "" : "none";
  $("leaveBtn").style.display = owner ? "none" : "";
  $("inviteBox").style.display = owner ? "" : "none";

  const badge = $("accessBadge");
  badge.innerHTML = owner
    ? `<span class="badge b-DONE">You own this</span>`
    : `<span class="badge b-IN_PROGRESS">Editing shared target</span>`;

  const hint = $("accessHint");
  hint.innerHTML = owner
    ? `<span class="t-meta" style="color:var(--faint);">People you invite can edit milestones, mark tasks done, and see it on their dashboard.</span>`
    : `<span class="t-meta" style="color:var(--faint);">Shared with you by <strong>${esc(T.ownerName || "the owner")}</strong>. You can edit the plan — the owner can remove you.</span>`;

  const linked = (T.members || []).filter((m) => m.userId);
  const chips = [(T.members || []).find((m) => m.userId === T.ownerId)]
    .filter(Boolean)
    .map((m) => `<span class="member-tag" title="Owner">👑 ${esc(m.name)}</span>`);
  linked
    .filter((m) => m.userId !== T.ownerId)
    .forEach((m) => {
      const isYou = m.userId === me.id;
      const label = isYou ? "you" : m.name;
      const remove = owner
        ? `<span class="x" onclick="removeCollaborator('${m.userId}')">✕</span>`
        : "";
      chips.push(`<span class="member-tag">🔗 ${esc(label)} <span style="color:var(--faint);font-size:11px;">${esc(m.email || "")}</span>${remove}</span>`);
    });
  $("collabList").innerHTML = chips.length
    ? chips.join("")
    : `<span style="color:var(--faint);font-size:13px;">Not shared with anyone yet. Invite a teammate to work together.</span>`;
}

function inviteCollaborator() {
  const email = $("collabInput").value.trim();
  if (!email) return;
  API.addCollaborator(targetId, email)
    .then((view) => {
      T = view;
      $("collabInput").value = "";
      render();
      toast(`Invited ${email}`);
    })
    .catch((err) => toast(err.message, "error"));
}

function removeCollaborator(userId) {
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
  const spin = $("saveSpin");
  const btn = spin.closest("button");
  btn.disabled = true;
  spin.style.display = "inline-block";
  API.updateTarget(targetId, buildRequest())
    .then((res) => {
      T = res.target;
      render();
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
  if (!confirm("Delete this target and all its milestones and tasks?")) return;
  API.deleteTarget(targetId)
    .then(() => {
      toast("Target deleted");
      setTimeout(() => (location.href = "dashboard.html"), 400);
    })
    .catch((err) => toast(err.message, "error"));
}

load();
if (!requireAuth()) throw new Error("no auth");
mountNav("create");

let mode = null;
let analyzeResult = null;
let answers = {};
let proposal = null;
let usedPdf = null;
let proposalMembers = [];

const $ = (id) => document.getElementById(id);

function chooseMode(m) {
  mode = m;
  $("modeSelect").style.display = "none";
  $("manualSection").style.display = m === "manual" ? "block" : "none";
  $("aiSection").style.display = m === "ai" ? "block" : "none";
  if (m === "manual" && !$("manualMilestones").innerHTML) addMileBlock();
  if (m === "ai") goStep("1");
}

function backToModes() {
  $("aiSection").style.display = "none";
  $("modeSelect").style.display = "grid";
  resetAi();
}

function goStep(n) {
  ["1", "2", "3"].forEach((s) => ($("aiStep" + s).style.display = s === n ? "block" : "none"));
}

function resetAi() {
  analyzeResult = null;
  answers = {};
  proposal = null;
  $("aiGoal").value = "";
  $("aiPdf").value = "";
  $("pdfChip").style.display = "none";
  usedPdf = null;
}

function setExample(n) {
  const examples = {
    1: "Complete my IT3070 group project before October 20. We're a team of 4 building a web application. We need to research requirements, design a prototype, build the app, and write a final report.",
    2: "Finish chapter 3 of my thesis (literature review) by the end of next month. Around 6000 words, needs sources, a structure, and a draft for my supervisor to review.",
    3: "Prepare a 15-minute final presentation on machine learning for our software engineering class. Team of 3 people, needs slides, a demo, and practice before the presentation day.",
    4: "Complete the C++ programming assignment for CS2101. It's a linked list implementation with tests and a short report. Due in two weeks, done individually.",
  };
  $("aiGoal").value = examples[n] || "";
}

$("aiPdf").addEventListener("change", (e) => {
  const file = e.target.files[0];
  if (!file) return;
  if (!file.type.toLowerCase().includes("pdf") && !file.name.toLowerCase().endsWith(".pdf")) {
    toast("Only PDF files are supported", "error");
    e.target.value = "";
    return;
  }
  if (file.size > 8 * 1024 * 1024) {
    toast("File is larger than 8 MB", "error");
    e.target.value = "";
    return;
  }
  const reader = new FileReader();
  reader.onload = () => {
    const dataUrl = reader.result;
    usedPdf = { pdfName: file.name, pdfData: dataUrl.split(",")[1] };
    const chip = $("pdfChip");
    chip.textContent = "📄 " + file.name;
    chip.style.display = "inline-flex";
    toast("PDF attached — I'll read it");
  };
  reader.readAsDataURL(file);
});

// ----------------------------------------------------------------
// MANUAL MODE
// ----------------------------------------------------------------
function addMileBlock() {
  const wrap = $("manualMilestones");
  const div = document.createElement("div");
  div.className = "card";
  div.style.marginTop = "16px";
  div.innerHTML = `
    <div class="hr-flex" style="margin-bottom:6px;">
      <strong>🚩 Milestone</strong>
      <button class="icon-btn" onclick="rmMileBlock(this)">✕</button>
    </div>
    <div class="grid grid-2">
      <div class="form-row"><label class="label">Milestone title</label><input class="input ms-title" placeholder="e.g. Research & planning" /></div>
      <div class="form-row"><label class="label">Due date</label><input class="input" type="date" class="ms-date" /></div>
    </div>
    <div class="ms-tasks"></div>
    <button class="btn btn-ghost btn-sm" onclick="addTask(this)">+ Add task</button>`;
  wrap.appendChild(div);
}

function rmMileBlock(btn) {
  btn.closest(".card").remove();
}

function addTask(btn) {
  const host = btn.closest(".card").querySelector(".ms-tasks");
  const row = document.createElement("div");
  row.className = "card";
  row.style.borderStyle = "dashed";
  row.style.boxShadow = "none";
  row.style.padding = "14px";
  row.style.marginBottom = "10px";
  row.innerHTML = `
    <div class="hr-flex" style="margin-bottom:8px;">
      <strong style="font-size:13px;">Task</strong>
      <button class="icon-btn" onclick="this.closest('.card').remove()">✕</button>
    </div>
    <div class="grid grid-4">
      <div class="form-row"><label class="label">Title *</label><input class="input te-title" placeholder="e.g. Write intro" /></div>
      <div class="form-row"><label class="label">Due date</label><input class="input te-due" type="date" /></div>
      <div class="form-row"><label class="label">Priority</label><select class="select te-pri"><option>LOW</option><option selected>MEDIUM</option><option>HIGH</option></select></div>
      <div class="form-row"><label class="label">Assignee</label><input class="input te-assignee" placeholder="Optional" /></div>
    </div>
    <input class="input te-subtasks" placeholder="Subtasks, comma separated (optional)" />`;
  host.appendChild(row);
}

function saveManual(btnEl) {
  const title = $("mTitle").value.trim();
  if (!title) return toast("A target title is required", "error");

  const milestones = [];
  $("manualMilestones").querySelectorAll(".card").forEach((card) => {
    const mTitle = card.querySelector(".ms-title").value.trim();
    if (!mTitle) return;
    const ms = { title: mTitle, endDate: card.querySelector(".ms-date").value || null, notes: "", tasks: [] };
    card.querySelectorAll(".ms-tasks .card").forEach((tc) => {
      const tTitle = tc.querySelector(".te-title").value.trim();
      if (!tTitle) return;
      const subtasks = (tc.querySelector(".te-subtasks").value || "")
        .split(",").map((s) => s.trim()).filter(Boolean)
        .map((body) => ({ body, done: false }));
      ms.tasks.push({
        title: tTitle,
        description: "",
        dueDate: tc.querySelector(".te-due").value || null,
        priority: tc.querySelector(".te-pri").value,
        status: "TODO",
        assigneeName: tc.querySelector(".te-assignee").value.trim() || null,
        dependsOn: [],
        subtasks,
      });
    });
    milestones.push(ms);
  });

  const body = {
    title,
    description: $("mDesc").value.trim(),
    courseModule: $("mCourse").value.trim() || null,
    startDate: $("mStart").value || null,
    deadline: $("mDeadline").value || null,
    priority: "MEDIUM",
    source: "MANUAL",
    members: [],
    milestones,
  };

  const btn = btnEl;
  btn.disabled = true;
  btn.textContent = "Creating…";
  API.createTarget(body)
    .then((res) => {
      notifyNewly(res.newlyUnlocked);
      location.href = "target.html?id=" + res.target.id;
    })
    .catch((err) => {
      toast(err.message, "error");
      btn.disabled = false;
      btn.textContent = "Create Target";
    });
}

// ----------------------------------------------------------------
// AI MODE
// ----------------------------------------------------------------
async function analyze() {
  const goal = $("aiGoal").value.trim();
  const btn = $("analyzeBtn");
  if (!goal && !usedPdf) return toast("Write your goal or upload a PDF", "error");
  btn.disabled = true;
  btn.innerHTML = '<span class="spinner"></span> Analyzing…';
  try {
    const res = await API.analyze({ text: goal || null, pdfData: usedPdf ? usedPdf.pdfData : null, pdfName: usedPdf ? usedPdf.pdfName : null });
    analyzeResult = res;
    renderStep2();
    goStep("2");
  } catch (err) {
    toast(err.message, "error");
  } finally {
    btn.disabled = false;
    btn.textContent = "Analyze my goal";
  }
}

function renderStep2() {
  const { extracted, questions } = analyzeResult;
  const chips = [];
  for (const k of ["title", "deadline", "startDate", "courseModule", "members", "priority"]) {
    const v = extracted[k];
    if (v) chips.push(`<span class="chip">${k}: ${esc(Array.isArray(v) ? v.join(", ") : v)}</span>`);
  }
  const dl = extracted.deliverables;
  if (Array.isArray(dl) && dl.length) chips.push(`<span class="chip">deliverables: ${esc(dl.slice(0, 3).join(", "))}</span>`);
  $("knownChips").innerHTML = chips.length ? chips.join(" ") : "•";
  answers = {};

  const list = $("questionList");
  list.innerHTML = questions.length
    ? questions
        .map(
          (q, i) => `
        <div class="card q-card">
          <div class="q-label">${esc(q.question)}</div>
          ${q.type === "date" ? `<input class="input" type="date" id="q_${q.field}" />`
            : q.type === "number" ? `<input class="input" type="number" id="q_${q.field}" min="1" placeholder="e.g. 4" />`
            : q.type === "select" ? `<select class="select" id="q_${q.field}"><option value="">—</option>${(q.options || []).map((o) => `<option>${esc(o)}</option>`).join("")}</select>`
            : `<input class="input" type="text" id="q_${q.field}" placeholder="Type your answer" />`}
        </div>`
        )
        .join("")
    : `<div class="empty"><div class="big">⭐</div><h3>Everything we need is already known</h3><p>Hit "Generate my plan" to continue.</p></div>`;
}

async function generatePlan() {
  const goal = $("aiGoal").value.trim() || (analyzeResult ? "Plan this target based on the uploaded document" : "");
  if (!analyzeResult) return toast("Please analyze your goal first", "error");

  answers = {};
  (analyzeResult.questions || []).forEach((q) => {
    const el = $(`q_${q.field}`);
    if (!el || !el.value) return;
    if (q.type === "number") answers[q.field] = parseInt(el.value, 10);
    else if (q.type === "text" && q.field === "memberNames") answers[q.field] = el.value;
    else answers[q.field] = el.value;
  });

  const btn = $("planBtn");
  btn.disabled = true;
  btn.innerHTML = '<span class="spinner"></span> Building your plan…';
  try {
    proposal = await API.plan({ goal, extracted: analyzeResult.extracted, answers });
    renderStep3();
    goStep("3");
    toast("AI proposal ready — review it, change anything, then accept");
  } catch (err) {
    toast(err.message, "error");
  } finally {
    btn.disabled = false;
    btn.innerHTML = "Generate my plan";
  }
}

function proposalMemberNames() {
  const set = new Set(proposalMembers.map((m) => m.name));
  const add = (name) => name && set.add(String(name).trim());
  (answers.memberNames || "").split(",").forEach(add);
  const mn = analyzeResult.extracted.memberNames;
  if (Array.isArray(mn)) mn.forEach(add);
  proposal.milestones.forEach((ms) =>
    ms.tasks.forEach((t) => t.assigneeName && add(t.assigneeName)));
  return [...set].filter(Boolean);
}

function renderStep3() {
  $("pTitle").value = proposal.title || "";
  $("pCourse").value = proposal.courseModule || "";
  $("pStart").value = proposal.startDate || todayISO();
  $("pDeadline").value = proposal.deadline || "";
  $("pPriority").value = proposal.priority || "MEDIUM";
  $("pDesc").value = proposal.description || "";

  const notesEl = $("pNotes");
  if (proposal.notes) {
    notesEl.style.display = "block";
    notesEl.textContent = "💡 " + proposal.notes;
  } else notesEl.style.display = "none";

  proposalMembers = proposalMemberNames().map((name) => ({ id: "n" + Math.random().toString(36).slice(2, 7), name }));
  proposalMembers = [...new Map(proposalMembers.map((m) => [m.name, m])).values()];
  renderProposalMembers();

  const host = $("pMilestones");
  host.innerHTML = "";
  proposal.milestones.forEach((ms, mi) => {
    const card = document.createElement("div");
    card.className = "card prop-card pos-rel";
    card.style.marginTop = "18px";
    card.style.padding = "22px";
    card.setAttribute("data-mi", mi);
    card.innerHTML = `
      <span class="prop-label">MILESTONE ${mi + 1}</span>
      <div class="grid grid-2" style="margin-top:8px;">
        <div class="form-row"><label class="label">Milestone title</label><input class="input pm-title" value="${esc(ms.title)}" /></div>
        <div class="form-row"><label class="label">End date</label><input class="input pm-date" type="date" value="${esc(ms.endDate)}" /></div>
      </div>
      <input class="input pm-notes" style="margin-bottom:12px;" placeholder="Notes (optional)" value="${esc(ms.notes || "")}" />
      <div class="pm-tasks"></div>
      <button class="btn btn-ghost btn-sm" onclick="addTaskToMilestone(this)">+ Add task</button>`;
    const tasksHost = card.querySelector(".pm-tasks");
    ms.tasks.forEach((t, ti) => tasksHost.appendChild(buildProposalTaskRow(t, ti, mi)));
    host.appendChild(card);
  });
}

function buildProposalTaskRow(t, ti, mi) {
  const row = document.createElement("div");
  row.className = "card";
  row.style.borderStyle = "dashed";
  row.style.boxShadow = "none";
  row.style.padding = "14px";
  row.style.marginBottom = "10px";
  row.setAttribute("data-ti", ti);
  row.innerHTML = `
    <div class="hr-flex" style="margin-bottom:8px;">
      <strong style="font-size:13px;">Task</strong>
      <button class="icon-btn" onclick="this.closest('.card').remove()">✕</button>
    </div>
    <div class="grid grid-4">
      <div class="form-row"><label class="label">Title *</label><input class="input pt-title" value="${esc(t.title)}" /></div>
      <div class="form-row"><label class="label">Due date</label><input class="input pt-due" type="date" value="${esc(t.dueDate)}" /></div>
      <div class="form-row"><label class="label">Priority</label><select class="select pt-pri">${["LOW", "MEDIUM", "HIGH"].map((p) => `<option ${t.priority === p ? "selected" : ""}>${p}</option>`).join("")}</select></div>
      <div class="form-row"><label class="label">Assignee</label><input class="input pt-assignee" list="memberList" value="${esc(t.assigneeName || "")}" placeholder="Optional" /></div>
    </div>
    <input class="input pt-desc" value="${esc(t.description || "")}" placeholder="Short description (optional)" style="margin-bottom:8px;" />
    <input class="input pt-subtasks" value="${esc((t.subtasks || []).join(", "))}" placeholder="Subtasks, comma separated (optional)" />`;
  return row;
}

function addTaskToMilestone(btn) {
  const host = btn.closest(".card").querySelector(".pm-tasks");
  host.appendChild(buildProposalTaskRow({ title: "", dueDate: "", description: "", subtasks: [], priority: "MEDIUM", assigneeName: "" }, host.children.length, btn.closest(".card").dataset.mi));
}

function renderProposalMembers() {
  $("pMembers").innerHTML = proposalMembers
    .map((m) => `<span class="member-tag">👤 ${esc(m.name)} <span class="x" onclick="removeProposalMember('${esc(m.name)}')">✕</span></span>`)
    .join("");
  const dl = document.getElementById("memberList");
  if (!dl) {
    const d = document.createElement("datalist");
    d.id = "memberList";
    document.body.appendChild(d);
  }
  document.getElementById("memberList").innerHTML = proposalMembers.map((m) => `<option value="${esc(m.name)}">`).join("");
}

function addProposalMember() {
  const input = $("pMemberInput");
  const name = input.value.trim();
  if (!name) return;
  if (!proposalMembers.some((m) => m.name.toLowerCase() === name.toLowerCase())) {
    proposalMembers.push({ id: "n" + Math.random().toString(36).slice(2, 7), name });
  }
  input.value = "";
  renderProposalMembers();
}

function removeProposalMember(name) {
  proposalMembers = proposalMembers.filter((m) => m.name !== name);
  renderProposalMembers();
}

function saveAiPlan() {
  const title = $("pTitle").value.trim() || proposal.title || "Untitled Target";
  const startDate = $("pStart").value;
  const deadline = $("pDeadline").value;
  if (deadline && startDate && deadline < startDate) {
    return toast("Deadline cannot be before the start date", "error");
  }

  const milestones = [];
  $("pMilestones").querySelectorAll("[data-mi]").forEach((mc) => {
    const msTitle = mc.querySelector(".pm-title").value.trim();
    if (!msTitle) return;
    const ms = { title: msTitle, endDate: mc.querySelector(".pm-date").value || null, notes: mc.querySelector(".pm-notes").value.trim(), tasks: [] };
    mc.querySelectorAll(".pm-tasks [data-ti]").forEach((tr) => {
      const tTitle = tr.querySelector(".pt-title").value.trim();
      if (!tTitle) return;
      const subtasks = (tr.querySelector(".pt-subtasks").value || "")
        .split(",").map((s) => s.trim()).filter(Boolean)
        .map((body) => ({ body, done: false }));
      ms.tasks.push({
        title: tTitle,
        description: tr.querySelector(".pt-desc").value.trim(),
        dueDate: tr.querySelector(".pt-due").value || null,
        priority: tr.querySelector(".pt-pri").value,
        status: "TODO",
        assigneeName: tr.querySelector(".pt-assignee").value.trim() || null,
        dependsOn: [],
        subtasks,
      });
    });
    milestones.push(ms);
  });

  if (!milestones.length) return toast("Add at least one milestone", "error");

  const body = {
    title,
    description: $("pDesc").value.trim(),
    courseModule: $("pCourse").value.trim() || null,
    startDate: startDate || null,
    deadline: deadline || null,
    priority: $("pPriority").value,
    source: "AI",
    aiNotes: proposal.notes || null,
    members: proposalMembers.map((m) => ({ name: m.name, email: null, role: null })),
    milestones,
  };

  const btn = $("acceptBtn");
  btn.disabled = true;
  btn.innerHTML = '<span class="spinner"></span> Saving…';
  API.createTarget(body)
    .then((res) => {
      notifyNewly(res.newlyUnlocked);
      toast("Plan saved! Welcome to execution. 🎉");
      setTimeout(() => (location.href = "target.html?id=" + res.target.id), 500);
    })
    .catch((err) => {
      toast(err.message, "error");
      btn.disabled = false;
      btn.textContent = "Accept & Save Plan";
    });
}
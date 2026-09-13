if (!requireAuth()) throw new Error("no auth");
mountNav("profile");

async function load() {
  try {
    const me = await API.me();
    const u = me.user || me;
    document.getElementById("profName").value = u.name || "";
    document.getElementById("profEmail").value = u.email || "";
    document.getElementById("profAvatar").textContent = initials(u.name);
    if (u.createdAt) {
      document.getElementById("profCreated").textContent =
        new Date(u.createdAt).toLocaleDateString(undefined, { year: "numeric", month: "long", day: "numeric" });
    }
  } catch (err) {
    toast(err.message, "error");
  }
}

function saveProfile() {
  const name = document.getElementById("profName").value.trim();
  if (!name) return toast("Name can't be empty", "error");
  const spin = document.getElementById("profSpin");
  const btn = document.getElementById("profSaveBtn");
  btn.disabled = true;
  spin.style.display = "inline-block";
  API.updateProfile(name)
    .then(async (res) => {
      const u = res.user || res;
      const raw = store.getRawUser() || {};
      raw.name = u.name || raw.name;
      store.setRawUser(raw);
      const st = document.getElementById("nav-slot");
      if (st) st.innerHTML = renderNav("profile");
      toast("Profile updated");
    })
    .catch((err) => toast(err.message, "error"))
    .finally(() => {
      btn.disabled = false;
      spin.style.display = "none";
    });
}

function savePassword() {
  const current = document.getElementById("pwCurrent").value;
  const pw = document.getElementById("pwNew").value;
  const confirm = document.getElementById("pwConfirm").value;
  if (pw.length < 8) return toast("New password must be at least 8 characters", "error");
  if (pw !== confirm) return toast("Passwords don't match", "error");
  const spin = document.getElementById("pwSpin");
  const btn = document.getElementById("pwSaveBtn");
  btn.disabled = true;
  spin.style.display = "inline-block";
  API.changePassword(current, pw)
    .then(() => {
      toast("Password updated");
      document.getElementById("pwCurrent").value = "";
      document.getElementById("pwNew").value = "";
      document.getElementById("pwConfirm").value = "";
    })
    .catch((err) => toast(err.message, "error"))
    .finally(() => {
      btn.disabled = false;
      spin.style.display = "none";
    });
}

load();
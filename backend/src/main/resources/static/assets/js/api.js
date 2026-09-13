const store = {
  getToken: () => localStorage.getItem("sf_token"),
  getRawUser: () => {
    try {
      return JSON.parse(localStorage.getItem("sf_user") || "null");
    } catch {
      return null;
    }
  },
  setUser: (token, user) => {
    localStorage.setItem("sf_token", token);
    localStorage.setItem("sf_user", JSON.stringify(user));
  },
  setRawUser: (user) => {
    localStorage.setItem("sf_user", JSON.stringify(user));
  },
  clear: () => {
    localStorage.removeItem("sf_token");
    localStorage.removeItem("sf_user");
  },
};

async function api(path, options = {}) {
  const token = store.getToken();
  const headers = { "Content-Type": "application/json", ...(options.headers || {}) };
  if (token) headers.Authorization = `Bearer ${token}`;

  const res = await fetch(path, {
    method: options.method || "GET",
    headers,
    body: options.body ? JSON.stringify(options.body) : undefined,
  });

  if (res.status === 204) return null;

  let data = null;
  const text = await res.text();
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = { error: text };
    }
  }

  if (!res.ok) {
    const message = data?.error || `Request failed with ${res.status}`;
    if (res.status === 401 || res.status === 403) {
      store.clear();
      if (!location.pathname.endsWith("login.html")) {
        location.href = "login.html";
        throw new Error(message);
      }
    }
    throw new Error(message);
  }
  return data;
}

const API = {
  login: (email, password) => api("/api/auth/login", { method: "POST", body: { email, password } }),
  register: (name, email, password) =>
    api("/api/auth/register", { method: "POST", body: { name, email, password } }),
  me: () => api("/api/auth/me"),
  updateProfile: (name) => api("/api/auth/me", { method: "PUT", body: { name } }),
  changePassword: (currentPassword, newPassword) =>
    api("/api/auth/me/password", { method: "PUT", body: { currentPassword, newPassword } }),

  dashboard: () => api("/api/dashboard"),
  achievements: () => api("/api/achievements"),
  notifications: () => api("/api/me/notifications"),
  markNotificationsRead: () => api("/api/me/notifications/read", { method: "POST" }),

  listTargets: () => api("/api/targets"),
  getTarget: (id) => api(`/api/targets/${id}`),
  createTarget: (body) => api("/api/targets", { method: "POST", body }),
  updateTarget: (id, body) => api(`/api/targets/${id}`, { method: "PUT", body }),
  deleteTarget: (id) => api(`/api/targets/${id}`, { method: "DELETE" }),
  replanTarget: (id) => api(`/api/targets/${id}/replan`, { method: "POST" }),
  exportCsv: (id) => api(`/api/targets/${id}/export/csv`),
  exportIcs: (id) => api(`/api/targets/${id}/export/ics`),
  addMember: (id, name, email = "", role = "") =>
    api(`/api/targets/${id}/members`, { method: "POST", body: { name, email, role } }),
  removeMember: (id, memberId) => api(`/api/targets/${id}/members/${memberId}`, { method: "DELETE" }),
  addCollaborator: (id, email) =>
    api(`/api/targets/${id}/collaborators`, { method: "POST", body: { email } }),
  removeCollaborator: (id, userId) =>
    api(`/api/targets/${id}/collaborators/${userId}`, { method: "DELETE" }),
  setCollaboratorRole: (id, userId, role) =>
    api(`/api/targets/${id}/collaborators/${userId}/role`, { method: "PUT", body: { role } }),

  analyze: (body) => api("/api/ai/analyze", { method: "POST", body }),
  plan: (body) => api("/api/ai/plan", { method: "POST", body }),
};

async function downloadFrom(blob, filename) {
  const a = document.createElement("a");
  a.href = URL.createObjectURL(blob);
  a.download = filename;
  a.click();
  setTimeout(() => URL.revokeObjectURL(a.href), 4000);
}

async function downloadExport(url, filename) {
  const token = store.getToken();
  const res = await fetch(url, { headers: token ? { Authorization: `Bearer ${token}` } : {} });
  if (!res.ok) {
    let message = `Export failed with ${res.status}`;
    try {
      message = (await res.json()).error || message;
    } catch {
      /* ignore */
    }
    throw new Error(message);
  }
  await downloadFrom(await res.blob(), filename);
}
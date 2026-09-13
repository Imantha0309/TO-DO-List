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

  dashboard: () => api("/api/dashboard"),
  achievements: () => api("/api/achievements"),

  listTargets: () => api("/api/targets"),
  getTarget: (id) => api(`/api/targets/${id}`),
  createTarget: (body) => api("/api/targets", { method: "POST", body }),
  updateTarget: (id, body) => api(`/api/targets/${id}`, { method: "PUT", body }),
  deleteTarget: (id) => api(`/api/targets/${id}`, { method: "DELETE" }),
  addMember: (id, name, email = "", role = "") =>
    api(`/api/targets/${id}/members`, { method: "POST", body: { name, email, role } }),
  removeMember: (id, memberId) => api(`/api/targets/${id}/members/${memberId}`, { method: "DELETE" }),
  addCollaborator: (id, email) =>
    api(`/api/targets/${id}/collaborators`, { method: "POST", body: { email } }),
  removeCollaborator: (id, userId) =>
    api(`/api/targets/${id}/collaborators/${userId}`, { method: "DELETE" }),

  analyze: (body) => api("/api/ai/analyze", { method: "POST", body }),
  plan: (body) => api("/api/ai/plan", { method: "POST", body }),
};
function setLoading(on) {
  const spin = document.getElementById("btnSpin");
  const label = document.getElementById("btnLabel");
  if (on) {
    spin.style.display = "inline-block";
    label.textContent = "One moment…";
  } else {
    spin.style.display = "none";
    label.textContent = document.title.startsWith("Log") || document.title.includes("Log in") ? "Log in" : "Get Started";
  }
}

const isLogin = location.pathname.endsWith("login.html");
const form = document.getElementById(isLogin ? "loginForm" : "signupForm");
const password = document.getElementById("password");

form.addEventListener("submit", async (e) => {
  e.preventDefault();
  const email = document.getElementById("email").value.trim();
  if (!email || !password.value) {
    toast("Please fill in all fields", "error");
    return;
  }
  if (!isLogin && password.value.length < 6) {
    toast("Password must be at least 6 characters", "error");
    return;
  }
  setLoading(true);
  try {
    const res = isLogin ? await API.login(email, password.value) : await API.register(document.getElementById("name").value.trim(), email, password.value);
    store.setUser(res.token, res.user);
    toast(`Welcome${isLogin ? " back" : ""}, ${res.user.name.split(" ")[0]}!`);
    setTimeout(() => (location.href = "dashboard.html"), 400);
  } catch (err) {
    toast(err.message, "error");
    setLoading(false);
  }
});
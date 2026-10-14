(function () {
  const TOKEN_KEY = "tm_token";
  const LOGIN_KEY = "tm_login";
  const apiBase = (window.APP_CONFIG && window.APP_CONFIG.API_BASE_URL) || "/api";

  const authScreen = document.getElementById("auth-screen");
  const boardScreen = document.getElementById("board-screen");
  const authForm = document.getElementById("auth-form");
  const authError = document.getElementById("auth-error");
  const boardError = document.getElementById("board-error");
  const tabLogin = document.getElementById("tab-login");
  const tabRegister = document.getElementById("tab-register");
  const authSubmit = document.getElementById("auth-submit");
  const currentUser = document.getElementById("current-user");
  const logoutBtn = document.getElementById("logout-btn");
  const createTaskForm = document.getElementById("create-task-form");

  let mode = "login";
  let dragTaskId = null;

  function getToken() {
    return localStorage.getItem(TOKEN_KEY);
  }

  function setSession(token, login) {
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(LOGIN_KEY, login);
  }

  function clearSession() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(LOGIN_KEY);
  }

  function showError(el, message) {
    if (!message) {
      el.hidden = true;
      el.textContent = "";
      return;
    }
    el.hidden = false;
    el.textContent = message;
  }

  async function api(path, options = {}) {
    const headers = Object.assign({ "Content-Type": "application/json" }, options.headers || {});
    const token = getToken();
    if (token) {
      headers.Authorization = "Bearer " + token;
    }

    const response = await fetch(apiBase + path, Object.assign({}, options, { headers }));
    if (response.status === 204) {
      return null;
    }

    const text = await response.text();
    let data = null;
    if (text) {
      try {
        data = JSON.parse(text);
      } catch (e) {
        data = { message: text };
      }
    }

    if (!response.ok) {
      const message = (data && data.message) || "Request failed";
      const error = new Error(message);
      error.status = response.status;
      throw error;
    }
    return data;
  }

  function setAuthMode(nextMode) {
    mode = nextMode;
    tabLogin.classList.toggle("active", mode === "login");
    tabRegister.classList.toggle("active", mode === "register");
    authSubmit.textContent = mode === "login" ? "Войти" : "Зарегистрироваться";
    showError(authError, "");
  }

  function showAuth() {
    authScreen.hidden = false;
    boardScreen.hidden = true;
  }

  function showBoard() {
    authScreen.hidden = true;
    boardScreen.hidden = false;
    currentUser.textContent = localStorage.getItem(LOGIN_KEY) || "";
  }

  function createTaskCard(task) {
    const card = document.createElement("article");
    card.className = "task-card";
    card.draggable = true;
    card.dataset.id = String(task.id);

    const title = document.createElement("h3");
    title.textContent = task.title;

    const description = document.createElement("p");
    description.textContent = task.description || "";

    const actions = document.createElement("div");
    actions.className = "actions";

    const deleteBtn = document.createElement("button");
    deleteBtn.type = "button";
    deleteBtn.className = "delete-btn";
    deleteBtn.textContent = "Удалить";
    deleteBtn.addEventListener("click", async function (event) {
      event.stopPropagation();
      try {
        await api("/tasks/" + task.id, { method: "DELETE" });
        await loadTasks();
      } catch (error) {
        showError(boardError, error.message);
      }
    });

    actions.appendChild(deleteBtn);
    card.appendChild(title);
    card.appendChild(description);
    card.appendChild(actions);

    card.addEventListener("dragstart", function (event) {
      dragTaskId = task.id;
      event.dataTransfer.effectAllowed = "move";
      event.dataTransfer.setData("text/plain", String(task.id));
    });

    card.addEventListener("dragend", function () {
      dragTaskId = null;
    });

    return card;
  }

  function renderTasks(tasks) {
    document.querySelectorAll(".task-list").forEach(function (list) {
      list.innerHTML = "";
    });

    tasks.forEach(function (task) {
      const list = document.querySelector('.task-list[data-dropzone="' + task.status + '"]');
      if (list) {
        list.appendChild(createTaskCard(task));
      }
    });
  }

  async function loadTasks() {
    showError(boardError, "");
    const tasks = await api("/tasks");
    renderTasks(tasks);
  }

  function setupDropzones() {
    document.querySelectorAll(".task-list").forEach(function (list) {
      list.addEventListener("dragover", function (event) {
        event.preventDefault();
        list.classList.add("drag-over");
      });

      list.addEventListener("dragleave", function () {
        list.classList.remove("drag-over");
      });

      list.addEventListener("drop", async function (event) {
        event.preventDefault();
        list.classList.remove("drag-over");
        const status = list.dataset.dropzone;
        const taskId = dragTaskId || event.dataTransfer.getData("text/plain");
        if (!taskId || !status) {
          return;
        }
        try {
          await api("/tasks/" + taskId, {
            method: "PATCH",
            body: JSON.stringify({ status: status })
          });
          await loadTasks();
        } catch (error) {
          showError(boardError, error.message);
        }
      });
    });
  }

  tabLogin.addEventListener("click", function () {
    setAuthMode("login");
  });

  tabRegister.addEventListener("click", function () {
    setAuthMode("register");
  });

  authForm.addEventListener("submit", async function (event) {
    event.preventDefault();
    showError(authError, "");
    const login = document.getElementById("auth-login").value.trim();
    const password = document.getElementById("auth-password").value;
    const path = mode === "login" ? "/auth/login" : "/auth/register";

    try {
      const data = await api(path, {
        method: "POST",
        body: JSON.stringify({ login: login, password: password })
      });
      setSession(data.token, data.login);
      showBoard();
      await loadTasks();
    } catch (error) {
      showError(authError, error.message);
    }
  });

  createTaskForm.addEventListener("submit", async function (event) {
    event.preventDefault();
    showError(boardError, "");
    const title = document.getElementById("task-title").value.trim();
    const description = document.getElementById("task-description").value;
    try {
      await api("/tasks", {
        method: "POST",
        body: JSON.stringify({ title: title, description: description })
      });
      createTaskForm.reset();
      await loadTasks();
    } catch (error) {
      showError(boardError, error.message);
    }
  });

  logoutBtn.addEventListener("click", async function () {
    try {
      await api("/auth/logout", { method: "POST" });
    } catch (e) {
      // ignore logout errors
    }
    clearSession();
    showAuth();
  });

  setupDropzones();
  setAuthMode("login");

  if (getToken()) {
    showBoard();
    loadTasks().catch(function (error) {
      clearSession();
      showAuth();
      showError(authError, error.message);
    });
  } else {
    showAuth();
  }
})();

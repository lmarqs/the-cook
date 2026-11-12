// The landing page's demo: a sample project that the menu's recipes cook, update and discard. Nothing here runs Cook;
// the files, versions and terminal output are samples that show what each command changes and what it leaves alone.
(function () {
  const demo = document.querySelector(".demo");
  if (!demo) return;

  const RECIPES = [
    {
      id: "git#worktrees",
      tone: "tone-blue",
      versions: [
        {
          version: "1.0.0",
          files: [
            { path: ".gitignore", section: "worktrees", lines: ["/.worktrees/*", "!/.worktrees/setup"] },
            { path: ".worktrees/setup", lines: ["#!/bin/sh", "npm install"] }
          ]
        },
        {
          version: "1.1.0",
          news: "setup now copies the .env it kept missing.",
          files: [
            { path: ".gitignore", section: "worktrees", lines: ["/.worktrees/*", "!/.worktrees/setup"] },
            { path: ".worktrees/setup", lines: ["#!/bin/sh", "npm install", "cp ../../.env ."] }
          ]
        }
      ]
    },
    {
      id: "agents#seed",
      tone: "tone-purple",
      versions: [
        {
          version: "2.0.0",
          files: [{ path: "AGENTS.md", lines: ["# Rules", "- Run the tests before a change is done."] }]
        },
        {
          version: "2.1.0",
          news: "forbids secrets, after a leaked token.",
          files: [
            { path: "AGENTS.md", lines: ["# Rules", "- Run the tests before a change is done.", "- Never commit secrets."] }
          ]
        }
      ]
    },
    {
      id: "docs#layout",
      tone: "tone-green",
      versions: [
        {
          version: "1.0.0",
          files: [
            { path: ".agents/skills/docs-layout/SKILL.md", lines: ["# docs-layout", "A decision goes in docs/adrs/."] }
          ]
        },
        {
          version: "1.2.0",
          news: "gives traps a page, after a repeat one.",
          files: [
            {
              path: ".agents/skills/docs-layout/SKILL.md",
              lines: ["# docs-layout", "A decision goes in docs/adrs/.", "A trap goes in docs/features/pitfalls.md."]
            }
          ]
        }
      ]
    }
  ];
  const OWN_FILES = { "README.md": ["# my-project", "Our own notes."], ".gitignore": ["node_modules/", ".env"] };
  const EXPLORER_ORDER = [
    ".agents/skills/docs-layout/SKILL.md", ".worktrees/setup", ".gitignore", "AGENTS.md", "README.md", "cook.lock"
  ];
  const LOCK = "cook.lock";
  const LATEST = 1;
  const still = window.matchMedia("(prefers-reduced-motion: reduce)").matches;

  const part = function (name, scope) { return (scope || demo).querySelector('[data-demo="' + name + '"]'); };
  const iconOf = function (name) { return demo.querySelector('template[data-icon="' + name + '"]').innerHTML; };
  const make = function (tag, className, text) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (text !== undefined) node.textContent = text;
    return node;
  };
  const baseName = function (path) { return path.slice(path.lastIndexOf("/") + 1); };
  const folderOf = function (path) { return path.slice(0, Math.max(path.lastIndexOf("/"), 0)); };
  const fileIcon = function (path) {
    if (path === LOCK) return iconOf("lock");
    if (path === ".gitignore") return iconOf("git");
    return /\.md$/.test(path) ? iconOf("text") : iconOf("script");
  };

  const items = RECIPES.map(function (recipe) {
    const node = demo.querySelector('.demo-item[data-recipe="' + recipe.id + '"]');
    return { recipe: recipe, node: node, what: part("note", node).textContent };
  });
  const out = part("out");

  // Recipe id -> index of the version the project has cooked.
  let cooked = {};
  let openFile = "README.md";
  let view = "file";
  let busy = false;
  let commands = 0;
  let wasComplete = false;
  let freshLines = {};
  let freshFiles = {};

  // The project as a state has it: each file's lines, and the recipe that wrote each one.
  function project(state) {
    const files = {};
    Object.keys(OWN_FILES).forEach(function (path) {
      files[path] = OWN_FILES[path].map(function (text) { return { text: text }; });
    });
    const lock = [];
    RECIPES.forEach(function (recipe) {
      if (state[recipe.id] === undefined) return;
      const release = recipe.versions[state[recipe.id]];
      release.files.forEach(function (file) {
        const lines = files[file.path] = files[file.path] || [];
        const marker = function (edge) {
          return { text: "# cook:" + edge + " " + recipe.id + "/" + file.section, recipe: recipe, marker: true };
        };
        if (file.section) lines.push(marker("begin"));
        file.lines.forEach(function (text) { lines.push({ text: text, recipe: recipe }); });
        if (file.section) lines.push(marker("end"));
      });
      lock.push(
        { text: '- name: "' + recipe.id + '"', recipe: recipe },
        { text: '  source: "' + recipe.id.replace("#", ":" + release.version + "#") + '"', recipe: recipe }
      );
    });
    if (lock.length) files[LOCK] = [{ text: "# Written by the-cook; do not edit by hand." }].concat(lock);
    return files;
  }

  const keyOf = function (line) { return (line.recipe ? line.recipe.id : "") + "|" + line.text; };
  const ownedLines = function (files) {
    return Object.keys(files).reduce(function (count, path) {
      return path === LOCK ? count : count + files[path].filter(function (line) { return line.recipe; }).length;
    }, 0);
  };
  const untouched = project({});

  // A unified diff between two projects. A recipe only appends or removes whole lines, so two cursors are enough.
  function diff(before, after) {
    const lines = [];
    const changed = [];
    EXPLORER_ORDER.forEach(function (path) {
      const was = before[path] || [];
      const now = after[path] || [];
      const inWas = {};
      const inNow = {};
      was.forEach(function (line) { inWas[keyOf(line)] = true; });
      now.forEach(function (line) { inNow[keyOf(line)] = true; });
      const same = was.length === now.length && was.every(function (line) { return inNow[keyOf(line)]; });
      if (same) return;
      changed.push(path);
      lines.push(["demo-meta", "--- " + (was.length ? "a/" + path : "/dev/null")]);
      lines.push(["demo-meta", "+++ " + (now.length ? "b/" + path : "/dev/null")]);
      let i = 0;
      let j = 0;
      while (i < was.length || j < now.length) {
        if (i < was.length && !inNow[keyOf(was[i])]) lines.push(["demo-del", "-" + was[i++].text]);
        else if (j < now.length && !inWas[keyOf(now[j])]) lines.push(["demo-add", "+" + now[j++].text]);
        else { lines.push(["", " " + now[j].text]); i++; j++; }
      }
    });
    return { lines: lines, changed: changed };
  }

  function print(className, text) {
    out.append(make("span", className, text + "\n"));
    out.scrollTop = out.scrollHeight;
  }

  function type(command, done) {
    const line = make("span");
    const typed = make("span");
    line.append(make("span", "demo-prompt", "$ "), typed, "\n");
    out.append(line);
    if (still) {
      typed.textContent = command;
      out.scrollTop = out.scrollHeight;
      done();
      return;
    }
    let shown = 0;
    const timer = setInterval(function () {
      typed.textContent = command.slice(0, ++shown);
      out.scrollTop = out.scrollHeight;
      if (shown < command.length) return;
      clearInterval(timer);
      setTimeout(done, 300);
    }, 28);
  }

  function run(action, item) {
    if (busy) return;
    const recipe = item.recipe;
    const next = Object.assign({}, cooked);
    if (action === "discard") delete next[recipe.id];
    else next[recipe.id] = action === "cook" ? 0 : LATEST;
    const before = project(cooked);
    const after = project(next);
    const change = diff(before, after);
    const command = { cook: "cook ", update: "cook update ", discard: "cook discard " }[action] + recipe.id;
    busy = true;
    freshLines = {};
    freshFiles = {};
    render();
    type(command, function () {
      if (action === "update") {
        print("demo-hunk", "Update " + recipe.id + " " + recipe.versions[0].version + " -> " + recipe.versions[LATEST].version);
      }
      change.lines.forEach(function (line) { print(line[0], line[1]); });
      change.changed.forEach(function (path) {
        print("", (!before[path] ? "Created " : !after[path] ? "Deleted " : "Updated ") + path);
      });
      const goal = item.node.querySelector(".demo-goal").textContent.trim();
      if (action === "discard") print("demo-done", "Took out what " + recipe.id + " brought. Your own lines: untouched.");
      else print("demo-done", "✓ " + goal + ".");

      const known = {};
      Object.keys(before).forEach(function (path) {
        before[path].forEach(function (line) { known[path + "|" + keyOf(line)] = true; });
      });
      Object.keys(after).forEach(function (path) {
        after[path].forEach(function (line) {
          if (!known[path + "|" + keyOf(line)]) freshLines[path + "|" + keyOf(line)] = true;
        });
      });
      change.changed.forEach(function (path) { freshFiles[path] = true; });
      cooked = next;
      openFile = change.changed.filter(function (path) { return path !== LOCK && after[path]; })[0] || "README.md";
      commands++;
      busy = false;
      render();
      // The button that ran the command is hidden in the new state, which would leave a keyboard user at the page's top.
      const focused = document.activeElement;
      if (focused === document.body || item.node.contains(focused)) {
        const nextAction = { todo: "cook", cooked: "update", latest: "discard" }[item.node.dataset.state];
        item.node.querySelector('[data-action="' + nextAction + '"]').focus({ preventScroll: true });
      }
    });
  }

  function renderMenu(files) {
    const hasUpdate = items.some(function (item) { return cooked[item.recipe.id] === 0; });
    let suggested = false;
    items.forEach(function (item) {
      const at = cooked[item.recipe.id];
      const state = at === undefined ? "todo" : at === LATEST ? "latest" : "cooked";
      item.node.dataset.state = state;
      part("version", item.node).textContent = at === undefined ? "" : item.recipe.versions[at].version;
      const note = part("note", item.node);
      note.textContent = "";
      if (state === "todo") note.textContent = item.what;
      else if (state === "latest") note.textContent = "Up to date with what the team learned.";
      else note.append(make("b", "", item.recipe.versions[LATEST].version), " " + item.recipe.versions[LATEST].news);
      item.node.querySelectorAll("button").forEach(function (button) {
        const action = button.dataset.action;
        button.disabled = busy || action === "latest";
        const next = !busy && !suggested && (action === "update" ? state === "cooked" : action === "cook" && state === "todo" && !hasUpdate);
        button.classList.toggle("demo-next", next);
        if (next) suggested = true;
      });
    });
    const done = Object.keys(cooked).length;
    part("bar").style.width = (done / RECIPES.length * 100) + "%";
    part("count").textContent = done + " of " + RECIPES.length + " standards";
    part("lines").textContent = ownedLines(files);
    part("commands").textContent = commands;
  }

  function renderExplorer(files) {
    const tree = part("tree");
    const folders = {};
    tree.textContent = "";
    EXPLORER_ORDER.forEach(function (path) {
      if (!files[path]) return;
      const folder = folderOf(path);
      if (folder && !folders[folder]) {
        folders[folder] = true;
        const row = make("li", "demo-folder");
        row.innerHTML = iconOf("folder");
        row.append(make("span", "", folder));
        row.title = folder;
        tree.append(row);
      }
      const badge = !untouched[path] ? "U" : files[path].length !== untouched[path].length ? "M" : "";
      const row = make("li");
      const button = make("button", (folder ? "demo-nested " : "") + (badge ? "demo-" + badge : ""));
      button.type = "button";
      button.innerHTML = fileIcon(path);
      button.append(make("span", "", baseName(path)), make("b", "", badge));
      if (view === "file" && path === openFile) button.setAttribute("aria-current", "true");
      if (freshFiles[path]) button.classList.add("demo-fresh");
      button.addEventListener("click", function () {
        openFile = path;
        view = "file";
        freshFiles = {};
        render();
        tree.querySelector("[aria-current]").focus();
      });
      row.append(button);
      tree.append(row);
    });
  }

  function renderEditor(files, complete) {
    const tabs = part("tabs");
    tabs.textContent = "";
    const tab = function (icon, name, target) {
      const button = make("button", "demo-tab");
      button.type = "button";
      button.innerHTML = icon;
      const close = make("span", "demo-close", "×");
      close.setAttribute("aria-hidden", "true");
      button.append(make("span", "", name), close);
      if (view === target) button.setAttribute("aria-current", "true");
      button.addEventListener("click", function () {
        view = target;
        render();
        tabs.querySelector("[aria-current]").focus();
      });
      tabs.append(button);
    };
    tab(fileIcon(openFile), baseName(openFile), "file");
    if (complete) tab(iconOf("check"), "Up to standard", "win");

    const code = part("code");
    code.textContent = "";
    if (view === "win") {
      const win = part("win").content.cloneNode(true);
      part("win-commands", win).textContent = commands;
      part("win-lines", win).textContent = ownedLines(files);
      code.append(win);
      return;
    }
    files[openFile].forEach(function (line, index) {
      const row = make("div", "demo-line");
      if (line.recipe) row.classList.add("demo-owned", line.recipe.tone);
      if (line.marker) row.classList.add("demo-marker");
      if (freshLines[openFile + "|" + keyOf(line)]) row.classList.add("demo-fresh");
      row.append(
        make("span", "demo-number", index + 1),
        make("span", "demo-text", line.text),
        make("span", "demo-who", line.recipe && openFile !== LOCK ? line.recipe.id : "")
      );
      code.append(row);
    });
  }

  function renderStatus(files) {
    const status = part("status");
    const done = Object.keys(cooked).length;
    status.textContent = "";
    status.classList.toggle("demo-pass", done > 0);
    if (!done) {
      status.textContent = "cook check: no cook.lock yet";
      return;
    }
    status.append(
      make("span", "", "✓ cook check: files match cook.lock"),
      make("span", "", done + (done === 1 ? " recipe" : " recipes") + " · " + ownedLines(files) + " lines owned")
    );
  }

  function render() {
    const files = project(cooked);
    const complete = RECIPES.every(function (recipe) { return cooked[recipe.id] === LATEST; });
    if (complete && !wasComplete) view = "win";
    if (!complete) view = "file";
    wasComplete = complete;
    if (!files[openFile]) openFile = "README.md";
    renderMenu(files);
    renderExplorer(files);
    renderEditor(files, complete);
    renderStatus(files);
  }

  items.forEach(function (item) {
    item.node.querySelectorAll("button").forEach(function (button) {
      button.addEventListener("click", function () { run(button.dataset.action, item); });
    });
  });
  render();
  demo.closest(".demo-section").hidden = false;
})();

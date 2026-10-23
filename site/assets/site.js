// Small enhancements over the rendered Markdown; every page reads fully without them.
(function () {
  var site = window.THE_COOK || { base: "", source: "", pages: [] };
  var pages = {};
  site.pages.forEach(function (url) { pages[site.base + url] = true; });

  // A link to a file the site does not render, such as a recipe's .groovy source, a script or a directory, opens on
  // GitHub at the release these pages document.
  function linkSources() {
    document.querySelectorAll(".prose a[href]").forEach(function (a) {
      var raw = a.getAttribute("href");
      if (!raw || raw.charAt(0) === "#" || /^[a-z][a-z0-9+.-]*:/i.test(raw)) return;
      var url = new URL(raw, location.href);
      if (url.origin !== location.origin) return;
      var path = url.pathname;
      if (pages[path] || pages[path + "/"] || pages[path.replace(/index\.html$/, "")]) return;
      if (path.indexOf(site.base + "/docs/assets/") === 0 || path.indexOf(site.base + "/site/") === 0) return;
      var repoPath = decodeURIComponent(path.slice(site.base.length)).replace(/^\/+/, "").replace(/\/$/, "");
      a.href = site.source + repoPath + url.hash;
    });
  }

  function anchors() {
    document.querySelectorAll(".prose h2[id], .prose h3[id], .prose h4[id]").forEach(function (h) {
      var a = document.createElement("a");
      a.className = "anchor";
      a.href = "#" + h.id;
      a.setAttribute("aria-label", "Link to this section");
      a.textContent = "#";
      h.insertBefore(a, h.firstChild);
    });
  }

  function toc() {
    var aside = document.querySelector(".toc");
    var headings = document.querySelectorAll(".prose h2[id]");
    if (!aside || headings.length < 3) return;
    var title = document.createElement("p");
    title.textContent = "On this page";
    var list = document.createElement("ul");
    var links = [];
    headings.forEach(function (h) {
      var li = document.createElement("li");
      var a = document.createElement("a");
      a.href = "#" + h.id;
      a.textContent = h.textContent.replace(/^#/, "");
      li.appendChild(a);
      list.appendChild(li);
      links.push([h, a]);
    });
    aside.appendChild(title);
    aside.appendChild(list);
    if (!("IntersectionObserver" in window)) return;
    var observer = new IntersectionObserver(function () {
      var current = links[0];
      links.forEach(function (pair) {
        if (pair[0].getBoundingClientRect().top < 140) current = pair;
      });
      links.forEach(function (pair) { pair[1].classList.toggle("active", pair === current); });
    }, { rootMargin: "0px 0px -60% 0px", threshold: [0, 1] });
    headings.forEach(function (h) { observer.observe(h); });
  }

  function copyButtons() {
    if (!navigator.clipboard) return;
    document.querySelectorAll("div.highlighter-rouge").forEach(function (block) {
      var code = block.querySelector("pre code") || block.querySelector("pre");
      if (!code) return;
      var button = document.createElement("button");
      button.type = "button";
      button.className = "copy";
      button.textContent = "copy";
      button.addEventListener("click", function () {
        navigator.clipboard.writeText(code.textContent).then(function () {
          button.textContent = "copied";
          setTimeout(function () { button.textContent = "copy"; }, 1500);
        });
      });
      block.appendChild(button);
    });
  }

  // On the landing page, a pin on the lab image and its legend entry light up together.
  function labPins() {
    document.querySelectorAll("[data-pin]").forEach(function (el) {
      var id = el.getAttribute("data-pin");
      var pair = document.querySelectorAll('[data-pin="' + id + '"]');
      function set(on) { pair.forEach(function (p) { p.classList.toggle("on", on); }); }
      el.addEventListener("mouseenter", function () { set(true); });
      el.addEventListener("mouseleave", function () { set(false); });
      el.addEventListener("focus", function () { set(true); });
      el.addEventListener("blur", function () { set(false); });
    });
  }

  linkSources();
  anchors();
  toc();
  copyButtons();
  labPins();
})();

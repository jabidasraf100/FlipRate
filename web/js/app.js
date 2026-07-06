function applyTheme(theme) {
  document.body.setAttribute("data-theme", theme);
  document.getElementById("theme-toggle").textContent = theme === "dark" ? "☀️" : "🌙";
}

function initTheme() {
  let theme = getTheme();
  if (!theme) {
    theme = window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
  }
  applyTheme(theme);

  document.getElementById("theme-toggle").addEventListener("click", () => {
    const current = document.body.getAttribute("data-theme");
    const next = current === "dark" ? "light" : "dark";
    applyTheme(next);
    setTheme(next);
    renderRateChart(converterState.from, converterState.to);
  });
}

function init() {
  initTheme();
  initConverter();
  initPopularPairs();
  initFavorites();
  initWishlist();
  renderRateChart(converterState.from, converterState.to);
}

document.addEventListener("DOMContentLoaded", init);

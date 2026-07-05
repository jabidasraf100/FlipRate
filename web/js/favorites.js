function renderFavorites() {
  const favorites = getFavorites();
  const listEl = document.getElementById("favorites-list");
  const emptyEl = document.getElementById("favorites-empty");

  listEl.innerHTML = "";
  emptyEl.hidden = favorites.length > 0;

  favorites.forEach((fav, index) => {
    const li = document.createElement("li");

    const label = document.createElement("span");
    label.className = "pair-label";
    label.textContent = `${fav.from} → ${fav.to}`;
    label.addEventListener("click", () => {
      setConverterPair(fav.from, fav.to);
    });

    const removeBtn = document.createElement("button");
    removeBtn.className = "remove-btn";
    removeBtn.type = "button";
    removeBtn.textContent = "✕";
    removeBtn.setAttribute("aria-label", `Remove ${fav.from} to ${fav.to} favorite`);
    removeBtn.addEventListener("click", () => {
      const favorites = getFavorites();
      favorites.splice(index, 1);
      saveFavorites(favorites);
      renderFavorites();
    });

    li.appendChild(label);
    li.appendChild(removeBtn);
    listEl.appendChild(li);
  });
}

function showFavoritesMessage(message) {
  const msgEl = document.getElementById("favorites-msg");
  msgEl.textContent = message;
  msgEl.hidden = !message;
}

function addCurrentPairToFavorites() {
  const favorites = getFavorites();
  const { from, to } = converterState;

  if (favorites.length >= MAX_LIST_LENGTH) {
    showFavoritesMessage(`Favorites full (${MAX_LIST_LENGTH}/${MAX_LIST_LENGTH}) — remove one to add another.`);
    return;
  }

  showFavoritesMessage("");
  favorites.push({ from, to });
  saveFavorites(favorites);
  renderFavorites();
}

function initFavorites() {
  document.getElementById("save-favorite-btn").addEventListener("click", addCurrentPairToFavorites);
  renderFavorites();
}

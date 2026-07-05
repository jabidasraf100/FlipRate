function showWishlistMessage(message) {
  const msgEl = document.getElementById("wishlist-msg");
  msgEl.textContent = message;
  msgEl.hidden = !message;
}

async function renderWishlist() {
  const wishlist = getWishlist();
  const tbody = document.getElementById("wishlist-tbody");
  const emptyEl = document.getElementById("wishlist-empty");

  emptyEl.hidden = wishlist.length > 0;
  tbody.innerHTML = "";

  for (let index = 0; index < wishlist.length; index++) {
    const item = wishlist[index];
    const tr = document.createElement("tr");

    let rate = null;
    try {
      const rates = await getLiveRates(item.from);
      rate = item.from === item.to ? 1 : rates[item.to];
    } catch {
      rate = null;
    }

    const total = rate == null ? null : item.amount * rate;

    tr.innerHTML = `
      <td>${formatNumber(item.amount)} ${item.from}</td>
      <td>${item.to}</td>
      <td>${rate == null ? "—" : formatNumber(rate)}</td>
      <td>${total == null ? "—" : formatNumber(total)}</td>
      <td></td>
    `;

    const removeBtn = document.createElement("button");
    removeBtn.className = "remove-btn";
    removeBtn.type = "button";
    removeBtn.textContent = "✕";
    removeBtn.setAttribute("aria-label", `Remove wishlist entry ${item.from} to ${item.to}`);
    removeBtn.addEventListener("click", () => {
      const wishlist = getWishlist();
      wishlist.splice(index, 1);
      saveWishlist(wishlist);
      renderWishlist();
    });
    tr.lastElementChild.appendChild(removeBtn);

    tbody.appendChild(tr);
  }
}

function addCurrentPairToWishlist() {
  const wishlist = getWishlist();
  const { from, to, amount } = converterState;

  if (wishlist.length >= MAX_LIST_LENGTH) {
    showWishlistMessage(`Wishlist full (${MAX_LIST_LENGTH}/${MAX_LIST_LENGTH}) — remove one to add another.`);
    return;
  }

  showWishlistMessage("");
  wishlist.push({ from, to, amount });
  saveWishlist(wishlist);
  renderWishlist();
}

function initWishlist() {
  document.getElementById("add-wishlist-btn").addEventListener("click", addCurrentPairToWishlist);
  renderWishlist();
}

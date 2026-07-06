const POPULAR_PAIRS = [
  { from: "USD", to: "EUR" },
  { from: "USD", to: "GBP" },
  { from: "USD", to: "BDT" },
  { from: "EUR", to: "BDT" },
  { from: "GBP", to: "BDT" },
  { from: "USD", to: "JPY" },
  { from: "USD", to: "INR" },
];

function renderPopularPairs() {
  const listEl = document.getElementById("popular-pairs-list");

  POPULAR_PAIRS.forEach((pair) => {
    const li = document.createElement("li");

    const label = document.createElement("button");
    label.type = "button";
    label.className = "pair-label";
    label.textContent = `${pair.from} → ${pair.to}`;
    label.setAttribute("aria-label", `Load ${pair.from} to ${pair.to}`);
    label.addEventListener("click", () => {
      setConverterPair(pair.from, pair.to);
    });

    li.appendChild(label);
    listEl.appendChild(li);
  });
}

function initPopularPairs() {
  renderPopularPairs();
}

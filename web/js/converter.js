const converterState = {
  from: "USD",
  to: "EUR",
  amount: 1,
};

function populateCurrencySelects() {
  const fromSelect = document.getElementById("from-select");
  const toSelect = document.getElementById("to-select");

  const optionsHtml = CURRENCIES.map(
    (c) => `<option value="${c.code}">${formatCurrencyOption(c)}</option>`
  ).join("");

  fromSelect.innerHTML = optionsHtml;
  toSelect.innerHTML = optionsHtml;

  fromSelect.value = converterState.from;
  toSelect.value = converterState.to;
}

async function updateConverterResult() {
  const { from, to, amount } = converterState;
  const resultEl = document.getElementById("converted-amount");
  const rateLineEl = document.getElementById("rate-line");

  try {
    const rates = await getLiveRates(from);
    const rate = from === to ? 1 : rates[to];

    if (rate == null) {
      resultEl.textContent = "Rate unavailable";
      rateLineEl.textContent = "—";
      return;
    }

    const converted = amount * rate;
    resultEl.textContent = `${formatNumber(converted)} ${to}`;
    rateLineEl.textContent = `1 ${from} = ${formatNumber(rate)} ${to}`;
  } catch (err) {
    resultEl.textContent = "Unable to fetch rate";
    rateLineEl.textContent = "Check your connection and try again.";
  }

  if (typeof onConverterPairChanged === "function") {
    onConverterPairChanged(from, to);
  }
}

function formatNumber(value) {
  if (!isFinite(value)) return "—";
  return value.toLocaleString(undefined, { maximumFractionDigits: 6 });
}

function setConverterPair(from, to, amount) {
  converterState.from = from;
  converterState.to = to;
  if (amount !== undefined) {
    converterState.amount = amount;
    document.getElementById("amount-input").value = amount;
  }
  document.getElementById("from-select").value = from;
  document.getElementById("to-select").value = to;
  updateConverterResult();
}

function initConverter() {
  populateCurrencySelects();

  const amountInput = document.getElementById("amount-input");
  const fromSelect = document.getElementById("from-select");
  const toSelect = document.getElementById("to-select");
  const swapBtn = document.getElementById("swap-btn");

  amountInput.addEventListener("input", () => {
    const value = parseFloat(amountInput.value);
    converterState.amount = isNaN(value) ? 0 : value;
    updateConverterResult();
  });

  fromSelect.addEventListener("change", () => {
    converterState.from = fromSelect.value;
    updateConverterResult();
  });

  toSelect.addEventListener("change", () => {
    converterState.to = toSelect.value;
    updateConverterResult();
  });

  swapBtn.addEventListener("click", () => {
    const { from, to } = converterState;
    setConverterPair(to, from);
  });

  updateConverterResult();
}

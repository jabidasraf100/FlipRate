// Placeholder — replace with the real GitHub owner/repo before deploying.
const RATES_HISTORY_URL =
  "https://raw.githubusercontent.com/YOUR_GH_USERNAME/FlipRate/main/data/rates-history.json";

const LIVE_RATES_ENDPOINT = "https://api.exchangerate-api.com/v4/latest/";

// In-memory session cache keyed by base currency code, so we don't refetch
// on every keystroke — only when the "From" currency changes.
const ratesCache = new Map();

async function getLiveRates(base) {
  if (ratesCache.has(base)) {
    return ratesCache.get(base);
  }
  const res = await fetch(LIVE_RATES_ENDPOINT + base);
  if (!res.ok) {
    throw new Error(`Failed to fetch rates for ${base}`);
  }
  const data = await res.json();
  ratesCache.set(base, data.rates);
  return data.rates;
}

let ratesHistoryCache = null;

async function getRatesHistory() {
  if (ratesHistoryCache) {
    return ratesHistoryCache;
  }
  const res = await fetch(RATES_HISTORY_URL);
  if (!res.ok) {
    throw new Error("Failed to fetch rates history");
  }
  ratesHistoryCache = await res.json();
  return ratesHistoryCache;
}

// Computes the last-7-days ratio series for a from->to pair from the
// (up to 30-entry) history, handling the base currency being either side.
function computeHistorySeries(history, from, to) {
  const last7 = history.slice(-7);
  return last7
    .map((entry) => {
      const rate = pairRateFromEntry(entry, from, to);
      return rate === null ? null : { date: entry.date, rate };
    })
    .filter((point) => point !== null);
}

function pairRateFromEntry(entry, from, to) {
  const { base, rates } = entry;
  if (from === to) return 1;
  if (from === base && to in rates) return rates[to];
  if (to === base && from in rates) return 1 / rates[from];
  if (from in rates && to in rates) return rates[to] / rates[from];
  return null;
}

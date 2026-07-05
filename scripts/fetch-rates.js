// Run daily by .github/workflows/fetch-rates.yml.
// Fetches today's USD rates, appends them to data/rates-history.json,
// and trims the file to the most recent 30 entries.
const https = require("https");
const fs = require("fs");
const path = require("path");

const API_URL = "https://api.exchangerate-api.com/v4/latest/USD";
const HISTORY_PATH = path.join(__dirname, "..", "data", "rates-history.json");
const MAX_ENTRIES = 30;

function fetchJson(url) {
  return new Promise((resolve, reject) => {
    https
      .get(url, (res) => {
        if (res.statusCode !== 200) {
          reject(new Error(`Request failed with status ${res.statusCode}`));
          res.resume();
          return;
        }
        let data = "";
        res.on("data", (chunk) => (data += chunk));
        res.on("end", () => {
          try {
            resolve(JSON.parse(data));
          } catch (err) {
            reject(err);
          }
        });
      })
      .on("error", reject);
  });
}

async function main() {
  const payload = await fetchJson(API_URL);
  const today = payload.date || new Date().toISOString().slice(0, 10);

  let history = [];
  if (fs.existsSync(HISTORY_PATH)) {
    history = JSON.parse(fs.readFileSync(HISTORY_PATH, "utf8"));
  }

  const entry = { date: today, base: "USD", rates: payload.rates };

  const existingIndex = history.findIndex((h) => h.date === today);
  if (existingIndex >= 0) {
    history[existingIndex] = entry;
  } else {
    history.push(entry);
  }

  history.sort((a, b) => (a.date < b.date ? -1 : a.date > b.date ? 1 : 0));
  const trimmed = history.slice(-MAX_ENTRIES);

  fs.writeFileSync(HISTORY_PATH, JSON.stringify(trimmed, null, 2) + "\n");
  console.log(`Wrote ${trimmed.length} entries (latest: ${today}) to ${HISTORY_PATH}`);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});

# FlipRate — Build Specification

A free, no-account currency converter available as a website and an Android app. This document is the complete build spec — implement exactly what's described here.

## Overview

FlipRate lets people convert between world currencies, track favorite currency pairs, and monitor specific amounts (wishlist) as their real-world value changes day to day. No backend, no user accounts, no login. Everything is either called live from a free public API or stored locally on the user's own device/browser.

**Platforms:** Web (plain HTML/CSS/JS) and Android (Java)
**Cost to run:** $0 — free API tier + free GitHub Actions + local storage only

---

## 1. Data sources

### 1.1 Live rates
- **Provider:** ExchangeRate-API (free tier)
- **Endpoint:** `https://api.exchangerate-api.com/v4/latest/{BASE}` (open access, no key required) — or the registered free-tier endpoint if a key is added later (`https://v6.exchangerate-api.com/v6/{API_KEY}/latest/{BASE}`)
- Called directly from the client (web JS or Android Java) — no server in between.
- Cache the response in memory during a session; don't refetch on every keystroke.
- Note: the open-access endpoint updates once every 24 hours and is rate-limited if hit too often — respect that and don't poll faster than hourly.

### 1.2 Seven-day rate history (self-collected)
Free tiers of every exchange rate API either lack historical data or lack BDT. To solve this, FlipRate collects its own daily history via a scheduled job.

**Component: GitHub Actions workflow**
- File: `.github/workflows/fetch-rates.yml`
- Trigger: `schedule` with a daily cron (e.g. `0 0 * * *` — once a day)
- Steps:
  1. Check out the repo
  2. Run a small script (Python or Node) that calls the ExchangeRate-API endpoint for a base currency (e.g. USD) and gets the full rates object
  3. Append that day's date + rates object as a new entry to `data/rates-history.json` (don't overwrite — append)
  4. Trim the file so it only keeps the most recent 30 days (rolling window; the app itself only needs the last 7, but keeping 30 gives headroom for future features)
  5. Commit and push the updated file back to the repo using the workflow's built-in `GITHUB_TOKEN`

**Component: `data/rates-history.json` structure**
```json
[
  { "date": "2026-06-28", "base": "USD", "rates": { "EUR": 0.92, "GBP": 0.79, "BDT": 110.5, "...": "..." } },
  { "date": "2026-06-29", "base": "USD", "rates": { "EUR": 0.921, "GBP": 0.788, "BDT": 110.6, "...": "..." } }
]
```

**Component: how the apps read it**
- Public raw URL: `https://raw.githubusercontent.com/{username}/{repo}/main/data/rates-history.json`
- Both web and Android fetch this URL directly (it's just a static JSON file, no auth needed since the repo is public)
- To build a chart for pair A→B: for each of the last 7 entries, compute `rates[B] / rates[A]` (or handle the case where the base itself is A or B)
- If fewer than 7 days of data exist yet (e.g. right after launch), show whatever days are available — don't fabricate missing days

---

## 2. Web app

### 2.1 Structure
Single page (`index.html`), no routing, no build step required. Plain HTML/CSS/JS, one file or a small set of files (`index.html`, `style.css`, `script.js`).

### 2.2 Sections (top to bottom)

**Header**
- App name "FlipRate"
- Dark/light mode toggle (persists choice in browser local storage)

**Converter**
- Amount input (numeric)
- "From" dropdown — every option displays as `{Full Currency Name} ({CODE})`, e.g. "Bangladeshi Taka (BDT)". Full major-currency list, no artificial subset.
- Swap button to flip From/To
- "To" dropdown — same display format
- Live converted result, auto-updating on any input change
- Displayed exchange rate (1 {FROM} = {rate} {TO})

**Actions row (below converter)**
- "Save as favorite" button — saves the current From/To pair only
- "Add to wishlist" button — saves the current From/To pair **and** the current amount entered

**Favorites list**
- Each entry: `[{FROM} → {TO}]` with an `X` remove button
- Clicking the pair (not the X) loads it into the converter
- Max 10 entries — once at 10, block adding more and show a small inline message asking the user to remove one first

**Wishlist table**
- Columns: Amount | Convert to | Current rate | Total | Remove
- "Current rate" and "Total" recompute live from the latest fetched rates (not frozen at save time)
- Each row is independent — saving the same pair twice with different (or same) amounts creates two separate rows, never merged
- Max 10 entries — same block-and-message behavior as favorites

**Rate history chart**
- Shows the 7-day trend for whichever pair is currently loaded in the converter
- Updates whenever the user picks a new pair, swaps, or clicks a favorite/wishlist entry
- Data source: `rates-history.json` as described in section 1.2
- Simple line chart is sufficient (a lightweight charting approach or minimal custom SVG/canvas drawing — implementer's choice, no heavy dependency required)

### 2.3 Local storage keys (suggested)
- `fliprate_theme` → `"dark"` or `"light"`
- `fliprate_favorites` → JSON array of `{ from, to }`, max length 10
- `fliprate_wishlist` → JSON array of `{ from, to, amount }`, max length 10

### 2.4 Dark/light mode
- Toggle switches a class or attribute on `<body>` (e.g. `data-theme="dark"`)
- All colors driven by CSS variables so both themes are defined in one place
- Persist the user's choice; default to system preference (`prefers-color-scheme`) on first visit if no saved choice exists

---

## 3. Android app

### 3.1 Structure
Single Activity, single scrollable layout (`ScrollView` containing a vertical `LinearLayout` or `ConstraintLayout`). Java. No login/account screens.

### 3.2 Layout (top to bottom, matches web ordering conceptually)

**Top bar**
- App name "FlipRate"
- Dark/light mode toggle (persisted via `SharedPreferences`)
- Three-dot overflow menu → opens "Popular pairs" list; tapping one of those pairs loads it into the main converter and updates the chart

**Converter card**
- Amount input (numeric keyboard)
- From/To currency pickers — each option displays `{Full Currency Name} ({CODE})`
- Swap button
- Live result + displayed rate, same behavior as web

**Action buttons**
- "Save as favorite" — saves pair only
- "Add to wishlist" — saves pair + current amount

**Favorites section**
- Horizontal chips or a simple list: `[{FROM} → {TO}]` with an `X` to remove
- Tapping the pair (not the X) loads it into the converter
- Max 10, same block-and-message rule as web

**Wishlist section**
- Card-style rows (not a table — screen too narrow), one card per entry showing:
  - Amount + source currency
  - Converted-to currency
  - Current rate (live)
  - Total (live)
  - Remove button
- Max 10, same block-and-message rule

**Rate history chart**
- Below wishlist, on the same scrollable main screen
- Shows 7-day trend for the currently loaded pair
- Updates on any pair change (converter selection, favorite tap, wishlist tap, or popular-pair selection from the 3-dot menu)
- Data source: same `rates-history.json` raw GitHub URL, fetched over HTTP from the Android app

### 3.3 Local storage
- Use `SharedPreferences` (simple key-value, fine for this scale) or a lightweight local JSON file in app storage
- Same three data points as web: theme, favorites (max 10), wishlist (max 10)

### 3.4 Popular pairs (3-dot menu)
- Fixed shortlist (e.g. USD/EUR, USD/GBP, USD/BDT, EUR/BDT, GBP/BDT, USD/JPY, USD/INR — implementer can finalize the exact list)
- Selecting one loads it into the main converter and refreshes the chart, same as tapping a favorite

---

## 4. Explicit non-goals for v1

To keep this buildable and avoid scope creep, the following are **out of scope** for v1:

- No user accounts, login, or cross-device sync
- No backend server or database beyond the static `rates-history.json` file in the repo
- No monetization (ads, affiliate links, paid tiers) — add only after v1 is working
- No push notifications or rate alerts
- No currency conversion for cryptocurrencies
- No offline mode beyond whatever the browser/OS caches naturally

---

## 5. Open items for implementer discretion

These were intentionally left flexible and can be decided during implementation without needing to check back:

- Exact chart library/approach for both platforms (any lightweight option is fine)
- Exact visual design, colors, and typography (dark/light mode is required; specific palette is not specified here)
- Exact wording of the "list full" message when favorites/wishlist hit 10 items
- Final fixed list of currencies shown in the "popular pairs" shortlist

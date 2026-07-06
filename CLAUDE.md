# CLAUDE.md

Operational guidance for Claude Code when working in this repo. This file is for
*how to work here* (commands, conventions, gotchas). For *what the product is
and does*, see [FlipRate-README.md](FlipRate-README.md) — that's the build
spec and product source of truth; don't duplicate it here, and if the two
ever disagree, treat the README as canonical for product behavior and update
code (or ask the user) rather than silently reinterpreting it.

## What this is

FlipRate: a free, no-account currency converter shipped as a static web app
and an Android app. No backend — both clients call the free ExchangeRate-API
directly, plus a self-collected 7-day/30-day rate history file that a daily
GitHub Actions job appends to. Both clients implement the same feature set
(converter, favorites, wishlist, rate history chart, dark/light mode) and
should be kept in parity — a feature added to one side belongs on the other
unless the README says otherwise.

## Layout

- `web/` — plain HTML/CSS/JS, no build step, no bundler, no npm. Scripts are
  loaded as ordinary `<script>` tags in `index.html` in dependency order
  (`currencies.js` → `storage.js` → `rates.js` → `converter.js` →
  `favorites.js` → `wishlist.js` → `chart.js` → `app.js`) and share state via
  plain globals (e.g. `converterState`, `CURRENCIES`). Keep that load order
  if you add a new script.
- `android/` — single-Activity Java app (`MainActivity`), Gradle project
  rooted at `android/` (not repo root). No ViewBinding/DataBinding — views
  are wired with manual `findViewById`. Async work uses callback interfaces
  (`RatesRepository.RatesCallback`, `RatesHistoryRepository.SeriesCallback`)
  backed by a cached thread pool + a main-thread `Handler`, not coroutines/
  RxJava/AsyncTask.
- `scripts/fetch-rates.js` — Node script the GitHub Action runs daily. Fetches
  USD rates from ExchangeRate-API, upserts today's entry into
  `data/rates-history.json`, sorts by date, and trims to the most recent 30
  entries.
- `.github/workflows/fetch-rates.yml` — daily cron (`0 0 * * *`) plus manual
  `workflow_dispatch`. Runs the script, then commits/pushes
  `data/rates-history.json` using the built-in `GITHUB_TOKEN`.
- `data/rates-history.json` — rolling window of daily `{date, base, rates}`
  snapshots (base is always `"USD"`). Both clients fetch this file straight
  from the `raw.githubusercontent.com` URL for the 7-day chart.

## Commands

There is no package.json / build tool at the repo root and none is needed for
the web app.

- **Web app:** open `web/index.html` directly, or serve the `web/` folder
  with any static file server (needed if you hit CORS issues opening via
  `file://`). No lint/test/build commands exist.
- **Rate history script:** `node scripts/fetch-rates.js` (run from repo
  root — it resolves `data/rates-history.json` relative to its own path).
- **Android:** standard Gradle build from the `android/` directory (e.g.
  `./gradlew assembleDebug` on a machine with the Android SDK configured —
  no `gradlew` wrapper is currently checked in, so an IDE build via Android
  Studio is the practical path).

## Gotchas / things easy to break

- **Hardcoded GitHub raw URL:** the rates-history URL
  (`https://raw.githubusercontent.com/jabidasraf100/FlipRate/main/data/rates-history.json`)
  is duplicated in two places — [web/js/rates.js](web/js/rates.js) and
  [RatesHistoryRepository.java](android/app/src/main/java/com/nexusbuild/apps/fliprate/data/RatesHistoryRepository.java).
  If the GitHub username/repo ever changes, update both.
- **Currency list is API-constrained, not arbitrary:** `web/js/currencies.js`
  and `android/app/src/main/assets/currencies.json` are deliberately
  restricted to codes the free ExchangeRate-API `/latest/USD` endpoint
  actually returns, so every listed currency is guaranteed convertible. Don't
  add a currency to one list without confirming the API returns a rate for
  it, and keep both lists in sync.
- **Max list length (10) is duplicated:** `MAX_LIST_LENGTH` in
  `web/js/storage.js` and `PreferencesStore.MAX_LIST_LENGTH` in Android. Same
  value, two places, per the README's favorites/wishlist cap.
- **Storage key naming differs slightly by platform on purpose:** web uses
  `localStorage` keys `fliprate_theme` / `fliprate_favorites` /
  `fliprate_wishlist`; Android uses a `fliprate_prefs` SharedPreferences file
  with `fliprate_theme_dark` / `fliprate_theme_set` / `fliprate_favorites` /
  `fliprate_wishlist`. This is expected — they're separate storage systems,
  not a shared format.
- **No backend, ever (for v1):** don't introduce a server, database, or
  user accounts — see the README's "Explicit non-goals" section before
  adding anything that smells like one.
- **Rate fetch is session-cached, not live-polled:** both `getLiveRates`
  (web) and `RatesRepository.getRates` (Android) cache by base currency for
  the life of the page/app session. Don't add polling/refetch-on-every-
  keystroke behavior — the README explicitly calls out the API's rate limits.

## Devlog

Check [DEVLOG.md](DEVLOG.md) for a running log of notable work in this repo
and append a dated entry there for anything non-trivial you do (new
features, bug fixes, architectural changes) — not for trivial edits like
typo fixes.

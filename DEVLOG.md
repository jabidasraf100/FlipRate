# FlipRate Devlog

Reverse-chronological log of notable development activity on this repo.
Newest entry at the top. Use absolute dates (YYYY-MM-DD), not "yesterday"/
"last week". Add an entry for anything non-trivial (new features, bug fixes,
architectural changes) — skip it for typo fixes or pure formatting.

Entry format:

```
## YYYY-MM-DD — Short title
What changed, in a sentence or two. Why, if not obvious. Files touched, if
worth calling out.
```

---

## 2026-07-06 — Added Popular Pairs to the web app

The original spec only defined "Popular pairs" for Android (section 3.4, a
3-dot overflow menu). User asked why web didn't have it and confirmed they
wanted it added for platform parity.

Added a static "Popular pairs" card to web, between the Converter and
Favorites sections, listing the same 7 pairs as Android's `PopularPairs.LIST`
(USD→EUR, USD→GBP, USD→BDT, EUR→BDT, GBP→BDT, USD→JPY, USD→INR) as
keyboard-focusable pill buttons. Clicking one calls the existing
`setConverterPair()` — the same function Favorites already uses — so no new
state machine or chart-wiring was needed. Unlike Favorites/Wishlist, this
list is fixed and not persisted to `localStorage`.

Files: `web/js/popularpairs.js` (new), `web/index.html` (new section + script
tag after `converter.js`), `web/style.css` (pill-button reset styles),
`web/js/app.js` (`initPopularPairs()` wired into `init()`). Android untouched
— it was already correct and served as the parity reference.

## 2026-07-06 — Baseline audit; added CLAUDE.md and this devlog

No product code changed. Read through the full repo against
[FlipRate-README.md](FlipRate-README.md) to establish a starting baseline,
then added [CLAUDE.md](CLAUDE.md) (operational guidance for future Claude
Code sessions) and this devlog.

Findings from the audit — the repo already has a complete, working v1
implementation of the spec on both platforms:

- **Web** (`web/`): single-page vanilla HTML/CSS/JS, no build step. Converter,
  swap, favorites (max 10), wishlist with live-recomputed rate/total (max 10),
  dark/light theme with system-preference default, and a hand-drawn canvas
  line chart for the 7-day history are all implemented and wired together in
  `web/js/app.js`.
- **Android** (`android/`): single-Activity Java app mirroring the web
  feature set — converter card, favorites chips, wishlist cards, a custom
  `RateChartView`, dark/light theme via `AppCompatDelegate` +
  `SharedPreferences`, and a "popular pairs" overflow menu. Uses a manual
  `HttpURLConnection` + `ExecutorService` + callback pattern for networking
  (no Retrofit/OkHttp/coroutines).
- **Rate history pipeline**: `.github/workflows/fetch-rates.yml` runs
  `scripts/fetch-rates.js` daily, which upserts today's USD rates into
  `data/rates-history.json` and trims it to 30 entries. Both clients compute
  the last-7-day series client-side from that same file
  (`pairRateFromEntry` in web, `pairRate` in Android), handling the case
  where the requested pair includes the history's base currency.
- **Known duplication to watch**: the raw GitHub history URL, the currency
  list, and `MAX_LIST_LENGTH` are each defined once per platform (JS and
  Java) rather than shared — documented in CLAUDE.md's Gotchas section
  rather than refactored, since introducing a shared-source build step would
  be a bigger architectural change than this pass called for.

No bugs were found that block the spec's v1 scope; nothing was fixed in this
pass since none was requested.

package com.nexusbuild.apps.fliprate.data;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RatesHistoryRepository {
    public interface HistoryCallback {
        void onSuccess(List<HistoryEntry> history);
        void onError(Exception e);
    }

    public interface SeriesCallback {
        void onSuccess(List<RatePoint> series);
        void onError(Exception e);
    }

    public static class HistoryEntry {
        public final String date;
        public final String base;
        public final java.util.Map<String, Double> rates;

        HistoryEntry(String date, String base, java.util.Map<String, Double> rates) {
            this.date = date;
            this.base = base;
            this.rates = rates;
        }
    }

    public static class RatePoint {
        public final String date;
        public final double rate;

        public RatePoint(String date, double rate) {
            this.date = date;
            this.rate = rate;
        }
    }

    // Placeholder — replace with the real GitHub owner/repo before deploying.
    private static final String RATES_HISTORY_URL =
            "https://raw.githubusercontent.com/YOUR_GH_USERNAME/FlipRate/main/data/rates-history.json";

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private List<HistoryEntry> cachedHistory;

    public void getHistory(HistoryCallback callback) {
        if (cachedHistory != null) {
            callback.onSuccess(cachedHistory);
            return;
        }
        executor.execute(() -> {
            try {
                String json = RatesRepository.fetchUrl(RATES_HISTORY_URL);
                List<HistoryEntry> history = parseHistory(json);
                cachedHistory = history;
                mainHandler.post(() -> callback.onSuccess(history));
            } catch (IOException | JSONException e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    public void getSeries(String from, String to, SeriesCallback callback) {
        getHistory(new HistoryCallback() {
            @Override
            public void onSuccess(List<HistoryEntry> history) {
                callback.onSuccess(computeSeries(history, from, to));
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        });
    }

    static List<HistoryEntry> parseHistory(String json) throws JSONException {
        JSONArray arr = new JSONArray(json);
        List<HistoryEntry> history = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject obj = arr.getJSONObject(i);
            String date = obj.getString("date");
            String base = obj.getString("base");
            JSONObject ratesJson = obj.getJSONObject("rates");
            java.util.Map<String, Double> rates = new java.util.HashMap<>();
            Iterator<String> keys = ratesJson.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                rates.put(key, ratesJson.getDouble(key));
            }
            history.add(new HistoryEntry(date, base, rates));
        }
        return history;
    }

    // Computes the last-7-days ratio series for a from->to pair, handling
    // the case where the base currency itself is one side of the pair.
    static List<RatePoint> computeSeries(List<HistoryEntry> history, String from, String to) {
        List<RatePoint> series = new ArrayList<>();
        int start = Math.max(0, history.size() - 7);
        for (int i = start; i < history.size(); i++) {
            HistoryEntry entry = history.get(i);
            Double rate = pairRate(entry, from, to);
            if (rate != null) {
                series.add(new RatePoint(entry.date, rate));
            }
        }
        return series;
    }

    private static Double pairRate(HistoryEntry entry, String from, String to) {
        if (from.equals(to)) return 1.0;
        if (from.equals(entry.base) && entry.rates.containsKey(to)) {
            return entry.rates.get(to);
        }
        if (to.equals(entry.base) && entry.rates.containsKey(from)) {
            return 1.0 / entry.rates.get(from);
        }
        if (entry.rates.containsKey(from) && entry.rates.containsKey(to)) {
            return entry.rates.get(to) / entry.rates.get(from);
        }
        return null;
    }
}

package com.nexusbuild.apps.fliprate.data;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RatesRepository {
    public interface RatesCallback {
        void onSuccess(Map<String, Double> rates);
        void onError(Exception e);
    }

    private static final String LIVE_RATES_ENDPOINT = "https://api.exchangerate-api.com/v4/latest/";

    // In-memory session cache keyed by base currency; avoids refetching on
    // every keystroke, only refetches when the "From" currency changes.
    private final Map<String, Map<String, Double>> cache = new HashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public void getRates(String base, RatesCallback callback) {
        Map<String, Double> cached = cache.get(base);
        if (cached != null) {
            callback.onSuccess(cached);
            return;
        }

        executor.execute(() -> {
            try {
                String json = fetchUrl(LIVE_RATES_ENDPOINT + base);
                JSONObject root = new JSONObject(json);
                JSONObject ratesJson = root.getJSONObject("rates");
                Map<String, Double> rates = new HashMap<>();
                Iterator<String> keys = ratesJson.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    rates.put(key, ratesJson.getDouble(key));
                }
                cache.put(base, rates);
                mainHandler.post(() -> callback.onSuccess(rates));
            } catch (IOException | JSONException e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    static String fetchUrl(String urlString) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
        try {
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            int status = conn.getResponseCode();
            if (status != 200) {
                throw new IOException("Request failed with status " + status);
            }
            InputStream is = conn.getInputStream();
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } finally {
            conn.disconnect();
        }
    }
}

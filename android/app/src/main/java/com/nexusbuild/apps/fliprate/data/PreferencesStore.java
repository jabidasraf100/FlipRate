package com.nexusbuild.apps.fliprate.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.nexusbuild.apps.fliprate.model.FavoritePair;
import com.nexusbuild.apps.fliprate.model.WishlistItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PreferencesStore {
    public static final int MAX_LIST_LENGTH = 10;

    private static final String PREFS_NAME = "fliprate_prefs";
    private static final String KEY_THEME_DARK = "fliprate_theme_dark";
    private static final String KEY_THEME_SET = "fliprate_theme_set";
    private static final String KEY_FAVORITES = "fliprate_favorites";
    private static final String KEY_WISHLIST = "fliprate_wishlist";

    private final SharedPreferences prefs;

    public PreferencesStore(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean hasThemePreference() {
        return prefs.getBoolean(KEY_THEME_SET, false);
    }

    public boolean isDarkTheme() {
        return prefs.getBoolean(KEY_THEME_DARK, false);
    }

    public void setDarkTheme(boolean dark) {
        prefs.edit()
                .putBoolean(KEY_THEME_DARK, dark)
                .putBoolean(KEY_THEME_SET, true)
                .apply();
    }

    public List<FavoritePair> getFavorites() {
        List<FavoritePair> result = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(prefs.getString(KEY_FAVORITES, "[]"));
            for (int i = 0; i < arr.length(); i++) {
                result.add(FavoritePair.fromJson(arr.getJSONObject(i)));
            }
        } catch (JSONException ignored) {
        }
        return result;
    }

    public void saveFavorites(List<FavoritePair> favorites) {
        JSONArray arr = new JSONArray();
        try {
            for (FavoritePair fav : favorites) {
                arr.put(fav.toJson());
            }
        } catch (JSONException ignored) {
        }
        prefs.edit().putString(KEY_FAVORITES, arr.toString()).apply();
    }

    public List<WishlistItem> getWishlist() {
        List<WishlistItem> result = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(prefs.getString(KEY_WISHLIST, "[]"));
            for (int i = 0; i < arr.length(); i++) {
                result.add(WishlistItem.fromJson(arr.getJSONObject(i)));
            }
        } catch (JSONException ignored) {
        }
        return result;
    }

    public void saveWishlist(List<WishlistItem> wishlist) {
        JSONArray arr = new JSONArray();
        try {
            for (WishlistItem item : wishlist) {
                arr.put(item.toJson());
            }
        } catch (JSONException ignored) {
        }
        prefs.edit().putString(KEY_WISHLIST, arr.toString()).apply();
    }
}

package com.nexusbuild.apps.fliprate.model;

import org.json.JSONException;
import org.json.JSONObject;

public class FavoritePair {
    public final String from;
    public final String to;

    public FavoritePair(String from, String to) {
        this.from = from;
        this.to = to;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("from", from);
        obj.put("to", to);
        return obj;
    }

    public static FavoritePair fromJson(JSONObject obj) throws JSONException {
        return new FavoritePair(obj.getString("from"), obj.getString("to"));
    }
}

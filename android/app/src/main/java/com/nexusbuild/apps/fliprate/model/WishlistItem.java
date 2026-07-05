package com.nexusbuild.apps.fliprate.model;

import org.json.JSONException;
import org.json.JSONObject;

public class WishlistItem {
    public final String from;
    public final String to;
    public final double amount;

    public WishlistItem(String from, String to, double amount) {
        this.from = from;
        this.to = to;
        this.amount = amount;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("from", from);
        obj.put("to", to);
        obj.put("amount", amount);
        return obj;
    }

    public static WishlistItem fromJson(JSONObject obj) throws JSONException {
        return new WishlistItem(obj.getString("from"), obj.getString("to"), obj.getDouble("amount"));
    }
}

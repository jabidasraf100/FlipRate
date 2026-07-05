package com.nexusbuild.apps.fliprate.model;

public class Currency {
    public final String code;
    public final String name;

    public Currency(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String display() {
        return name + " (" + code + ")";
    }

    @Override
    public String toString() {
        return display();
    }
}

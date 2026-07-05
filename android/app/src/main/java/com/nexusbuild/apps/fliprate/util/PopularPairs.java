package com.nexusbuild.apps.fliprate.util;

import com.nexusbuild.apps.fliprate.model.FavoritePair;

import java.util.Arrays;
import java.util.List;

public class PopularPairs {
    public static final List<FavoritePair> LIST = Arrays.asList(
            new FavoritePair("USD", "EUR"),
            new FavoritePair("USD", "GBP"),
            new FavoritePair("USD", "BDT"),
            new FavoritePair("EUR", "BDT"),
            new FavoritePair("GBP", "BDT"),
            new FavoritePair("USD", "JPY"),
            new FavoritePair("USD", "INR")
    );
}

package com.nexusbuild.apps.fliprate;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.flexbox.FlexboxLayout;

import com.nexusbuild.apps.fliprate.data.PreferencesStore;
import com.nexusbuild.apps.fliprate.data.RatesHistoryRepository;
import com.nexusbuild.apps.fliprate.data.RatesRepository;
import com.nexusbuild.apps.fliprate.model.Currency;
import com.nexusbuild.apps.fliprate.model.FavoritePair;
import com.nexusbuild.apps.fliprate.model.WishlistItem;
import com.nexusbuild.apps.fliprate.ui.RateChartView;
import com.nexusbuild.apps.fliprate.util.PopularPairs;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private PreferencesStore prefsStore;
    private final RatesRepository ratesRepository = new RatesRepository();
    private final RatesHistoryRepository historyRepository = new RatesHistoryRepository();

    private List<Currency> currencies = new ArrayList<>();
    private String currentFrom = "USD";
    private String currentTo = "EUR";
    private double currentAmount = 1;

    private Spinner fromSpinner;
    private Spinner toSpinner;
    private EditText amountInput;
    private TextView convertedAmountView;
    private TextView rateLineView;
    private TextView favoritesMsgView;
    private TextView wishlistMsgView;
    private TextView favoritesEmptyView;
    private TextView wishlistEmptyView;
    private TextView chartEmptyView;
    private TextView chartTitleView;
    private FlexboxLayout favoritesContainer;
    private android.widget.LinearLayout wishlistContainer;
    private RateChartView rateChartView;

    private boolean suppressSpinnerEvents = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefsStore = new PreferencesStore(this);
        applyStoredOrSystemTheme();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindViews();
        currencies = loadCurrencies();
        setupSpinners();
        wireActions();

        updateConverterResult();
        renderFavorites();
        renderWishlist();
    }

    private void applyStoredOrSystemTheme() {
        if (prefsStore.hasThemePreference()) {
            AppCompatDelegate.setDefaultNightMode(prefsStore.isDarkTheme()
                    ? AppCompatDelegate.MODE_NIGHT_YES
                    : AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

    private void bindViews() {
        fromSpinner = findViewById(R.id.from_spinner);
        toSpinner = findViewById(R.id.to_spinner);
        amountInput = findViewById(R.id.amount_input);
        convertedAmountView = findViewById(R.id.converted_amount);
        rateLineView = findViewById(R.id.rate_line);
        favoritesMsgView = findViewById(R.id.favorites_msg);
        wishlistMsgView = findViewById(R.id.wishlist_msg);
        favoritesEmptyView = findViewById(R.id.favorites_empty);
        wishlistEmptyView = findViewById(R.id.wishlist_empty);
        chartEmptyView = findViewById(R.id.chart_empty);
        chartTitleView = findViewById(R.id.chart_title);
        favoritesContainer = findViewById(R.id.favorites_container);
        wishlistContainer = findViewById(R.id.wishlist_container);
        rateChartView = findViewById(R.id.rate_chart_view);

        amountInput.setText(formatAmountForInput(currentAmount));
    }

    private List<Currency> loadCurrencies() {
        List<Currency> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(getAssets().open("currencies.json"), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            JSONArray arr = new JSONArray(sb.toString());
            for (int i = 0; i < arr.length(); i++) {
                result.add(new Currency(arr.getJSONObject(i).getString("code"), arr.getJSONObject(i).getString("name")));
            }
        } catch (IOException | JSONException e) {
            throw new RuntimeException("Failed to load currencies.json", e);
        }
        return result;
    }

    private void setupSpinners() {
        ArrayAdapter<Currency> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, currencies);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fromSpinner.setAdapter(adapter);
        toSpinner.setAdapter(adapter);

        setSpinnerSelection(fromSpinner, currentFrom);
        setSpinnerSelection(toSpinner, currentTo);

        fromSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (suppressSpinnerEvents) return;
                currentFrom = currencies.get(position).code;
                updateConverterResult();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        toSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (suppressSpinnerEvents) return;
                currentTo = currencies.get(position).code;
                updateConverterResult();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setSpinnerSelection(Spinner spinner, String code) {
        for (int i = 0; i < currencies.size(); i++) {
            if (currencies.get(i).code.equals(code)) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private void wireActions() {
        amountInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    currentAmount = s.length() == 0 ? 0 : Double.parseDouble(s.toString());
                } catch (NumberFormatException e) {
                    currentAmount = 0;
                }
                updateConverterResult();
            }
        });

        findViewById(R.id.swap_btn).setOnClickListener(v -> {
            String from = currentFrom;
            currentFrom = currentTo;
            currentTo = from;
            suppressSpinnerEvents = true;
            setSpinnerSelection(fromSpinner, currentFrom);
            setSpinnerSelection(toSpinner, currentTo);
            suppressSpinnerEvents = false;
            updateConverterResult();
        });

        ((Button) findViewById(R.id.save_favorite_btn)).setOnClickListener(v -> addCurrentPairToFavorites());
        ((Button) findViewById(R.id.add_wishlist_btn)).setOnClickListener(v -> addCurrentPairToWishlist());

        ((ImageButton) findViewById(R.id.theme_toggle_btn)).setOnClickListener(v -> toggleTheme());
        ((ImageButton) findViewById(R.id.overflow_menu_btn)).setOnClickListener(this::showPopularPairsMenu);
    }

    private void toggleTheme() {
        boolean targetDark = !isCurrentlyDark();
        prefsStore.setDarkTheme(targetDark);
        AppCompatDelegate.setDefaultNightMode(targetDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }

    private boolean isCurrentlyDark() {
        int uiMode = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return uiMode == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    private void showPopularPairsMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenuInflater().inflate(R.menu.popular_pairs_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(this::onPopularPairSelected);
        popup.show();
    }

    private boolean onPopularPairSelected(MenuItem item) {
        int index;
        int id = item.getItemId();
        if (id == R.id.pair_usd_eur) index = 0;
        else if (id == R.id.pair_usd_gbp) index = 1;
        else if (id == R.id.pair_usd_bdt) index = 2;
        else if (id == R.id.pair_eur_bdt) index = 3;
        else if (id == R.id.pair_gbp_bdt) index = 4;
        else if (id == R.id.pair_usd_jpy) index = 5;
        else if (id == R.id.pair_usd_inr) index = 6;
        else return false;

        FavoritePair pair = PopularPairs.LIST.get(index);
        loadPairIntoConverter(pair.from, pair.to, null);
        return true;
    }

    private void loadPairIntoConverter(String from, String to, Double amount) {
        currentFrom = from;
        currentTo = to;
        if (amount != null) {
            currentAmount = amount;
            amountInput.setText(formatAmountForInput(amount));
        }
        suppressSpinnerEvents = true;
        setSpinnerSelection(fromSpinner, currentFrom);
        setSpinnerSelection(toSpinner, currentTo);
        suppressSpinnerEvents = false;
        updateConverterResult();
    }

    private void updateConverterResult() {
        ratesRepository.getRates(currentFrom, new RatesRepository.RatesCallback() {
            @Override
            public void onSuccess(java.util.Map<String, Double> rates) {
                Double rate = currentFrom.equals(currentTo) ? 1.0 : rates.get(currentTo);
                if (rate == null) {
                    convertedAmountView.setText("Rate unavailable");
                    rateLineView.setText("—");
                } else {
                    double converted = currentAmount * rate;
                    convertedAmountView.setText(formatNumber(converted) + " " + currentTo);
                    rateLineView.setText("1 " + currentFrom + " = " + formatNumber(rate) + " " + currentTo);
                }
                updateChart();
                renderWishlist();
            }

            @Override
            public void onError(Exception e) {
                convertedAmountView.setText("Unable to fetch rate");
                rateLineView.setText("Check your connection and try again.");
            }
        });
    }

    private void updateChart() {
        chartTitleView.setText("7-day trend: " + currentFrom + " → " + currentTo);
        historyRepository.getSeries(currentFrom, currentTo, new RatesHistoryRepository.SeriesCallback() {
            @Override
            public void onSuccess(List<RatesHistoryRepository.RatePoint> series) {
                rateChartView.setSeries(series);
                chartEmptyView.setVisibility(series.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(Exception e) {
                rateChartView.setSeries(null);
                chartEmptyView.setVisibility(View.VISIBLE);
            }
        });
    }

    private void addCurrentPairToFavorites() {
        List<FavoritePair> favorites = prefsStore.getFavorites();
        if (favorites.size() >= PreferencesStore.MAX_LIST_LENGTH) {
            showMessage(favoritesMsgView, listFullMessage("Favorites"));
            return;
        }
        showMessage(favoritesMsgView, null);
        favorites.add(new FavoritePair(currentFrom, currentTo));
        prefsStore.saveFavorites(favorites);
        renderFavorites();
    }

    private void addCurrentPairToWishlist() {
        List<WishlistItem> wishlist = prefsStore.getWishlist();
        if (wishlist.size() >= PreferencesStore.MAX_LIST_LENGTH) {
            showMessage(wishlistMsgView, listFullMessage("Wishlist"));
            return;
        }
        showMessage(wishlistMsgView, null);
        wishlist.add(new WishlistItem(currentFrom, currentTo, currentAmount));
        prefsStore.saveWishlist(wishlist);
        renderWishlist();
    }

    private String listFullMessage(String listName) {
        return String.format(Locale.US, "%s full (%d/%d) — remove one to add another.",
                listName, PreferencesStore.MAX_LIST_LENGTH, PreferencesStore.MAX_LIST_LENGTH);
    }

    private void showMessage(TextView view, String message) {
        if (message == null) {
            view.setVisibility(View.GONE);
        } else {
            view.setText(message);
            view.setVisibility(View.VISIBLE);
        }
    }

    private void renderFavorites() {
        List<FavoritePair> favorites = prefsStore.getFavorites();
        favoritesContainer.removeAllViews();
        favoritesEmptyView.setVisibility(favorites.isEmpty() ? View.VISIBLE : View.GONE);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < favorites.size(); i++) {
            FavoritePair fav = favorites.get(i);
            int index = i;
            View chip = inflater.inflate(R.layout.item_favorite_chip, favoritesContainer, false);
            TextView label = chip.findViewById(R.id.chip_label);
            label.setText(fav.from + " → " + fav.to);
            label.setOnClickListener(v -> loadPairIntoConverter(fav.from, fav.to, null));
            chip.findViewById(R.id.chip_remove_btn).setOnClickListener(v -> {
                List<FavoritePair> current = prefsStore.getFavorites();
                current.remove(index);
                prefsStore.saveFavorites(current);
                renderFavorites();
            });
            favoritesContainer.addView(chip);
        }
    }

    private void renderWishlist() {
        List<WishlistItem> wishlist = prefsStore.getWishlist();
        wishlistContainer.removeAllViews();
        wishlistEmptyView.setVisibility(wishlist.isEmpty() ? View.VISIBLE : View.GONE);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < wishlist.size(); i++) {
            WishlistItem item = wishlist.get(i);
            int index = i;
            View card = inflater.inflate(R.layout.item_wishlist_card, wishlistContainer, false);
            TextView amountLine = card.findViewById(R.id.wishlist_amount_line);
            TextView rateLine = card.findViewById(R.id.wishlist_rate_line);
            TextView totalLine = card.findViewById(R.id.wishlist_total_line);

            amountLine.setText(formatNumber(item.amount) + " " + item.from + " → " + item.to);
            rateLine.setText("Current rate: —");
            totalLine.setText("Total: —");

            card.findViewById(R.id.wishlist_remove_btn).setOnClickListener(v -> {
                List<WishlistItem> current = prefsStore.getWishlist();
                current.remove(index);
                prefsStore.saveWishlist(current);
                renderWishlist();
            });

            ratesRepository.getRates(item.from, new RatesRepository.RatesCallback() {
                @Override
                public void onSuccess(java.util.Map<String, Double> rates) {
                    Double rate = item.from.equals(item.to) ? 1.0 : rates.get(item.to);
                    if (rate == null) {
                        rateLine.setText("Current rate: —");
                        totalLine.setText("Total: —");
                    } else {
                        double total = item.amount * rate;
                        rateLine.setText("Current rate: " + formatNumber(rate));
                        totalLine.setText("Total: " + formatNumber(total) + " " + item.to);
                    }
                }

                @Override
                public void onError(Exception e) {
                    rateLine.setText("Current rate: —");
                    totalLine.setText("Total: —");
                }
            });

            wishlistContainer.addView(card);
        }
    }

    private String formatAmountForInput(double amount) {
        if (amount == Math.floor(amount)) {
            return String.valueOf((long) amount);
        }
        return String.valueOf(amount);
    }

    private String formatNumber(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return "—";
        return String.format(Locale.US, "%,.6f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}

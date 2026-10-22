package com.gathin.tvhome;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The B3 weather: now, today's high and low, and the next four hours, from MET Norway's locationforecast
 * (api.met.no, the yr.no data: free, no key, CC BY 4.0, credited in the city picker). City search goes to
 * OpenStreetMap's Nominatim. Open-Meteo was the first choice, but its servers do not answer from this TV's
 * network. The only network the launcher does: one small JSON request every 30 minutes while Home
 * or the screensaver is on screen, and the city search when the owner asks for it. The last answer is kept in SharedPreferences,
 * so Home shows it straight away after a reboot and when the network is down.
 *
 * The city is the owner's choice (OK on the weather opens the picker); config.xml weather_city and
 * weather_latitude / weather_longitude give the first one.
 */
final class Weather {

    private static final String PREFS = "weather";
    private static final long REFRESH_MS = 30 * 60_000L;
    /** A cached answer older than this is not shown as current. */
    private static final long STALE_MS = 6 * 3600_000L;
    private static final int ACCENT = 0xFF6B97FF;

    /** The picker's quick choices: the owner's region first. Name, latitude, longitude. */
    private static final Object[][] PRESETS = {
            {"Ростов-на-Дону", 47.2357, 39.7015},
            {"Краснодар", 45.0355, 38.9753},
            {"Москва", 55.7558, 37.6173},
            {"Санкт-Петербург", 59.9386, 30.3141},
            {"Сочи", 43.5855, 39.7231},
            {"Таганрог", 47.2362, 38.8969},
    };

    private final Context context;
    /** Null on the screensaver, which shows the weather but has no city picker. */
    private final Activity activity;
    private final View block;
    private final TextView condition, city, temp;
    private final ImageView icon;
    private final LinearLayout hours;
    private final SharedPreferences prefs;
    private final float density;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private boolean running;
    private boolean fetching;
    private boolean destroyed;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            refreshIfDue();
            main.postDelayed(this, 60_000L);
        }
    };

    /** context is Home (OK on the block opens the city picker) or the screensaver (shown only). */
    Weather(Context context, View block) {
        this.context = context;
        this.activity = context instanceof Activity ? (Activity) context : null;
        this.block = block;
        condition = block.findViewById(R.id.weatherCondition);
        city = block.findViewById(R.id.weatherCity);
        temp = block.findViewById(R.id.weatherTemp);
        icon = block.findViewById(R.id.weatherIcon);
        hours = block.findViewById(R.id.weatherHours);
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        density = context.getResources().getDisplayMetrics().density;
        if (!prefs.contains("city")) {
            prefs.edit()
                    .putString("city", context.getString(R.string.weather_city))
                    .putFloat("lat", Float.parseFloat(context.getString(R.string.weather_latitude)))
                    .putFloat("lon", Float.parseFloat(context.getString(R.string.weather_longitude)))
                    .apply();
        }
        if (activity != null) block.setOnClickListener(v -> showCityPicker());
        render();
    }

    /** While Home or the screensaver is on screen: shows the cached answer and refreshes it when it is 30 minutes old. */
    void start() {
        running = true;
        render();
        main.removeCallbacks(tick);
        main.post(tick);
    }

    void stop() {
        running = false;
        main.removeCallbacks(tick);
    }

    /** The network came back: try again now if the last answer is due or missing. */
    void onNetworkAvailable() {
        if (running) refreshIfDue();
    }

    void destroy() {
        destroyed = true;
        stop();
        io.shutdownNow();
    }

    private void refreshIfDue() {
        if (fetching) return;
        long at = prefs.getLong("fetched_at", 0);
        long failedAt = prefs.getLong("failed_at", 0);
        long now = System.currentTimeMillis();
        boolean due = now - at >= REFRESH_MS || !prefs.getString("json", "").startsWith("{");
        // After a failure wait two minutes before the next try, so a TV without internet does not keep asking.
        if (due && now - failedAt >= 120_000L) fetch();
    }

    private void fetch() {
        fetching = true;
        final float lat = prefs.getFloat("lat", 47.2357f), lon = prefs.getFloat("lon", 39.7015f);
        io.execute(() -> {
            String json = null;
            try {
                // MET asks for at most 4 decimals in the coordinates and an identifying User-Agent.
                json = get(String.format(Locale.US,
                        "https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=%.4f&lon=%.4f", lat, lon));
                new JSONObject(json).getJSONObject("properties").getJSONArray("timeseries").getJSONObject(0);
            } catch (Exception e) {
                android.util.Log.w("TvHome.Weather", "forecast failed: " + e);
                json = null;
            }
            final String answer = json;
            main.post(() -> {
                fetching = false;
                SharedPreferences.Editor ed = prefs.edit();
                if (answer != null) {
                    ed.putString("json", answer).putLong("fetched_at", System.currentTimeMillis()).remove("failed_at");
                } else {
                    ed.putLong("failed_at", System.currentTimeMillis());
                }
                ed.apply();
                if (!destroyed) render();
            });
        });
    }

    private static String get(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(8000);
        c.setReadTimeout(8000);
        // api.met.no and Nominatim both require a User-Agent that says who is asking.
        c.setRequestProperty("User-Agent", "CleanYourTV-launcher/1.0 github.com/Gathin23/cleanyourtv");
        try (InputStream in = c.getInputStream()) {
            if (c.getResponseCode() != 200) throw new IllegalStateException("HTTP " + c.getResponseCode());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            for (int n; (n = in.read(buf)) > 0; ) {
                out.write(buf, 0, n);
                if (out.size() > 1024 * 1024) throw new IllegalStateException("answer too large");
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            c.disconnect();
        }
    }

    // ---- showing it ----------------------------------------------------------------------------------

    private void render() {
        String cityName = prefs.getString("city", "");
        String json = prefs.getString("json", "");
        long at = prefs.getLong("fetched_at", 0);
        hours.removeAllViews();
        JSONObject data = null;
        if (json.startsWith("{") && System.currentTimeMillis() - at < STALE_MS) {
            try {
                data = new JSONObject(json);
            } catch (Exception ignored) {
            }
        }
        if (data == null) {
            temp.setText("");
            icon.setImageDrawable(null);
            icon.setVisibility(View.GONE);
            condition.setText(fetching || prefs.getLong("failed_at", 0) == 0
                    ? R.string.weather_loading : R.string.weather_unavailable);
            city.setText(cityName);
            return;
        }
        try {
            JSONArray series = data.getJSONObject("properties").getJSONArray("timeseries");
            long now = System.currentTimeMillis();
            // The hour now: the last step that has started (the series is hourly for the first two days).
            int cur = 0;
            for (int i = 0; i < series.length(); i++) {
                if (time(series.getJSONObject(i)) <= now) cur = i; else break;
            }
            JSONObject step = series.getJSONObject(cur).getJSONObject("data");
            String symbol = symbol(step);
            temp.setText(degrees(step.getJSONObject("instant").getJSONObject("details").getDouble("air_temperature")));
            icon.setImageResource(iconFor(symbol));
            icon.setVisibility(View.VISIBLE);
            condition.setText(conditionFor(symbol));
            // Today's high and low from the hourly steps of the TV's date.
            java.time.LocalDate today = java.time.LocalDate.now();
            double hi = -999, lo = 999;
            for (int i = 0; i < series.length(); i++) {
                JSONObject s = series.getJSONObject(i);
                java.time.LocalDate d = java.time.Instant.ofEpochMilli(time(s)).atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                if (d.isAfter(today)) break;
                if (d.isBefore(today)) continue;
                double t = s.getJSONObject("data").getJSONObject("instant").getJSONObject("details").getDouble("air_temperature");
                hi = Math.max(hi, t);
                lo = Math.min(lo, t);
            }
            city.setText(hi > -999 ? context.getString(R.string.weather_city_line, cityName, degrees(hi), degrees(lo)) : cityName);
            block.setContentDescription(condition.getText() + ", " + temp.getText() + ", " + cityName);
            addHours(series, cur);
        } catch (Exception e) {
            condition.setText(R.string.weather_unavailable);
            city.setText(cityName);
        }
    }

    private static long time(JSONObject step) throws Exception {
        return java.time.Instant.parse(step.getString("time")).toEpochMilli();
    }

    /** The step's weather for its next hour, or the next six hours further out in the series. */
    private static String symbol(JSONObject data) {
        JSONObject next = data.optJSONObject("next_1_hours");
        if (next == null) next = data.optJSONObject("next_6_hours");
        return next == null ? "cloudy" : next.optJSONObject("summary") == null ? "cloudy"
                : next.optJSONObject("summary").optString("symbol_code", "cloudy");
    }

    /** The four hours after the current one, in the TV's time. */
    private void addHours(JSONArray series, int cur) throws Exception {
        int added = 0;
        java.time.format.DateTimeFormatter hh = java.time.format.DateTimeFormatter.ofPattern("HH:00");
        for (int i = cur + 1; i < series.length() && added < 4; i++) {
            JSONObject s = series.getJSONObject(i);
            JSONObject data = s.getJSONObject("data");
            String sym = symbol(data);
            double rain = 0;
            JSONObject next = data.optJSONObject("next_1_hours");
            if (next != null && next.optJSONObject("details") != null) {
                rain = next.optJSONObject("details").optDouble("precipitation_amount", 0);
            }
            boolean wet = isWet(sym) || rain >= 0.2;
            String at = java.time.Instant.ofEpochMilli(time(s)).atZone(java.time.ZoneId.systemDefault()).format(hh);
            hours.addView(chip(at, degrees(data.getJSONObject("instant").getJSONObject("details").getDouble("air_temperature")),
                    wet ? iconFor(isWet(sym) ? sym : "rain") : 0), chipParams(added));
            added++;
        }
    }

    private LinearLayout.LayoutParams chipParams(int index) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(25.5f));
        if (index > 0) lp.setMarginStart(dp(4.5f));
        return lp;
    }

    /** "20:00 +10°" on a faint chip; an hour with rain or snow gets the blue chip and its icon. */
    private View chip(String time, String degrees, int iconRes) {
        LinearLayout c = new LinearLayout(context);
        c.setOrientation(LinearLayout.HORIZONTAL);
        c.setGravity(Gravity.CENTER_VERTICAL);
        c.setPadding(dp(9), 0, dp(9), 0);
        c.setBackgroundResource(iconRes != 0 ? R.drawable.weather_chip_wet_bg : R.drawable.weather_chip_bg);
        if (iconRes != 0) {
            ImageView i = new ImageView(context);
            i.setImageResource(iconRes);
            i.setColorFilter(ACCENT);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(12), dp(12));
            lp.setMarginEnd(dp(6));
            c.addView(i, lp);
        }
        TextView t = text(time, 10.5f, iconRes != 0 ? 0xFFA9C2FF : 0xFFA3A3A3, R.font.outfit_regular);
        c.addView(t);
        TextView d = text(degrees, 11.25f, 0xFFF5F5F5, R.font.outfit_medium);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMarginStart(dp(6));
        c.addView(d, lp);
        return c;
    }

    private TextView text(String s, float sp, int color, int font) {
        TextView t = new TextView(context);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setIncludeFontPadding(false);
        t.setSingleLine(true);
        try {
            t.setTypeface(context.getResources().getFont(font));
        } catch (RuntimeException ignored) {
        }
        return t;
    }

    private int dp(float v) {
        return Math.round(v * density);
    }

    /** "+11°", "0°", "−3°" (a real minus sign). */
    static String degrees(double t) {
        long r = Math.round(t);
        if (r == 0) return "0°";
        return (r > 0 ? "+" : "−") + Math.abs(r) + "°";
    }

    /** MET symbol codes: "clearsky_day", "partlycloudy_night", "lightrainshowers_day", "heavysnow", ... */
    static boolean isWet(String symbol) {
        return symbol.contains("rain") || symbol.contains("snow") || symbol.contains("sleet") || symbol.contains("thunder");
    }

    static int iconFor(String symbol) {
        boolean night = symbol.endsWith("_night");
        if (symbol.contains("thunder")) return R.drawable.wx_thunder;
        if (symbol.contains("snow") || symbol.contains("sleet")) return R.drawable.wx_snow;
        if (symbol.contains("rain")) return R.drawable.wx_rain;
        if (symbol.startsWith("fog")) return R.drawable.wx_fog;
        if (symbol.startsWith("clearsky")) return night ? R.drawable.wx_clear_night : R.drawable.wx_clear_day;
        if (symbol.startsWith("fair") || symbol.startsWith("partlycloudy")) return R.drawable.wx_partly;
        return R.drawable.wx_cloudy;
    }

    static int conditionFor(String symbol) {
        String s = symbol.replace("_day", "").replace("_night", "").replace("_polartwilight", "");
        if (s.contains("thunder")) return R.string.wmo_thunder;
        if (s.contains("sleet")) return R.string.wmo_sleet;
        if (s.contains("snowshowers")) return R.string.wmo_snow_showers;
        if (s.contains("snow")) return s.startsWith("light") ? R.string.wmo_snow_light
                : s.startsWith("heavy") ? R.string.wmo_snow_heavy : R.string.wmo_snow;
        if (s.contains("rainshowers")) return s.startsWith("light") ? R.string.wmo_rain_light : R.string.wmo_showers;
        if (s.contains("rain")) return s.startsWith("light") ? R.string.wmo_rain_light
                : s.startsWith("heavy") ? R.string.wmo_rain_heavy : R.string.wmo_rain;
        switch (s) {
            case "clearsky": return R.string.wmo_clear;
            case "fair": return R.string.wmo_mainly_clear;
            case "partlycloudy": return R.string.wmo_partly;
            case "fog": return R.string.wmo_fog;
            default: return R.string.wmo_overcast;
        }
    }

    // ---- choosing the city ---------------------------------------------------------------------------

    private Dialog picker;

    private void showCityPicker() {
        if (picker != null && picker.isShowing()) return;
        picker = new Dialog(activity);
        picker.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout panel = panel();
        panel.addView(title(context.getString(R.string.weather_city_title)));
        String current = prefs.getString("city", "");
        View first = null;
        for (Object[] p : PRESETS) {
            String name = (String) p[0];
            View row = row(name, name.equals(current));
            row.setOnClickListener(v -> choose(name, ((Double) p[1]).floatValue(), ((Double) p[2]).floatValue()));
            panel.addView(row);
            if (first == null || name.equals(current)) first = row;
        }
        View other = row(context.getString(R.string.weather_city_other), false);
        other.setOnClickListener(v -> showSearch());
        panel.addView(other);
        panel.addView(note(context.getString(R.string.weather_credit)));
        show(picker, panel, first);
    }

    private void choose(String name, float lat, float lon) {
        prefs.edit().putString("city", name).putFloat("lat", lat).putFloat("lon", lon)
                .remove("json").remove("fetched_at").remove("failed_at").apply();
        if (picker != null) picker.dismiss();
        render();
        fetch();
    }

    /** Free text search through Open-Meteo's geocoding, in Russian, eight results at most. */
    private void showSearch() {
        if (picker != null) picker.dismiss();
        picker = new Dialog(activity);
        picker.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout panel = panel();
        panel.addView(title(context.getString(R.string.weather_city_other)));
        EditText input = new EditText(context);
        input.setHint(R.string.weather_search_hint);
        input.setHintTextColor(0xFF737373);
        input.setTextColor(0xFFF5F5F5);
        input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        input.setBackgroundResource(R.drawable.nav_item_bg);
        input.setPadding(dp(12), dp(10), dp(12), dp(10));
        panel.addView(input, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout results = new LinearLayout(context);
        results.setOrientation(LinearLayout.VERTICAL);
        panel.addView(results);
        panel.addView(note(context.getString(R.string.weather_search_credit)));
        Runnable search = () -> search(input.getText().toString().trim(), results);
        input.setOnEditorActionListener((v, action, event) -> {
            boolean go = action == EditorInfo.IME_ACTION_SEARCH || action == EditorInfo.IME_ACTION_DONE
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN);
            if (go) search.run();
            return go;
        });
        show(picker, panel, input);
    }

    private void search(String query, LinearLayout results) {
        results.removeAllViews();
        if (query.isEmpty()) return;
        results.addView(note(context.getString(R.string.weather_searching)));
        io.execute(() -> {
            List<Object[]> found = new ArrayList<>();
            boolean failed = false;
            try {
                // Nominatim (OpenStreetMap): towns and cities only, names in the TV's language.
                JSONArray list = new JSONArray(get("https://nominatim.openstreetmap.org/search?format=jsonv2&limit=8"
                        + "&featureType=settlement&addressdetails=1&accept-language="
                        + URLEncoder.encode(Locale.getDefault().getLanguage(), "UTF-8")
                        + "&q=" + URLEncoder.encode(query, "UTF-8")));
                for (int i = 0; i < list.length(); i++) {
                    JSONObject r = list.getJSONObject(i);
                    JSONObject a = r.optJSONObject("address");
                    String place = a == null ? "" : a.optString("state", "");
                    String country = a == null ? "" : a.optString("country", "");
                    String detail = place.isEmpty() ? country : country.isEmpty() ? place : place + ", " + country;
                    String name = r.optString("name", "");
                    if (name.isEmpty()) continue;
                    found.add(new Object[]{name, detail, Double.parseDouble(r.getString("lat")), Double.parseDouble(r.getString("lon"))});
                }
            } catch (Exception e) {
                android.util.Log.w("TvHome.Weather", "city search failed: " + e);
                failed = true;
            }
            final boolean error = failed;
            main.post(() -> {
                if (picker == null || !picker.isShowing()) return;
                results.removeAllViews();
                if (error) results.addView(note(context.getString(R.string.weather_search_failed)));
                else if (found.isEmpty()) results.addView(note(context.getString(R.string.weather_search_none)));
                View firstRow = null;
                for (Object[] f : found) {
                    String name = (String) f[0];
                    View row = row(f[1].toString().isEmpty() ? name : name + "  ·  " + f[1], false);
                    row.setOnClickListener(v -> choose(name, ((Double) f[2]).floatValue(), ((Double) f[3]).floatValue()));
                    results.addView(row);
                    if (firstRow == null) firstRow = row;
                }
                if (firstRow != null) firstRow.requestFocus();
            });
        });
    }

    private LinearLayout panel() {
        LinearLayout panel = new LinearLayout(context);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(15), dp(15), dp(15), dp(15));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF1E1E1E);
        bg.setCornerRadius(dp(16.5f));
        bg.setStroke(Math.max(1, dp(0.75f)), 0x1AF5F5F5);
        panel.setBackground(bg);
        return panel;
    }

    private TextView title(String s) {
        TextView t = text(s, 12f, 0xFFA3A3A3, R.font.outfit_medium);
        t.setPadding(dp(10.5f), dp(3), dp(10.5f), dp(9));
        return t;
    }

    private TextView note(String s) {
        TextView t = text(s, 12f, 0xFFA3A3A3, R.font.outfit_regular);
        t.setPadding(dp(10.5f), dp(10), dp(10.5f), dp(4));
        return t;
    }

    /** A picker row in the header's style: #333 when focused, the current city on 5% white. */
    private View row(String s, boolean current) {
        TextView t = text(s, 13.5f, 0xFFF5F5F5, current ? R.font.outfit_semibold : R.font.outfit_regular);
        t.setBackgroundResource(R.drawable.nav_item_bg);
        t.setActivated(current);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setPadding(dp(10.5f), 0, dp(10.5f), 0);
        t.setFocusable(true);
        t.setClickable(true);
        t.setMinHeight(dp(36));
        t.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(36)));
        return t;
    }

    private void show(Dialog d, LinearLayout panel, View focus) {
        ScrollView scroll = new ScrollView(context);
        scroll.addView(panel);
        d.setContentView(scroll);
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.setLayout(dp(300), ViewGroup.LayoutParams.WRAP_CONTENT);
            w.setGravity(Gravity.TOP | Gravity.END);
            android.view.WindowManager.LayoutParams lp = w.getAttributes();
            lp.x = dp(54);
            lp.y = dp(96);
            lp.dimAmount = 0.5f;
            w.setAttributes(lp);
            w.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }
        d.show();
        if (focus != null) focus.requestFocus();
    }
}

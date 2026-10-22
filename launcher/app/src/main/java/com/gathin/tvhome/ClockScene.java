package com.gathin.tvhome;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import java.util.Locale;
import java.util.Random;

/**
 * The screensaver's scene (layout dream_clock, the B3 mockup): Home's clock, date and weather in one row
 * centred on the screen. Every minute the row slides a few dp to a new spot around the centre, too slowly
 * to notice, so nothing stays on the same pixels for hours.
 * Shared by ClockDream (the system screensaver) and ClockActivity (Home left idle).
 */
final class ClockScene {

    /** How far the row may wander from the centre, each way. */
    private static final float NUDGE_DP = 8;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private final Context context;
    private final View content;
    private final TextView date;
    private final Weather weather;
    private final float density;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            updateDate();
            nudge();
            handler.postDelayed(this, untilNextMinute());
        }
    };

    /** root is the inflated dream_clock layout. */
    ClockScene(Context context, View root) {
        this.context = context;
        content = root.findViewById(R.id.dreamContent);
        date = root.findViewById(R.id.dreamDate);
        weather = new Weather(context, root.findViewById(R.id.weather));
        density = context.getResources().getDisplayMetrics().density;
    }

    void start() {
        updateDate();
        weather.start();
        content.setAlpha(0f);
        content.animate().alpha(1f).setDuration(1200).start();
        handler.removeCallbacks(tick);
        handler.postDelayed(tick, untilNextMinute());
    }

    void stop() {
        handler.removeCallbacks(tick);
        weather.stop();
    }

    void destroy() {
        stop();
        weather.destroy();
    }

    /** "Четверг, 22 октября", as on Home. */
    private void updateDate() {
        String day = new java.text.SimpleDateFormat(context.getString(R.string.date_format_b3), Locale.getDefault())
                .format(new java.util.Date());
        if (!day.isEmpty()) day = day.substring(0, 1).toUpperCase(Locale.getDefault()) + day.substring(1);
        if (!day.contentEquals(date.getText())) date.setText(day);
    }

    private void nudge() {
        float max = NUDGE_DP * density;
        content.animate()
                .translationX(max * (2 * random.nextFloat() - 1))
                .translationY(max * (2 * random.nextFloat() - 1))
                .setDuration(4000).start();
    }

    private static long untilNextMinute() {
        return 60_000L - System.currentTimeMillis() % 60_000L + 50;
    }
}

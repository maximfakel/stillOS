package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * B3 rows: "Favourites" over the dock row and "All apps" over the first grid row, in the section-title style
 * of the mockup (16px Outfit Medium, #A3A3A3, 40px above the tiles), and the gaps between the rows.
 * The dock title sits in the grid's top padding, so it scrolls away with the rows like the other title.
 */
public class SectionDecoration extends RecyclerView.ItemDecoration {

    /** Title top to tile top: 40px in the mockup. */
    static final float TITLE_ROOM_DP = 30;
    /** Dock names' bottom to the "All apps" title: 40px. */
    static final float SECTION_GAP_DP = 30;
    /** Between grid rows: 24px. */
    static final float ROW_GAP_DP = 18;

    private final int dockSize;
    private final float density;
    private final String favourites, allApps;
    private final Paint title = new Paint(Paint.ANTI_ALIAS_FLAG);

    SectionDecoration(Context ctx, int dockSize, float density) {
        this.dockSize = dockSize;
        this.density = density;
        favourites = ctx.getString(R.string.section_favourites);
        allApps = ctx.getString(R.string.section_all_apps);
        title.setColor(0xFFA3A3A3);
        title.setTextSize(12f * ctx.getResources().getDisplayMetrics().scaledDensity);
        Typeface medium;
        try {
            medium = ctx.getResources().getFont(R.font.outfit_medium);
        } catch (RuntimeException e) {
            medium = Typeface.DEFAULT;
        }
        title.setTypeface(medium);
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent,
                               @NonNull RecyclerView.State state) {
        int pos = parent.getChildAdapterPosition(view);
        if (pos < 0 || pos < dockSize) return;
        // The first grid row: the gap after the dock names, then the "All apps" title above the tiles.
        outRect.top = Math.round((pos < 2 * dockSize ? SECTION_GAP_DP + TITLE_ROOM_DP : ROW_GAP_DP) * density);
    }

    @Override
    public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        boolean dockDrawn = false, gridDrawn = false;
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            int pos = parent.getChildAdapterPosition(child);
            if (pos < 0) continue;
            boolean dock = pos < dockSize;
            if (dock ? dockDrawn : (gridDrawn || pos >= 2 * dockSize)) continue;
            // The row's first column gives the left edge: the card is centred in its cell.
            if (pos % dockSize != 0) continue;
            View card = child.findViewById(R.id.card);
            float left = child.getLeft() + (card != null ? card.getLeft() : 0);
            float top = child.getTop() + child.getTranslationY();
            // Baseline so the 20px line box starts 40px above the tile, as in the mockup.
            Paint.FontMetrics fm = title.getFontMetrics();
            float lineTop = top - TITLE_ROOM_DP * density;
            float baseline = lineTop + (15f * density - (fm.descent + fm.ascent)) / 2f;
            c.drawText(dock ? favourites : allApps, left, baseline, title);
            if (dock) dockDrawn = true; else gridDrawn = true;
        }
    }
}

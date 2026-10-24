package com.gathin.tvhome;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** AppAdapter.tileWidthPx in the B3 layout: the columns across decide the tile, the height never shrinks it. */
public class TileFitTest {

    private static final float D = 2f;              // 1080p TV: 2 px per dp
    private static final int INNER = 1696;          // grid width minus padding at 1920 px
    private static final int GRID_H = 846;          // grid height under the top bar

    @Test
    public void theCellMinusTheGapIsTheTile() {
        assertEquals(Math.round(INNER / 4 - AppAdapter.GAP_DP * D), AppAdapter.tileWidthPx(INNER, 4, D));
        assertEquals(Math.round(INNER / 5 - AppAdapter.GAP_DP * D), AppAdapter.tileWidthPx(INNER, 5, D));
    }

    @Test
    public void theMockupTileOnA960dpTv() {
        // 960dp wide, 45dp padding each side and 4 columns: the 199.5 x 112.5dp tile of the mockup (266 x 150 px).
        int inner = 960 - 2 * 45;
        int w = AppAdapter.tileWidthPx(inner, 4, 1f);
        assertTrue("width " + w, Math.abs(w - AppAdapter.DESIGN_W_DP) <= 1);
        assertTrue("height " + AppAdapter.tileHeightPx(w), Math.abs(AppAdapter.tileHeightPx(w) - AppAdapter.DESIGN_H_DP) <= 1);
    }

    @Test
    public void heightRowsAndTitleDoNotShrinkTheTiles() {
        int widthOnly = AppAdapter.tileWidthPx(INNER, 4, D);
        for (int rows = 0; rows <= 3; rows++) {
            for (boolean title : new boolean[]{true, false}) {
                for (boolean dockRow : new boolean[]{true, false}) {
                    assertEquals(widthOnly, AppAdapter.tileWidthPx(INNER, GRID_H, 4, rows, title, dockRow, D));
                    assertEquals(widthOnly, AppAdapter.tileWidthPx(INNER, 0, 4, rows, title, dockRow, D));
                }
            }
        }
    }

    @Test
    public void tinyGridsKeepAUsableTile() {
        assertEquals(Math.round(80 * D), AppAdapter.tileWidthPx(200, 6, D));
        assertEquals(Math.round(AppAdapter.DESIGN_W_DP * D), AppAdapter.tileWidthPx(0, 4, D));   // not laid out yet
    }
}

package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * The B3 focus plate: one flat #333 rounded rect under the focused tile and its name, the way the
 * Fluid Functionalism reference highlights the item in focus. One instance sits under the grid and is moved
 * with spring animations between tiles, so the highlight travels instead of blinking from tile to tile.
 * (The class keeps its old name: the springs and the layout know it as the glass.)
 */
public class GlassView extends View {

    static final int PLATE = 0xFF333333;

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float radius;

    public GlassView(Context context, AttributeSet attrs) {
        super(context, attrs);
        radius = 15f * getResources().getDisplayMetrics().density;   // 20px in the mockup
        fill.setColor(PLATE);
        // Drawn once into a texture: the springs only change translation.
        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    /** A festive accent tints the plate a little; 0 restores the plain plate. */
    void setAccent(int argb) {
        if (argb == 0) {
            fill.setColor(PLATE);
        } else {
            int r = (0x33 * 3 + ((argb >> 16) & 0xFF)) / 4, g = (0x33 * 3 + ((argb >> 8) & 0xFF)) / 4,
                    b = (0x33 * 3 + (argb & 0xFF)) / 4;
            fill.setColor(0xFF000000 | (r << 16) | (g << 8) | b);
        }
        invalidate();
    }

    void setRadius(float px) {
        radius = px;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawRoundRect(0, 0, getWidth(), getHeight(), radius, radius, fill);
    }
}

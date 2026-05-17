/*
 * This file is part of the Lime Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.themes.aurora.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.aurora.AuroraPalette;
import meteordevelopment.meteorclient.gui.themes.aurora.AuroraWidget;
import meteordevelopment.meteorclient.gui.widgets.WWidget;

/**
 * Horizontal breadcrumb. Renders segments separated by " / ". All but the last segment use
 * the muted "textSecondary" color; the last (current page) uses "textPrimary". Segments are
 * passive visual elements for now — no navigation handler.
 */
public class WAuroraBreadcrumb extends WWidget implements AuroraWidget {
    private static final String SEP = " / ";

    private String[] segments;

    public WAuroraBreadcrumb(String... segments) {
        this.segments = segments == null ? new String[0] : segments;
    }

    public void setSegments(String... segments) {
        this.segments = segments == null ? new String[0] : segments;
        invalidate();
    }

    public String[] getSegments() {
        return segments;
    }

    @Override
    protected void onCalculateSize() {
        double w = 0;
        for (int i = 0; i < segments.length; i++) {
            w += theme.textWidth(segments[i]);
            if (i < segments.length - 1) w += theme.textWidth(SEP);
        }
        width = w;
        height = theme.textHeight();
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        AuroraPalette p = theme().palette();
        double cx = x;
        double y = this.y;
        for (int i = 0; i < segments.length; i++) {
            boolean last = i == segments.length - 1;
            renderer.text(segments[i], cx, y, last ? p.textPrimary() : p.textSecondary(), false);
            cx += theme.textWidth(segments[i]);
            if (!last) {
                renderer.text(SEP, cx, y, p.textMuted(), false);
                cx += theme.textWidth(SEP);
            }
        }
    }
}

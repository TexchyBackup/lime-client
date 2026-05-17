/*
 * This file is part of the Lime Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.themes.aurora.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.aurora.AuroraPalette;
import meteordevelopment.meteorclient.gui.themes.aurora.AuroraWidget;
import meteordevelopment.meteorclient.gui.widgets.WWidget;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Aurora status bar — bottom-of-window strip. Hosts N "label: value" stat items on the left
 * (label in textMuted, value in textAccent) and an optional status string on the right (textMuted).
 * Stat values are pulled via {@link Supplier} each render frame.
 */
public class WAuroraStatusBar extends WWidget implements AuroraWidget {
    private static final double HORIZONTAL_PAD = 10;
    private static final double STAT_SPACING = 16;
    private static final double SEP = 4; // ":" padding

    private final List<Stat> stats = new ArrayList<>();
    private String status;

    public WAuroraStatusBar() {
    }

    public void addStat(String label, Supplier<String> value) {
        stats.add(new Stat(label, value));
        invalidate();
    }

    public void setStatus(String text) {
        this.status = text;
    }

    @Override
    protected void onCalculateSize() {
        height = theme.textHeight() + theme.scale(8);
        // Width is driven by the caller (expandX usually); fall back to a minimum.
        if (width == 0) width = theme.scale(200);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        AuroraPalette p = theme().palette();

        // Top divider
        renderer.quad(x, y, width, 1, p.divider());

        double textH = theme.textHeight();
        double ty = y + (height - textH) / 2;
        double cx = x + theme.scale(HORIZONTAL_PAD);

        for (int i = 0; i < stats.size(); i++) {
            Stat s = stats.get(i);
            String labelStr = s.label + ":";
            renderer.text(labelStr, cx, ty, p.textMuted(), false);
            cx += theme.textWidth(labelStr) + theme.scale(SEP);

            String val = s.value.get();
            if (val == null) val = "";
            renderer.text(val, cx, ty, p.textAccent(), false);
            cx += theme.textWidth(val);

            if (i < stats.size() - 1) cx += theme.scale(STAT_SPACING);
        }

        if (status != null && !status.isEmpty()) {
            double sw = theme.textWidth(status);
            renderer.text(status, x + width - sw - theme.scale(HORIZONTAL_PAD), ty, p.textMuted(), false);
        }
    }

    private record Stat(String label, Supplier<String> value) {}
}

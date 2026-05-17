/*
 * This file is part of the Lime Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.themes.aurora.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.renderer.anim.Animated;
import meteordevelopment.meteorclient.gui.renderer.anim.Easing;
import meteordevelopment.meteorclient.gui.themes.aurora.AuroraGuiTheme;
import meteordevelopment.meteorclient.gui.themes.aurora.AuroraPalette;
import meteordevelopment.meteorclient.gui.themes.aurora.AuroraWidget;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.utils.render.color.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * Aurora sidebar: a vertical container with stacked nav items at the top and a status block
 * (active / idle counts) at the bottom, separated by a divider. The active item is highlighted
 * with an accent-tinted row gradient and a 2px solid accent left border; the highlight position
 * tweens smoothly when {@link #setActive(int)} is invoked.
 */
public class WAuroraSidebar extends WContainer implements AuroraWidget {
    private static final double SIDEBAR_WIDTH = 110;
    private static final double ITEM_HEIGHT = 22;
    private static final double ITEM_SPACING = 2;
    private static final double DOT_SIZE = 4;
    private static final double STATUS_HEIGHT = 36;
    private static final double DIVIDER_PAD = 6;

    private final List<Item> items = new ArrayList<>();
    private int activeIndex = -1;
    /** Animated y-offset of the active stripe relative to {@link #y}. */
    private final Animated stripeY = new Animated(0, 0.22, Easing.OUT_CUBIC);

    private int activeStat = 0;
    private int idleStat = 0;

    public WAuroraSidebar() {
    }

    public void addItem(String label, Runnable onClick) {
        items.add(new Item(label, onClick));
        if (activeIndex == -1) activeIndex = 0;
        // stripe target is seeded lazily in onRender once theme is available
        invalidate();
    }

    public void setActive(int index) {
        if (index < 0 || index >= items.size() || index == activeIndex) return;
        activeIndex = index;
        // animated; will retarget on next render
    }

    public int getActive() {
        return activeIndex;
    }

    public void setStats(int active, int idle) {
        this.activeStat = active;
        this.idleStat = idle;
    }

    @Override
    protected void onCalculateSize() {
        width = theme.scale(SIDEBAR_WIDTH);
        // Items + spacing + divider + status
        double itemsH = items.size() * theme.scale(ITEM_HEIGHT) + Math.max(0, items.size() - 1) * theme.scale(ITEM_SPACING);
        height = itemsH + theme.scale(DIVIDER_PAD * 2 + 1 + STATUS_HEIGHT);
    }

    @Override
    protected void onCalculateWidgetPositions() {
        // No child widgets - layout is custom in onRender.
    }

    private double itemY(int i) {
        return y + i * (theme.scale(ITEM_HEIGHT) + theme.scale(ITEM_SPACING));
    }

    private double stripeTargetFor(int i) {
        return i * (theme.scale(ITEM_HEIGHT) + theme.scale(ITEM_SPACING));
    }

    @Override
    public boolean onMouseClicked(net.minecraft.client.input.MouseButtonEvent click, boolean doubled) {
        if (!mouseOver) return false;
        double mx = click.x();
        double my = click.y();
        double iH = theme.scale(ITEM_HEIGHT);
        for (int i = 0; i < items.size(); i++) {
            double iy = itemY(i);
            if (mx >= x && mx <= x + width && my >= iy && my <= iy + iH) {
                setActive(i);
                Runnable r = items.get(i).onClick;
                if (r != null) r.run();
                return true;
            }
        }
        return false;
    }

    @Override
    public void onMouseMoved(double mouseX, double mouseY, double lastMouseX, double lastMouseY) {
        // tracked via this.mouseOver from WWidget; nothing extra needed.
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        AuroraGuiTheme t = theme();
        AuroraPalette p = t.palette();

        stripeY.setDuration(t.animDuration(0.22));
        if (activeIndex >= 0) stripeY.set(stripeTargetFor(activeIndex));
        stripeY.update(delta);

        double iH = theme.scale(ITEM_HEIGHT);

        // Active stripe row background + left border
        if (activeIndex >= 0) {
            double sy = y + stripeY.get();
            // accent → transparent gradient row
            Color start = new Color(p.accent().r, p.accent().g, p.accent().b, 31); // ~0.12
            Color end = new Color(p.accent().r, p.accent().g, p.accent().b, 0);
            renderer.gradientLinear(x, sy, width, iH, start, end, 0);
            // 2px solid accent left border
            renderer.quad(x, sy, theme.scale(2), iH, p.accent());
        }

        // Nav items: dot + label
        double labelTextH = t.textHeight();
        for (int i = 0; i < items.size(); i++) {
            Item it = items.get(i);
            double iy = itemY(i);

            boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= iy && mouseY <= iy + iH;
            // Hover overlay (non-active rows)
            if (hovered && i != activeIndex) {
                renderer.quad(x, iy, width, iH, p.hover());
            }

            // Dot
            double dot = theme.scale(DOT_SIZE);
            double dotX = x + theme.scale(10);
            double dotY = iy + iH / 2 - dot / 2;
            renderer.roundedRect(dotX, dotY, dot, dot, dot / 2, p.accent());

            // Label
            Color labelColor = (i == activeIndex) ? p.textPrimary() : p.textSecondary();
            double labelX = dotX + dot + theme.scale(8);
            double labelY = iy + iH / 2 - labelTextH / 2;
            renderer.text(it.label, labelX, labelY, labelColor, false);
        }

        // Divider between nav items and status
        double itemsBottom = items.isEmpty()
            ? y
            : itemY(items.size() - 1) + iH;
        double divY = itemsBottom + theme.scale(DIVIDER_PAD);
        renderer.quad(x + theme.scale(8), divY, width - theme.scale(16), 1, p.divider());

        // Status block
        double statusTop = divY + 1 + theme.scale(DIVIDER_PAD);
        double lineH = labelTextH + theme.scale(3);
        // ● N active (accent)
        String activeLine = "● " + activeStat + " active";
        renderer.text(activeLine, x + theme.scale(10), statusTop, p.textAccent(), false);
        // ○ N idle (muted)
        String idleLine = "○ " + idleStat + " idle";
        renderer.text(idleLine, x + theme.scale(10), statusTop + lineH, p.textMuted(), false);
    }

    private record Item(String label, Runnable onClick) {}
}

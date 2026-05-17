/*
 * This file is part of the Lime Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.themes.aurora.widgets.input;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.renderer.anim.Animated;
import meteordevelopment.meteorclient.gui.renderer.anim.Easing;
import meteordevelopment.meteorclient.gui.themes.aurora.AuroraGuiTheme;
import meteordevelopment.meteorclient.gui.themes.aurora.AuroraPalette;
import meteordevelopment.meteorclient.gui.themes.aurora.AuroraWidget;
import meteordevelopment.meteorclient.gui.widgets.input.WDropdown;
import meteordevelopment.meteorclient.utils.render.color.Color;

public class WAuroraDropdown<T> extends WDropdown<T> implements AuroraWidget {
    private final Animated hoverAnim = new Animated(0, 0.14, Easing.OUT_CUBIC);

    public WAuroraDropdown(T[] values, T value) {
        super(values, value);
    }

    @Override
    protected WDropdownRoot createRootWidget() {
        return new WAuroraRoot();
    }

    @Override
    protected WDropdownValue createValueWidget() {
        return new WAuroraValue();
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        AuroraGuiTheme t = theme();
        AuroraPalette p = t.palette();

        hoverAnim.setDuration(t.animDuration(0.14));
        hoverAnim.set((mouseOver || expanded) ? 1 : 0);
        hoverAnim.update(delta);
        double ha = hoverAnim.get();

        double pad = pad();
        double radius = 10;

        // Pill background
        Color bg = p.hover();
        renderer.roundedRect(x, y, width, height, radius, bg);

        // Animated accent ring (1px, hover or expanded)
        if (ha > 0.01) {
            int alpha = (int) (255 * ha);
            Color ring = new Color(p.accent().r, p.accent().g, p.accent().b, alpha);
            renderer.roundedRectStroke(x, y, width, height, radius, new Color(0, 0, 0, 0), ring, 1);

            // Soft halo when active
            renderer.glow(x, y, width, height, radius, theme.scale(4),
                new Color(p.accent().r, p.accent().g, p.accent().b, (int)(40 * ha)));
        }

        // Value text (left of chevron)
        String text = get() == null ? "" : get().toString();
        double tw = theme.textWidth(text);
        double textY = y + (height - theme.textHeight()) / 2;
        renderer.text(text, x + pad + maxValueWidth / 2 - tw / 2, textY, p.textAccent(), false);

        // Chevron ▾ on the right (downward triangle, rotated when expanded)
        double chevSize = theme.textHeight();
        double chevX = x + pad + maxValueWidth + pad;
        double chevY = y + (height - chevSize) / 2;
        double rot = expanded ? 180 : 0;
        renderer.rotatedQuad(chevX, chevY, chevSize, chevSize, rot,
            GuiRenderer.TRIANGLE, p.textAccent());
    }

    private static class WAuroraRoot extends WDropdownRoot implements AuroraWidget {
        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            AuroraPalette p = theme().palette();
            double radius = 8;

            // Outer glow
            renderer.glow(x, y, width, height, radius, theme.scale(8), p.panelGlow());
            // Glass card body with accent border
            renderer.roundedRectStroke(x, y, width, height, radius, p.panelBase(), p.panelBorder(), 1);
        }
    }

    private class WAuroraValue extends WDropdownValue implements AuroraWidget {
        @Override
        protected void onCalculateSize() {
            double pad = pad();
            width = pad + theme.textWidth(value.toString()) + pad;
            height = pad + theme.textHeight() + pad;
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            AuroraGuiTheme t = theme();
            AuroraPalette p = t.palette();
            boolean isActive = value != null && value.equals(WAuroraDropdown.this.get());

            // Background tint
            if (isActive) {
                Color bg = new Color(p.accent().r, p.accent().g, p.accent().b, 40);
                renderer.roundedRect(x, y, width, height, 6, bg);
            } else if (mouseOver) {
                renderer.roundedRect(x, y, width, height, 6, p.hover());
            }

            String text = value.toString();
            Color textCol = isActive ? p.textAccent() : (mouseOver ? p.textPrimary() : p.textSecondary());
            renderer.text(text, x + width / 2 - theme.textWidth(text) / 2,
                y + (height - theme.textHeight()) / 2, textCol, false);
        }
    }
}

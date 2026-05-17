/*
 * This file is part of the Lime Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.themes.aurora.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.renderer.primitives.BackdropBlur;
import meteordevelopment.meteorclient.gui.themes.aurora.AuroraPalette;
import meteordevelopment.meteorclient.gui.themes.aurora.AuroraGuiTheme;
import meteordevelopment.meteorclient.gui.themes.aurora.AuroraWidget;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WWindow;
public class WAuroraWindow extends WWindow implements AuroraWidget {
    public WAuroraWindow(WWidget icon, String title) {
        super(icon, title);
    }

    @Override
    protected WHeader header(WWidget icon) {
        return new WAuroraHeader(icon);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        if (!(expanded || animProgress > 0)) return;

        AuroraGuiTheme t = theme();
        AuroraPalette p = t.palette();
        double bodyY = y + header.height;
        double bodyH = height - header.height;

        // 1. Backdrop blur stub (no-op until pipeline is wired)
        if (t.backdropBlur.get()) {
            BackdropBlur.sampleInto(x, bodyY, width, bodyH, 14, 0.5);
        }

        // 2. Soft outer ambient glow
        renderer.glow(x, bodyY, width, bodyH, 14, 24, p.panelGlow());

        // 3. Glass body: filled rounded rect with border stroke
        renderer.roundedRectStroke(x, bodyY, width, bodyH, 14, p.panelBase(), p.panelBorder(), 1);

        // 4. Top inner highlight — a horizontal gradient line fading in from the left and out to the right
        renderer.gradientLinear(x + 12, bodyY, width - 24, 1, p.panelTop(), p.panelTop(), 0);
    }

    private class WAuroraHeader extends WHeader {
        private static final double RADIUS = 14;
        private static final double ACCENT_DOT = 7;

        public WAuroraHeader(WWidget icon) {
            super(icon);
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            AuroraPalette p = theme().palette();

            // Header background — same radius as body, only top corners rounded
            renderer.roundedRect(x, y, width, height, RADIUS, p.panelBase());

            // Bottom divider line
            renderer.quad(x, y + height - 1, width, 1, p.dividerStrong());

            // Accent dot (glow + filled circle) at left side
            double dotX = x + theme().scale(10);
            double dotY = y + height / 2 - ACCENT_DOT / 2;
            renderer.glow(dotX, dotY, ACCENT_DOT, ACCENT_DOT, ACCENT_DOT / 2, 6, p.accentGlow());
            renderer.roundedRect(dotX, dotY, ACCENT_DOT, ACCENT_DOT, ACCENT_DOT / 2, p.accent());
        }
    }
}

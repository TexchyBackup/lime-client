/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens.debug;

import meteordevelopment.meteorclient.gui.renderer.primitives.BackdropBlur;
import meteordevelopment.meteorclient.gui.renderer.primitives.Glow;
import meteordevelopment.meteorclient.gui.renderer.primitives.Gradient;
import meteordevelopment.meteorclient.gui.renderer.primitives.RoundedRect;
import meteordevelopment.meteorclient.gui.themes.aurora.AuroraPalette;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * Debug screen that draws every Aurora primitive once so each can be eyeballed
 * for visual correctness. Bound to F8 in dev environments.
 */
public class AuroraDebugScreen extends Screen {
    private static final AuroraPalette PALETTE = new AuroraPalette(new Color(136, 238, 85));

    public AuroraDebugScreen() {
        super(Component.literal("Aurora Debug"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        // Backdrop blur (reuses existing dual-Kawase pipelines)
        if (BackdropBlur.isEnabled()) BackdropBlur.captureAndBlur();

        // Use unscaled projection so our primitives draw in pixel coordinates
        Utils.unscaledProjection();

        int w = mc.getWindow().getWidth();
        // baseline 'pad' in pixels (scaled by GUI scale for visibility on hi-DPI)
        int s = mc.getWindow().getGuiScale();
        int pad = 12 * s;
        int x = pad;
        int y = pad;

        // 1) Row of 4 rounded rects at radii 4,10,14,22
        int rrW = 80 * s, rrH = 50 * s, rrGap = 16 * s;
        int[] radii = {4 * s, 10 * s, 14 * s, 22 * s};
        for (int i = 0; i < 4; i++) {
            int rx = x + i * (rrW + rrGap);
            if (i < 2) {
                RoundedRect.draw(rx, y, rrW, rrH, radii[i], PALETTE.panelBase());
            } else {
                RoundedRect.draw(rx, y, rrW, rrH, radii[i], PALETTE.panelBase(), PALETTE.accent(), 2.0 * s);
            }
        }

        // 2) Row of 3 glow halos
        y += rrH + 32 * s;
        int gW = 60 * s, gH = 60 * s;
        int[] gRad = {8 * s, 16 * s, 24 * s};
        double[] gGlow = {20.0 * s, 36.0 * s, 52.0 * s};
        for (int i = 0; i < 3; i++) {
            int gx = x + i * (gW + 60 * s);
            Glow.draw(gx, y, gW, gH, gRad[i], gGlow[i], PALETTE.accentGlow());
            RoundedRect.draw(gx, y, gW, gH, gRad[i], PALETTE.panelBase());
        }

        // 3) Four gradient cards
        y += gH + 32 * s;
        int cardW = 96 * s, cardH = 60 * s, cardGap = 12 * s;
        Color a = PALETTE.accent();
        Color b = new Color(20, 30, 22, 255);
        Gradient.linear(x + 0 * (cardW + cardGap), y, cardW, cardH, 8 * s, a, b, 0);    // horizontal
        Gradient.linear(x + 1 * (cardW + cardGap), y, cardW, cardH, 8 * s, a, b, 90);   // vertical
        Gradient.linear(x + 2 * (cardW + cardGap), y, cardW, cardH, 8 * s, a, b, 45);   // diagonal
        Gradient.radial(x + 3 * (cardW + cardGap), y, cardW, cardH, 8 * s, a, b);

        // 4) Backdrop-blur panel (sampleInto is a stub — fall back to a translucent rounded rect)
        y += cardH + 32 * s;
        int panelW = 4 * cardW + 3 * cardGap;
        int panelH = 80 * s;
        BackdropBlur.sampleInto(x, y, panelW, panelH, 14 * s, 0.35);
        RoundedRect.draw(x, y, panelW, panelH, 14 * s, PALETTE.panelBase(), PALETTE.panelBorder(), 1.0 * s);

        // Restore for vanilla text/UI
        Utils.scaledProjection();

        // 5) Three text samples via vanilla font (LimeFonts not yet wired into MC's font system).
        if (font != null) {
            int ts = 12;
            int tx = pad / s;
            int ty = pad / s;
            int textY = ty + (50 + 60 + 60 + 80 + 4 * 32 + 16);
            graphics.text(font, "Aurora",          tx,                 textY,      0xFFE8F5E0, true);
            graphics.text(font, "Lime Client v0.5.8", tx,              textY + ts, 0xFF8A9A8A, true);
            graphics.text(font, "242 FPS",         tx,                 textY + 2 * ts, 0xFF88EE55, true);
        }
    }
}

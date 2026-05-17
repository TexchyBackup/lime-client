/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens.debug;

import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.gui.renderer.primitives.BackdropBlur;
import meteordevelopment.meteorclient.gui.renderer.primitives.Glow;
import meteordevelopment.meteorclient.gui.renderer.primitives.Gradient;
import meteordevelopment.meteorclient.gui.renderer.primitives.RoundedRect;
import meteordevelopment.meteorclient.gui.themes.aurora.AuroraPalette;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Debug screen that draws every Aurora primitive once so each can be eyeballed
 * for visual correctness. Bound to F8 in dev environments.
 *
 * <p>Extends {@link WidgetScreen} so that {@link #onRenderBefore} is invoked
 * from the post-GUI mixin hook (see {@code GuiRendererMixin#render$postGui}),
 * AFTER the vanilla GUI batch has finished rendering. Drawing primitives
 * directly from {@code Screen#extractRenderState} runs during the GUI
 * <em>extract</em> phase — vanilla then renders the GUI batch on top and
 * paints over any immediate-mode draws, leaving the primitives invisible.
 */
public class AuroraDebugScreen extends WidgetScreen {
    private static final AuroraPalette PALETTE = new AuroraPalette(new Color(136, 238, 85));

    public AuroraDebugScreen() {
        super(GuiThemes.get(), "Aurora Debug");
    }

    @Override
    public void initWidgets() {
        // No widgets — primitives drawn directly in onRenderBefore.
    }

    @Override
    protected void onRenderBefore(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        BackdropBlur.captureAndBlur();
        int pad = 16;
        int x = pad;
        int y = pad;

        // 1) Row of 4 rounded rects at radii 4,10,14,22
        int rrW = 90, rrH = 56, rrGap = 14;
        int[] radii = {4, 10, 14, 22};
        for (int i = 0; i < 4; i++) {
            int rx = x + i * (rrW + rrGap);
            if (i < 2) {
                RoundedRect.draw(rx, y, rrW, rrH, radii[i], PALETTE.panelBase());
            } else {
                RoundedRect.draw(rx, y, rrW, rrH, radii[i], PALETTE.panelBase(), PALETTE.accent(), 2.0);
            }
        }

        // 2) Row of 3 glow halos around small rounded rects
        y += rrH + 40;
        int gW = 64, gH = 64;
        int[] gRad = {8, 16, 24};
        double[] gGlow = {18.0, 30.0, 44.0};
        for (int i = 0; i < 3; i++) {
            int gx = x + i * (gW + 60);
            Glow.draw(gx, y, gW, gH, gRad[i], gGlow[i], PALETTE.accentGlow());
            RoundedRect.draw(gx, y, gW, gH, gRad[i], PALETTE.panelBase());
        }

        // 3) Four gradient cards
        y += gH + 40;
        int cardW = 104, cardH = 64, cardGap = 12;
        Color a = PALETTE.accent();
        Color b = new Color(20, 30, 22, 255);
        Gradient.linear(x + 0 * (cardW + cardGap), y, cardW, cardH, 8, a, b, 0);    // horizontal
        Gradient.linear(x + 1 * (cardW + cardGap), y, cardW, cardH, 8, a, b, 90);   // vertical
        Gradient.linear(x + 2 * (cardW + cardGap), y, cardW, cardH, 8, a, b, 45);   // diagonal
        Gradient.radial(x + 3 * (cardW + cardGap), y, cardW, cardH, 8, a, b);

        // 4) Backdrop-blur panel (sampleInto is a stub — falls back to translucent rect)
        y += cardH + 40;
        int panelW = 4 * cardW + 3 * cardGap;
        int panelH = 100;
        BackdropBlur.sampleInto(x, y, panelW, panelH, 14, 0.35);
        RoundedRect.draw(x, y, panelW, panelH, 14, PALETTE.panelBase(), PALETTE.panelBorder(), 1.0);

        // Text samples via vanilla font (LimeFonts not yet wired into MC font system)
        if (font != null) {
            int textY = y + panelH + 16;
            int ts = font.lineHeight + 2;
            graphics.text(font, "Aurora",              x, textY,            0xFFE8F5E0, true);
            graphics.text(font, "Lime Client v0.5.8",  x, textY + ts,       0xFF8A9A8A, true);
            graphics.text(font, "242 FPS",             x, textY + 2 * ts,   0xFF88EE55, true);
        }
    }
}

/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens.debug;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.renderer.primitives.BackdropBlur;
import meteordevelopment.meteorclient.gui.renderer.primitives.Glow;
import meteordevelopment.meteorclient.gui.renderer.primitives.Gradient;
import meteordevelopment.meteorclient.gui.renderer.primitives.RoundedRect;
import meteordevelopment.meteorclient.gui.themes.aurora.AuroraPalette;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Debug screen that draws every Aurora primitive once so each can be eyeballed
 * for visual correctness. Bound to F8 in dev environments.
 *
 * <p>Primitives are scheduled via {@link GuiRenderer#absolutePost} so they
 * render after the vanilla GUI batch — calling them directly from
 * {@code extractRenderState} executes at the wrong render phase and produces
 * no visible output.
 */
public class AuroraDebugScreen extends Screen {
    private static final AuroraPalette PALETTE = new AuroraPalette(new Color(136, 238, 85));

    private final GuiRenderer renderer = new GuiRenderer();

    public AuroraDebugScreen() {
        super(Component.literal("Aurora Debug"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        renderer.begin(graphics);
        try {
            int pad = 16;
            int x = pad;
            int y = pad;

            // 1) Row of 4 rounded rects at radii 4,10,14,22
            int rrW = 90, rrH = 56, rrGap = 14;
            int[] radii = {4, 10, 14, 22};
            for (int i = 0; i < 4; i++) {
                int rx = x + i * (rrW + rrGap);
                if (i < 2) {
                    renderer.roundedRect(rx, y, rrW, rrH, radii[i], PALETTE.panelBase());
                } else {
                    renderer.roundedRectStroke(rx, y, rrW, rrH, radii[i], PALETTE.panelBase(), PALETTE.accent(), 2.0);
                }
            }

            // 2) Row of 3 glow halos around small rounded rects
            y += rrH + 40;
            int gW = 64, gH = 64;
            int[] gRad = {8, 16, 24};
            double[] gGlow = {18.0, 30.0, 44.0};
            for (int i = 0; i < 3; i++) {
                int gx = x + i * (gW + 60);
                renderer.glow(gx, y, gW, gH, gRad[i], gGlow[i], PALETTE.accentGlow());
                renderer.roundedRect(gx, y, gW, gH, gRad[i], PALETTE.panelBase());
            }

            // 3) Four gradient cards
            y += gH + 40;
            int cardW = 104, cardH = 64, cardGap = 12;
            Color a = PALETTE.accent();
            Color b = new Color(20, 30, 22, 255);
            renderer.gradientLinear(x + 0 * (cardW + cardGap), y, cardW, cardH, 8, a, b, 0);    // horizontal
            renderer.gradientLinear(x + 1 * (cardW + cardGap), y, cardW, cardH, 8, a, b, 90);   // vertical
            renderer.gradientLinear(x + 2 * (cardW + cardGap), y, cardW, cardH, 8, a, b, 45);   // diagonal
            renderer.gradientRadial(x + 3 * (cardW + cardGap), y, cardW, cardH, 8, a, b);

            // 4) Backdrop-blur panel (sampleInto is a stub — falls back to translucent rect)
            y += cardH + 40;
            int panelW = 4 * cardW + 3 * cardGap;
            int panelH = 100;
            BackdropBlur.sampleInto(x, y, panelW, panelH, 14, 0.35);
            renderer.roundedRectStroke(x, y, panelW, panelH, 14, PALETTE.panelBase(), PALETTE.panelBorder(), 1.0);

            // Text samples via vanilla font (LimeFonts not yet wired into MC font system)
            if (font != null) {
                int textY = y + panelH + 16;
                int ts = font.lineHeight + 2;
                graphics.text(font, "Aurora",              x, textY,            0xFFE8F5E0, true);
                graphics.text(font, "Lime Client v0.5.8",  x, textY + ts,       0xFF8A9A8A, true);
                graphics.text(font, "242 FPS",             x, textY + 2 * ts,   0xFF88EE55, true);
            }
        } finally {
            renderer.end();
        }
    }
}

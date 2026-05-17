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
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.WMeteorLabel;
import meteordevelopment.meteorclient.gui.utils.CharFilter;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.utils.render.color.Color;

public class WAuroraTextBox extends WTextBox implements AuroraWidget {
    private final Animated focusAnim = new Animated(0, 0.18, Easing.OUT_CUBIC);
    private final Animated cursorBlink = new Animated(1, 0.5, Easing.LINEAR);
    private double blinkClock;
    private boolean blinkOn = true;

    public WAuroraTextBox(String text, String placeholder, CharFilter filter, Class<? extends Renderer> renderer) {
        super(text, placeholder, filter, renderer);
    }

    @Override
    protected WContainer createCompletionsRootWidget() {
        return new WVerticalList() {
            @Override
            protected void onRender(GuiRenderer renderer1, double mouseX, double mouseY, double delta) {
                AuroraPalette p = ((AuroraGuiTheme) theme).palette();
                double radius = 6;
                renderer1.glow(x, y, width, height, radius, theme.scale(6), p.panelGlow());
                renderer1.roundedRectStroke(x, y, width, height, radius, p.panelBase(), p.panelBorder(), 1);
            }
        };
    }

    @SuppressWarnings("unchecked")
    @Override
    protected <T extends WWidget & ICompletionItem> T createCompletionsValueWidth(String completion, boolean selected) {
        return (T) new CompletionItem(completion, selected);
    }

    private static class CompletionItem extends WMeteorLabel implements ICompletionItem {
        private boolean selected;

        public CompletionItem(String text, boolean selected) {
            super(text, false);
            this.selected = selected;
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            if (selected) {
                AuroraPalette p = ((AuroraGuiTheme) theme).palette();
                renderer.roundedRect(x, y, width, height, 4,
                    new Color(p.accent().r, p.accent().g, p.accent().b, 50));
            }
            super.onRender(renderer, mouseX, mouseY, delta);
        }

        @Override
        public boolean isSelected() { return selected; }

        @Override
        public void setSelected(boolean selected) { this.selected = selected; }

        @Override
        public String getCompletion() { return text; }
    }

    @Override
    protected void onCursorChanged() {
        blinkOn = true;
        blinkClock = 0;
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        AuroraGuiTheme t = theme();
        AuroraPalette p = t.palette();
        double pad = pad();
        double radius = 6;

        // Focus animation
        focusAnim.setDuration(t.animDuration(0.18));
        focusAnim.set(focused ? 1 : 0);
        focusAnim.update(delta);
        double fa = focusAnim.get();

        // Focus glow halo
        if (fa > 0.01) {
            renderer.glow(x, y, width, height, radius, theme.scale(6),
                new Color(p.accent().r, p.accent().g, p.accent().b, (int)(70 * fa)));
        }

        // Background panel + border (divider unfocused → accent focused)
        Color border = focused ? p.accent() : p.divider();
        renderer.roundedRectStroke(x, y, width, height, radius, p.panelBase(), border, 1);

        double overflowWidth = getOverflowWidthForRender();
        renderer.scissorStart(x + pad, y + pad, width - pad * 2, height - pad * 2);

        // Text or placeholder
        if (!text.isEmpty()) {
            this.renderer.render(renderer, x + pad - overflowWidth, y + pad, text, p.textPrimary());
        } else if (placeholder != null) {
            this.renderer.render(renderer, x + pad - overflowWidth, y + pad, placeholder, p.textMuted());
        }

        // Selection highlight
        if (focused && (cursor != selectionStart || cursor != selectionEnd)) {
            double selStart = x + pad + getTextWidth(selectionStart) - overflowWidth;
            double selEnd = x + pad + getTextWidth(selectionEnd) - overflowWidth;
            Color hi = new Color(p.accent().r, p.accent().g, p.accent().b, 90);
            renderer.quad(selStart, y + pad, selEnd - selStart, t.textHeight(), hi);
        }

        // Cursor (blink at 1s loop)
        if (focused) {
            blinkClock += delta;
            if (blinkClock >= 0.5) {
                blinkOn = !blinkOn;
                blinkClock = 0;
            }
            if (blinkOn) {
                double cx = x + pad + getTextWidth(cursor) - overflowWidth;
                renderer.quad(cx, y + pad, t.scale(1), t.textHeight(), p.accent());
            }
        }

        renderer.scissorEnd();
    }
}

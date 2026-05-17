/*
 * This file is part of the Lime Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.themes.aurora.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.renderer.anim.Animated;
import meteordevelopment.meteorclient.gui.renderer.anim.Easing;
import meteordevelopment.meteorclient.gui.themes.aurora.AuroraGuiTheme;
import meteordevelopment.meteorclient.gui.themes.aurora.AuroraWidget;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;
import meteordevelopment.meteorclient.gui.widgets.pressable.WTriangle;

public class WAuroraSection extends WSection {
    public WAuroraSection(String title, boolean expanded, WWidget headerWidget) {
        super(title, expanded, headerWidget);
    }

    @Override
    protected WHeader createHeader() {
        return new WAuroraSectionHeader(title);
    }

    protected class WAuroraSectionHeader extends WHeader {
        private WTriangle triangle;
        /** Animates from 0 (collapsed) to 1 (expanded) — drives chevron rotation. */
        private final Animated chevron;

        public WAuroraSectionHeader(String title) {
            super(title);
            chevron = new Animated(expanded ? 1 : 0, 0.18, Easing.OUT_CUBIC);
        }

        @Override
        public void init() {
            add(theme.horizontalSeparator(title)).expandX();

            if (headerWidget != null) add(headerWidget);

            triangle = new WAuroraHeaderTriangle();
            triangle.theme = theme;
            triangle.action = this::onClick;
            add(triangle);
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            AuroraGuiTheme t = (AuroraGuiTheme) theme;
            chevron.setDuration(t.animDuration(0.18));
            chevron.set(WAuroraSection.this.animProgress);
            chevron.update(delta);
            triangle.rotation = (1 - chevron.get()) * -90;
        }
    }

    protected static class WAuroraHeaderTriangle extends WTriangle implements AuroraWidget {
        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            renderer.rotatedQuad(x, y, width, height, rotation, GuiRenderer.TRIANGLE, theme().palette().textPrimary());
        }
    }
}

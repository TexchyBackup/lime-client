/*
 * This file is part of the Lime Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.themes.aurora.widgets.pressable;

import meteordevelopment.meteorclient.gui.themes.aurora.AuroraPalette;
import meteordevelopment.meteorclient.gui.themes.aurora.AuroraWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WFavorite;
import meteordevelopment.meteorclient.utils.render.color.Color;

public class WAuroraFavorite extends WFavorite implements AuroraWidget {
    public WAuroraFavorite(boolean checked) {
        super(checked);
    }

    @Override
    protected Color getColor() {
        AuroraPalette p = theme().palette();
        // Active: accent color with glow effect rendered in the parent; inactive: muted text
        return checked ? p.accent() : p.textMuted();
    }
}

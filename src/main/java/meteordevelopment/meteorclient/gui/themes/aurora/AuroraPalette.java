/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.themes.aurora;

import meteordevelopment.meteorclient.utils.render.color.Color;

public final class AuroraPalette {
    public static final Color BG_TOP         = new Color(5, 10, 8);
    public static final Color BG_BOTTOM      = new Color(10, 15, 10);
    public static final Color PANEL_BASE     = new Color(15, 22, 17, 245);
    public static final Color PANEL_TOP      = new Color(255, 255, 255, 20);
    public static final Color PANEL_SHADOW   = new Color(0, 0, 0, 153);
    public static final Color TEXT_PRIMARY   = new Color(232, 245, 224);
    public static final Color TEXT_SECONDARY = new Color(138, 154, 138);
    public static final Color TEXT_MUTED     = new Color(90, 110, 90);
    public static final Color DIVIDER        = new Color(255, 255, 255, 10);
    public static final Color DIVIDER_STRONG = new Color(255, 255, 255, 20);
    public static final Color HOVER          = new Color(255, 255, 255, 8);
    public static final Color SLIDER_TRACK   = new Color(255, 255, 255, 15);

    private final Color accent;
    private final Color accentLight;
    private final Color accentGlow;
    private final Color panelBorder;
    private final Color panelGlow;

    public AuroraPalette(Color accent) {
        this.accent      = new Color(accent.r, accent.g, accent.b, 255);
        this.accentLight = lighten(this.accent, 0.18);
        this.accentGlow  = new Color(accent.r, accent.g, accent.b, 128);
        this.panelBorder = new Color(accent.r, accent.g, accent.b, 38);
        this.panelGlow   = new Color(accent.r, accent.g, accent.b, 20);
    }

    public Color accent()         { return accent; }
    public Color accentLight()    { return accentLight; }
    public Color accentGlow()     { return accentGlow; }
    public Color panelBase()      { return PANEL_BASE; }
    public Color panelTop()       { return PANEL_TOP; }
    public Color panelBorder()    { return panelBorder; }
    public Color panelGlow()      { return panelGlow; }
    public Color textPrimary()    { return TEXT_PRIMARY; }
    public Color textSecondary()  { return TEXT_SECONDARY; }
    public Color textMuted()      { return TEXT_MUTED; }
    public Color textAccent()     { return accentLight; }
    public Color divider()        { return DIVIDER; }
    public Color dividerStrong()  { return DIVIDER_STRONG; }
    public Color hover()          { return HOVER; }
    public Color sliderTrack()    { return SLIDER_TRACK; }

    private static Color lighten(Color c, double amount) {
        int r = (int) Math.min(255, c.r + (255 - c.r) * amount);
        int g = (int) Math.min(255, c.g + (255 - c.g) * amount);
        int b = (int) Math.min(255, c.b + (255 - c.b) * amount);
        return new Color(r, g, b, c.a);
    }
}

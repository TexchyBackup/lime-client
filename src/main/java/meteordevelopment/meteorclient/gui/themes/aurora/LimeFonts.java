/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.themes.aurora;

import meteordevelopment.meteorclient.renderer.Fonts;
import meteordevelopment.meteorclient.renderer.text.FontFace;
import meteordevelopment.meteorclient.renderer.text.FontFamily;
import meteordevelopment.meteorclient.renderer.text.FontInfo;

/**
 * Symbolic font lookup for Aurora widgets. Resolves to FontFamilies loaded by Fonts.refresh().
 * Falls back to the user's default font if a family is not present (e.g. asset missing).
 */
public final class LimeFonts {
    private LimeFonts() {}

    public static FontFace ui()      { return resolve("Inter"); }
    public static FontFace display() { return resolve("Space Grotesk"); }
    public static FontFace mono()    { return resolve("JetBrains Mono"); }

    private static FontFace resolve(String family) {
        FontFamily f = Fonts.getFamily(family);
        if (f == null) return Fonts.DEFAULT_FONT;
        FontFace face = f.get(FontInfo.Type.Regular);
        return face != null ? face : Fonts.DEFAULT_FONT;
    }
}

package meteordevelopment.meteorclient.gui.themes.aurora;

import meteordevelopment.meteorclient.utils.render.color.Color;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AuroraPaletteTest {
    @Test void accentRoundtrips() {
        Color lime = new Color(136, 238, 85);
        AuroraPalette p = new AuroraPalette(lime);
        assertEquals(136, p.accent().r);
        assertEquals(238, p.accent().g);
        assertEquals(85,  p.accent().b);
        assertEquals(255, p.accent().a);
    }

    @Test void lightenedAccentIsLighter() {
        AuroraPalette p = new AuroraPalette(new Color(136, 238, 85));
        Color lit = p.accentLight();
        assertTrue(lit.r >= 136, "R should be >= source");
        assertTrue(lit.g >= 238, "G should be >= source");
        assertTrue(lit.b >= 85,  "B should be >= source");
    }

    @Test void glowAccentIsTranslucent() {
        AuroraPalette p = new AuroraPalette(new Color(136, 238, 85));
        Color glow = p.accentGlow();
        assertTrue(glow.a > 0 && glow.a < 255, "Glow alpha must be in (0,255), was " + glow.a);
        assertEquals(136, glow.r);
        assertEquals(238, glow.g);
        assertEquals(85,  glow.b);
    }

    @Test void panelTokensAreIndependentOfAccent() {
        AuroraPalette p1 = new AuroraPalette(new Color(255, 0, 0));
        AuroraPalette p2 = new AuroraPalette(new Color(0, 0, 255));
        assertEquals(p1.panelBase().r, p2.panelBase().r);
        assertEquals(p1.panelBase().g, p2.panelBase().g);
        assertEquals(p1.panelBase().b, p2.panelBase().b);
    }

    @Test void panelBorderInheritsAccentHue() {
        AuroraPalette pRed = new AuroraPalette(new Color(255, 0, 0));
        AuroraPalette pBlue = new AuroraPalette(new Color(0, 0, 255));
        assertEquals(255, pRed.panelBorder().r);
        assertEquals(255, pBlue.panelBorder().b);
        assertTrue(pRed.panelBorder().a > 0 && pRed.panelBorder().a < 255);
    }
}

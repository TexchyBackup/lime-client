package meteordevelopment.meteorclient.gui.renderer.anim;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EasingTest {
    private static final double E = 1e-4;

    @Test void linearEndpoints() {
        assertEquals(0.0, Easing.LINEAR.apply(0.0), E);
        assertEquals(1.0, Easing.LINEAR.apply(1.0), E);
        assertEquals(0.5, Easing.LINEAR.apply(0.5), E);
    }

    @Test void easeOutCubicEndpoints() {
        assertEquals(0.0, Easing.OUT_CUBIC.apply(0.0), E);
        assertEquals(1.0, Easing.OUT_CUBIC.apply(1.0), E);
    }

    @Test void easeOutCubicIsMonotonic() {
        double prev = -1;
        for (int i = 0; i <= 100; i++) {
            double v = Easing.OUT_CUBIC.apply(i / 100.0);
            assertTrue(v >= prev, "OUT_CUBIC must be monotonic at t=" + i);
            prev = v;
        }
    }

    @Test void easeOutBackOvershoots() {
        boolean overshoots = false;
        for (int i = 60; i <= 90; i++) {
            if (Easing.OUT_BACK.apply(i / 100.0) > 1.0) overshoots = true;
        }
        assertTrue(overshoots, "OUT_BACK should overshoot above 1.0");
        assertEquals(1.0, Easing.OUT_BACK.apply(1.0), E);
    }
}

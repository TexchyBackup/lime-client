package meteordevelopment.meteorclient.gui.renderer.anim;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AnimatedTest {
    @Test void startsAtInitialValue() {
        Animated a = new Animated(5.0, 0.180, Easing.OUT_CUBIC);
        assertEquals(5.0, a.get(), 1e-6);
    }

    @Test void reachesTargetAfterFullDuration() {
        Animated a = new Animated(0.0, 0.180, Easing.LINEAR);
        a.set(1.0);
        a.update(0.180);
        assertEquals(1.0, a.get(), 1e-4);
    }

    @Test void linearAtHalfDuration() {
        Animated a = new Animated(0.0, 0.180, Easing.LINEAR);
        a.set(1.0);
        a.update(0.090);
        assertEquals(0.5, a.get(), 1e-4);
    }

    @Test void retargetMidFlightContinues() {
        Animated a = new Animated(0.0, 0.200, Easing.LINEAR);
        a.set(1.0);
        a.update(0.100);   // halfway → ~0.5
        a.set(0.0);        // retarget back
        a.update(0.200);
        assertEquals(0.0, a.get(), 1e-4);
    }

    @Test void instantWhenDurationZero() {
        Animated a = new Animated(0.0, 0.0, Easing.LINEAR);
        a.set(7.0);
        a.update(0.001);
        assertEquals(7.0, a.get(), 1e-6);
    }
}

/*
 * This file is part of the Lime Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.renderer.anim;

public interface Easing {
    double apply(double t);

    Easing LINEAR        = t -> t;
    Easing OUT_CUBIC     = t -> 1 - Math.pow(1 - t, 3);
    Easing IN_CUBIC      = t -> t * t * t;
    Easing IN_OUT_CUBIC  = t -> t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
    Easing OUT_BACK      = t -> { double c1 = 1.70158, c3 = c1 + 1; return 1 + c3 * Math.pow(t - 1, 3) + c1 * Math.pow(t - 1, 2); };
    Easing OUT_SINE      = t -> Math.sin((t * Math.PI) / 2);
    Easing IN_OUT_SINE   = t -> -(Math.cos(Math.PI * t) - 1) / 2;
}

/*
 * This file is part of the Lime Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.renderer.anim;

public class Animated {
    private double from;
    private double to;
    private double elapsed;
    private double duration;
    private Easing easing;

    public Animated(double initial, double duration, Easing easing) {
        this.from = initial;
        this.to = initial;
        this.elapsed = duration;
        this.duration = duration;
        this.easing = easing;
    }

    public void set(double target) {
        if (Math.abs(target - to) < 1e-9) return;
        this.from = get();
        this.to = target;
        this.elapsed = 0;
    }

    public void setInstant(double value) {
        this.from = value;
        this.to = value;
        this.elapsed = duration;
    }

    public void update(double dt) {
        if (elapsed >= duration) return;
        elapsed = Math.min(duration, elapsed + dt);
    }

    public double get() {
        if (duration <= 0 || elapsed >= duration) return to;
        double t = easing.apply(elapsed / duration);
        return from + (to - from) * t;
    }

    public void setDuration(double d) { this.duration = d; }
    public void setEasing(Easing e) { this.easing = e; }
    public double target() { return to; }
}

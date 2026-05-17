/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.renderer.primitives;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.vertex.VertexFormat;
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import meteordevelopment.meteorclient.renderer.MeshRenderer;
import meteordevelopment.meteorclient.renderer.MeteorRenderPipelines;
import meteordevelopment.meteorclient.renderer.MeteorVertexFormats;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DynamicUniformStorage;

import java.nio.ByteBuffer;

/**
 * Soft glow halo around an SDF rounded rect. The emitted quad is expanded by
 * {@code glowRadius} on all sides so the gaussian falloff has room to fade.
 */
public final class Glow {
    private static final int UNIFORM_SIZE = new Std140SizeCalculator()
        .putVec2()    // halfSize
        .putFloat()   // radius
        .putFloat()   // glowRadius
        .get();

    private static final DynamicUniformStorage<GlowUniform> STORAGE =
        new DynamicUniformStorage<>("Aurora - Glow UBO", UNIFORM_SIZE, 16);

    public static void flipFrame() {
        STORAGE.endFrame();
    }

    public static void draw(double x, double y, double w, double h, double radius, double glowRadius, Color color) {
        if (w <= 0 || h <= 0) return;

        float hw = (float) (w * 0.5);
        float hh = (float) (h * 0.5);
        float cx = (float) (x + hw);
        float cy = (float) (y + hh);
        float r = (float) Math.min(radius, Math.min(hw, hh));
        float g = (float) Math.max(0.5, glowRadius);

        float qHw = hw + g;
        float qHh = hh + g;

        GpuBufferSlice slice = STORAGE.writeUniform(new GlowUniform(hw, hh, r, g));

        MeshBuilder mb = new MeshBuilder(MeteorVertexFormats.POS2_TEXTURE_COLOR, VertexFormat.Mode.TRIANGLES);
        mb.begin();
        mb.ensureQuadCapacity();
        mb.quad(
            mb.vec2(cx - qHw, cy - qHh).vec2(-qHw, -qHh).color(color).next(),
            mb.vec2(cx - qHw, cy + qHh).vec2(-qHw,  qHh).color(color).next(),
            mb.vec2(cx + qHw, cy + qHh).vec2( qHw,  qHh).color(color).next(),
            mb.vec2(cx + qHw, cy - qHh).vec2( qHw, -qHh).color(color).next()
        );
        mb.end();

        MeshRenderer.begin()
            .attachments(Minecraft.getInstance().getMainRenderTarget())
            .pipeline(MeteorRenderPipelines.AURORA_GLOW)
            .mesh(mb)
            .uniform("AuroraGlowData", slice)
            .end();
    }

    private Glow() {}

    private record GlowUniform(float halfX, float halfY, float radius, float glowRadius)
        implements DynamicUniformStorage.DynamicUniform {
        @Override
        public void write(ByteBuffer buffer) {
            Std140Builder.intoBuffer(buffer)
                .putVec2(halfX, halfY)
                .putFloat(radius)
                .putFloat(glowRadius);
        }
    }
}

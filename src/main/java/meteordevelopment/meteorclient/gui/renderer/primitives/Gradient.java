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
 * Linear / radial gradient primitive (rounded-rect masked).
 *
 * <p>Linear: direction is a unit vector; t = dot(local, dir)/extent + 0.5.
 * Radial: direction ignored, t = length(local) / length(halfSize).
 */
public final class Gradient {
    private static final int UNIFORM_SIZE = new Std140SizeCalculator()
        .putVec2()   // halfSize
        .putVec2()   // direction
        .putVec4()   // colorA
        .putVec4()   // colorB
        .putFloat()  // radius
        .putFloat()  // mode (0 linear, 1 radial)
        .get();

    private static final DynamicUniformStorage<GradUniform> STORAGE =
        new DynamicUniformStorage<>("Aurora - Gradient UBO", UNIFORM_SIZE, 16);

    public static void linear(double x, double y, double w, double h, Color colorA, Color colorB, double angleDegrees) {
        linear(x, y, w, h, 0, colorA, colorB, angleDegrees);
    }

    public static void linear(double x, double y, double w, double h, double radius, Color colorA, Color colorB, double angleDegrees) {
        double rad = Math.toRadians(angleDegrees);
        emit(x, y, w, h, radius, (float) Math.cos(rad), (float) Math.sin(rad), colorA, colorB, 0f);
    }

    public static void radial(double x, double y, double w, double h, Color centerColor, Color edgeColor) {
        radial(x, y, w, h, 0, centerColor, edgeColor);
    }

    public static void radial(double x, double y, double w, double h, double radius, Color centerColor, Color edgeColor) {
        emit(x, y, w, h, radius, 0f, 0f, centerColor, edgeColor, 1f);
    }

    private static void emit(double x, double y, double w, double h, double radius,
                             float dirX, float dirY, Color colorA, Color colorB, float mode) {
        if (w <= 0 || h <= 0) return;

        float hw = (float) (w * 0.5);
        float hh = (float) (h * 0.5);
        float cx = (float) (x + hw);
        float cy = (float) (y + hh);
        float r = (float) Math.min(radius, Math.min(hw, hh));

        GpuBufferSlice slice = STORAGE.writeUniform(new GradUniform(
            hw, hh, dirX, dirY,
            colorA.r / 255f, colorA.g / 255f, colorA.b / 255f, colorA.a / 255f,
            colorB.r / 255f, colorB.g / 255f, colorB.b / 255f, colorB.a / 255f,
            r, mode
        ));

        MeshBuilder mb = new MeshBuilder(MeteorVertexFormats.POS2_TEXTURE_COLOR, VertexFormat.Mode.TRIANGLES);
        mb.begin();
        mb.ensureQuadCapacity();
        Color white = Color.WHITE;
        mb.quad(
            mb.vec2(cx - hw, cy - hh).vec2(-hw, -hh).color(white).next(),
            mb.vec2(cx - hw, cy + hh).vec2(-hw,  hh).color(white).next(),
            mb.vec2(cx + hw, cy + hh).vec2( hw,  hh).color(white).next(),
            mb.vec2(cx + hw, cy - hh).vec2( hw, -hh).color(white).next()
        );
        mb.end();

        MeshRenderer.begin()
            .attachments(Minecraft.getInstance().getMainRenderTarget())
            .pipeline(MeteorRenderPipelines.AURORA_GRADIENT)
            .mesh(mb)
            .uniform("AuroraGradientData", slice)
            .end();
    }

    private Gradient() {}

    private record GradUniform(float hx, float hy, float dx, float dy,
                               float ar, float ag, float ab, float aa,
                               float br, float bg, float bb, float ba,
                               float radius, float mode) implements DynamicUniformStorage.DynamicUniform {
        @Override
        public void write(ByteBuffer buffer) {
            Std140Builder.intoBuffer(buffer)
                .putVec2(hx, hy)
                .putVec2(dx, dy)
                .putVec4(ar, ag, ab, aa)
                .putVec4(br, bg, bb, ba)
                .putFloat(radius)
                .putFloat(mode);
        }
    }
}

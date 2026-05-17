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
 * Aurora SDF-shaded rounded rectangle primitive.
 *
 * <p>Each call emits a single quad and binds its own UBO. This is intentionally
 * one-draw-call-per-primitive — fine for a debug screen and small panel counts,
 * not batched. A batched variant would require packing per-vertex SDF params.
 */
public final class RoundedRect {
    private static final int UNIFORM_SIZE = new Std140SizeCalculator()
        .putVec2()    // halfSize
        .putFloat()   // radius
        .putFloat()   // borderWidth
        .putVec4()    // borderColor
        .get();

    private static final DynamicUniformStorage<RectUniform> STORAGE =
        new DynamicUniformStorage<>("Aurora - RoundedRect UBO", UNIFORM_SIZE, 16);

    public static void draw(double x, double y, double w, double h, double radius, Color fill) {
        draw(x, y, w, h, radius, fill, null, 0.0);
    }

    public static void draw(double x, double y, double w, double h, double radius, Color fill, Color border, double borderWidth) {
        if (w <= 0 || h <= 0) return;

        float hw = (float) (w * 0.5);
        float hh = (float) (h * 0.5);
        float cx = (float) (x + hw);
        float cy = (float) (y + hh);
        float r = (float) Math.min(radius, Math.min(hw, hh));

        Color bc = border != null ? border : new Color(0, 0, 0, 0);
        GpuBufferSlice slice = STORAGE.writeUniform(new RectUniform(
            hw, hh, r, (float) Math.max(0.0, borderWidth),
            bc.r / 255f, bc.g / 255f, bc.b / 255f, bc.a / 255f
        ));

        MeshBuilder mb = new MeshBuilder(MeteorVertexFormats.POS2_TEXTURE_COLOR, VertexFormat.Mode.TRIANGLES);
        mb.begin();
        mb.ensureQuadCapacity();
        // UV slot carries local pos in pixels (relative to quad center)
        mb.quad(
            mb.vec2(cx - hw, cy - hh).vec2(-hw, -hh).color(fill).next(),
            mb.vec2(cx - hw, cy + hh).vec2(-hw,  hh).color(fill).next(),
            mb.vec2(cx + hw, cy + hh).vec2( hw,  hh).color(fill).next(),
            mb.vec2(cx + hw, cy - hh).vec2( hw, -hh).color(fill).next()
        );
        mb.end();

        MeshRenderer.begin()
            .attachments(Minecraft.getInstance().getMainRenderTarget())
            .pipeline(MeteorRenderPipelines.AURORA_ROUNDED_RECT)
            .mesh(mb)
            .uniform("AuroraRectData", slice)
            .end();
    }

    private RoundedRect() {}

    private record RectUniform(float halfX, float halfY, float radius, float borderWidth,
                               float br, float bg, float bb, float ba) implements DynamicUniformStorage.DynamicUniform {
        @Override
        public void write(ByteBuffer buffer) {
            Std140Builder.intoBuffer(buffer)
                .putVec2(halfX, halfY)
                .putFloat(radius)
                .putFloat(borderWidth)
                .putVec4(br, bg, bb, ba);
        }
    }
}

/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.renderer.primitives;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import meteordevelopment.meteorclient.renderer.FixedUniformStorage;
import meteordevelopment.meteorclient.renderer.MeshRenderer;
import meteordevelopment.meteorclient.renderer.MeteorRenderPipelines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DynamicUniformStorage;

import java.nio.ByteBuffer;

/**
 * Captures the main framebuffer into a downscaled FBO chain and applies a
 * dual-Kawase blur (reusing Meteor's existing BLUR_DOWN/BLUR_UP pipelines and
 * shaders). The blurred result lives in {@code blurred} and can be sampled by
 * Aurora panels.
 *
 * <p>This is intentionally a thin layer over the existing pipelines from
 * {@link MeteorRenderPipelines}; we don't duplicate the gaussian shaders.
 *
 * <p>Note: the {@code sampleInto} mask + tint variant requires an SDF-aware
 * sampling pipeline which has not yet been wired (the existing BLUR_PASSTHROUGH
 * is fullscreen only). Until that pipeline is added, {@code sampleInto} falls
 * back to a no-op so callers can be written ahead of time.
 */
public final class BackdropBlur {
    private static final int LEVELS = 3;
    private static final int UNIFORM_SIZE = new Std140SizeCalculator()
        .putVec2()    // halfTexelSize
        .putFloat()   // offset
        .get();

    private static GpuTextureView[] fbos;
    private static FixedUniformStorage<BlurUniformData> storage;
    private static GpuBufferSlice[] ubos;
    private static int lastWidth, lastHeight;
    private static boolean enabled = true;

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean v) {
        enabled = v;
    }

    /** Most recently produced blurred texture. */
    public static GpuTextureView blurred() {
        return fbos != null ? fbos[0] : null;
    }

    private static void ensureFbos() {
        int w = Minecraft.getInstance().getWindow().getWidth();
        int h = Minecraft.getInstance().getWindow().getHeight();
        if (fbos != null && lastWidth == w && lastHeight == h) return;

        if (fbos != null) {
            for (GpuTextureView v : fbos) if (v != null) v.close();
        }

        fbos = new GpuTextureView[LEVELS];
        for (int i = 0; i < LEVELS; i++) {
            double scale = 1 / Math.pow(2, i);
            int fw = Math.max(1, (int) (w * scale));
            int fh = Math.max(1, (int) (h * scale));
            fbos[i] = RenderSystem.getDevice().createTextureView(
                RenderSystem.getDevice().createTexture("Aurora - BackdropBlur " + i, 15, TextureFormat.RGBA8, fw, fh, 1, 1)
            );
        }
        lastWidth = w;
        lastHeight = h;

        // (Re)compute UBOs
        if (storage == null) {
            storage = new FixedUniformStorage<>("Aurora - BackdropBlur UBO", UNIFORM_SIZE, LEVELS);
        }
        storage.clear();
        BlurUniformData[] data = new BlurUniformData[LEVELS];
        float offset = 3.0f;
        for (int i = 0; i < LEVELS; i++) {
            data[i] = new BlurUniformData(0.5f / fbos[i].getWidth(0), 0.5f / fbos[i].getHeight(0), offset);
        }
        ubos = storage.writeAll(data);
    }

    /**
     * Downsample + upsample the main framebuffer into the chain. Safe to call once per GUI frame.
     */
    public static void captureAndBlur() {
        if (!enabled) return;
        ensureFbos();

        var mc = Minecraft.getInstance();
        GpuTextureView src = mc.getMainRenderTarget().getColorTextureView();

        // Downsample
        renderToFbo(fbos[0], src, MeteorRenderPipelines.BLUR_DOWN, ubos[0]);
        for (int i = 0; i < LEVELS - 1; i++) {
            renderToFbo(fbos[i + 1], fbos[i], MeteorRenderPipelines.BLUR_DOWN, ubos[i + 1]);
        }
        // Upsample
        for (int i = LEVELS - 1; i >= 1; i--) {
            renderToFbo(fbos[i - 1], fbos[i], MeteorRenderPipelines.BLUR_UP, ubos[i - 1]);
        }
    }

    private static void renderToFbo(GpuTextureView target, GpuTextureView source, com.mojang.blaze3d.pipeline.RenderPipeline pipeline, GpuBufferSlice ubo) {
        MeshRenderer.begin()
            .attachments(target, null)
            .pipeline(pipeline)
            .fullscreen()
            .uniform("BlurData", ubo)
            .sampler("u_Texture", source, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR))
            .end();
    }

    /**
     * Stub: renders the blurred backdrop into the given rect with the given mask
     * and tint. Requires an SDF-aware sampling pipeline (not yet wired); for now
     * this is a no-op so callers can be written ahead of time.
     */
    @SuppressWarnings("unused")
    public static void sampleInto(double x, double y, double w, double h, double radius, double tint) {
        // TODO: requires a dedicated pipeline that samples u_Backdrop with a UV-to-screen mapping
        // and applies the rounded SDF mask + darkening tint. Not yet implemented.
    }

    private BackdropBlur() {}

    private record BlurUniformData(float halfTexelSizeX, float halfTexelSizeY,
                                   float offset) implements DynamicUniformStorage.DynamicUniform {
        @Override
        public void write(ByteBuffer buffer) {
            Std140Builder.intoBuffer(buffer)
                .putVec2(halfTexelSizeX, halfTexelSizeY)
                .putFloat(offset);
        }
    }
}

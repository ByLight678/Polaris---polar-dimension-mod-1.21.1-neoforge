package com.josep.polardimension.client;

import com.josep.polardimension.PolarDimension;
import com.josep.polardimension.portal.PortalContent;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/** Camera-centered spherical curtains, drawn after stars and before terrain. Black adds no light. */
@EventBusSubscriber(modid = PolarDimension.MOD_ID, value = Dist.CLIENT)
public final class PolarisAuroraRenderer {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            PolarDimension.MOD_ID, "textures/environment/polaris_aurora.png");
    private static final int COLUMNS = 48;
    private static final int ROWS = 12;

    private PolarisAuroraRenderer() {}

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;
        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        var camera = event.getCamera();
        if (level == null || !level.dimension().equals(PortalContent.POLARIS)
                || camera.getFluidInCamera() != FogType.NONE || minecraft.gui.getBossOverlay().shouldCreateWorldFog()) return;
        if (camera.getEntity() instanceof LivingEntity living
                && (living.hasEffect(MobEffects.BLINDNESS) || living.hasEffect(MobEffects.DARKNESS))) return;

        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float time = (level.getGameTime() % 180000L) + partial;
        float weather = 1.0F - level.getRainLevel(partial) * 0.55F;
        float pulse = 0.85F + 0.15F * Mth.sin(time * 0.002F);
        PoseStack pose = new PoseStack();
        pose.mulPose(event.getModelViewMatrix());
        pose.mulPose(Axis.YP.rotationDegrees(time * 0.002F));

        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, TEXTURE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        try {
            BufferBuilder vertices = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            curtain(vertices, pose.last().pose(), -110.0F, 110.0F, 10.0F, 82.0F, weather * pulse * 0.9F);
            curtain(vertices, pose.last().pose(), 100.0F, 265.0F, 18.0F, 72.0F, weather * pulse * 0.65F);
            BufferUploader.drawWithShader(vertices.buildOrThrow());
        } finally {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            RenderSystem.enableCull();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
        }
    }

    private static void curtain(BufferBuilder vertices, Matrix4f matrix, float start, float end,
                                float bottom, float top, float opacity) {
        for (int column = 0; column < COLUMNS; column++) for (int row = 0; row < ROWS; row++) {
            float u0 = column / (float) COLUMNS, u1 = (column + 1) / (float) COLUMNS;
            float v0 = row / (float) ROWS, v1 = (row + 1) / (float) ROWS;
            vertex(vertices, matrix, u0, v0, start, end, bottom, top, opacity);
            vertex(vertices, matrix, u1, v0, start, end, bottom, top, opacity);
            vertex(vertices, matrix, u1, v1, start, end, bottom, top, opacity);
            vertex(vertices, matrix, u0, v1, start, end, bottom, top, opacity);
        }
    }

    private static void vertex(BufferBuilder vertices, Matrix4f matrix, float u, float v, float start,
                               float end, float bottom, float top, float opacity) {
        float longitude = Mth.lerp(u, start, end) * Mth.DEG_TO_RAD;
        float latitude = Mth.lerp(v, top, bottom) * Mth.DEG_TO_RAD;
        float radius = 100.0F;
        float ring = Mth.cos(latitude) * radius;
        // Geometry fades guarantee seamless edges even with a replacement resource-pack texture.
        float edge = (float) Math.pow(Math.max(0.0F, Mth.sin(u * Mth.PI) * Mth.sin(v * Mth.PI)), 0.35D);
        vertices.addVertex(matrix, Mth.sin(longitude) * ring, Mth.sin(latitude) * radius, -Mth.cos(longitude) * ring)
                .setUv(u, v).setColor(1.0F, 1.0F, 1.0F, opacity * edge);
    }
}

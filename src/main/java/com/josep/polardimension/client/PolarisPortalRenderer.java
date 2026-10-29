package com.josep.polardimension.client;

import com.josep.polardimension.PolarDimension;
import com.josep.polardimension.portal.PolarisPortalBlock;
import com.josep.polardimension.portal.PolarisPortalBlockEntity;
import com.josep.polardimension.portal.PortalContent;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.io.IOException;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.TheEndPortalRenderer;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.joml.Matrix4f;

@EventBusSubscriber(modid = PolarDimension.MOD_ID, value = Dist.CLIENT)
public final class PolarisPortalRenderer implements BlockEntityRenderer<PolarisPortalBlockEntity> {
    private static ShaderInstance shader;
    private static final RenderType PORTAL = RenderType.create("polaris_portal", DefaultVertexFormat.POSITION,
            VertexFormat.Mode.QUADS, 1536, false, false, RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(() -> shader))
                    .setTextureState(RenderStateShard.MultiTextureStateShard.builder()
                            .add(TheEndPortalRenderer.END_SKY_LOCATION, false, false)
                            .add(TheEndPortalRenderer.END_PORTAL_LOCATION, false, false).build())
                    .setCullState(RenderStateShard.NO_CULL).createCompositeState(false));

    public PolarisPortalRenderer(BlockEntityRendererProvider.Context context) {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(PortalContent.PORTAL_ENTITY.get(), PolarisPortalRenderer::new);
    }

    @SubscribeEvent
    public static void registerShader(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                ResourceLocation.fromNamespaceAndPath(PolarDimension.MOD_ID, "polaris_portal"),
                DefaultVertexFormat.POSITION), loaded -> shader = loaded);
    }

    @Override
    public void render(PolarisPortalBlockEntity entity, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        if (shader == null || entity.getLevel() == null) return;
        boolean alongX = entity.getBlockState().getValue(PolarisPortalBlock.AXIS) == Direction.Axis.X;
        float x0 = alongX ? 0 : 0.375F;
        float x1 = alongX ? 1 : 0.625F;
        float z0 = alongX ? 0.375F : 0;
        float z1 = alongX ? 0.625F : 1;
        Matrix4f matrix = pose.last().pose();
        VertexConsumer vertices = buffers.getBuffer(PORTAL);
        for (Direction face : Direction.values()) {
            if (entity.getLevel().getBlockState(entity.getBlockPos().relative(face)) == entity.getBlockState()) continue;
            switch (face) {
                case NORTH -> quad(vertices, matrix, x1,0,z0, x0,0,z0, x0,1,z0, x1,1,z0);
                case SOUTH -> quad(vertices, matrix, x0,0,z1, x1,0,z1, x1,1,z1, x0,1,z1);
                case WEST -> quad(vertices, matrix, x0,0,z0, x0,0,z1, x0,1,z1, x0,1,z0);
                case EAST -> quad(vertices, matrix, x1,0,z1, x1,0,z0, x1,1,z0, x1,1,z1);
                case DOWN -> quad(vertices, matrix, x0,0,z0, x1,0,z0, x1,0,z1, x0,0,z1);
                case UP -> quad(vertices, matrix, x0,1,z1, x1,1,z1, x1,1,z0, x0,1,z0);
            }
        }
    }

    private static void quad(VertexConsumer consumer, Matrix4f matrix,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3) {
        consumer.addVertex(matrix, x0, y0, z0);
        consumer.addVertex(matrix, x1, y1, z1);
        consumer.addVertex(matrix, x2, y2, z2);
        consumer.addVertex(matrix, x3, y3, z3);
    }
}


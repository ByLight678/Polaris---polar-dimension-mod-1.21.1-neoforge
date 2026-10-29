package com.josep.polardimension.client;

import com.josep.polardimension.PolarDimension;
import com.josep.polardimension.client.model.PenguinModel;
import com.josep.polardimension.entity.PenguinContent;
import com.josep.polardimension.entity.PenguinEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = PolarDimension.MOD_ID, value = Dist.CLIENT)
public final class PenguinRenderer extends MobRenderer<PenguinEntity, PenguinModel> {
    private static final ModelLayerLocation LAYER = new ModelLayerLocation(id("penguin"), "main");
    private static final ResourceLocation TEXTURE = id("textures/entity/penguin.png");

    public PenguinRenderer(EntityRendererProvider.Context context) {
        super(context, new PenguinModel(context.bakeLayer(LAYER)), 0.35F);
    }

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(PolarDimension.MOD_ID, path); }

    @Override
    public ResourceLocation getTextureLocation(PenguinEntity entity) { return TEXTURE; }

    @Override
    protected void scale(PenguinEntity entity, PoseStack pose, float partialTick) {
        if (entity.isBaby()) pose.scale(0.5F, 0.5F, 0.5F);
    }

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LAYER, PenguinModel::createLayer);
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(PenguinContent.PENGUIN.get(), PenguinRenderer::new);
    }
}

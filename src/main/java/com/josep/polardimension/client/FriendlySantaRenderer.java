package com.josep.polardimension.client;

import com.josep.polardimension.PolarDimension;
import com.josep.polardimension.client.model.FriendlySantaModel;
import com.josep.polardimension.entity.FriendlySantaContent;
import com.josep.polardimension.entity.FriendlySantaEntity;
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
public final class FriendlySantaRenderer extends MobRenderer<FriendlySantaEntity, FriendlySantaModel> {
    private static final ModelLayerLocation LAYER = new ModelLayerLocation(id("giant_santa"), "main");
    private static final ResourceLocation TEXTURE = id("textures/entity/giant_santa.png");

    public FriendlySantaRenderer(EntityRendererProvider.Context context) {
        super(context, new FriendlySantaModel(context.bakeLayer(LAYER)), 0.8F);
    }

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(PolarDimension.MOD_ID, path); }

    @Override
    public ResourceLocation getTextureLocation(FriendlySantaEntity entity) { return TEXTURE; }

    @Override
    protected void scale(FriendlySantaEntity entity, PoseStack pose, float partialTick) {
        if (entity.isBaby()) pose.scale(0.5F, 0.5F, 0.5F);
    }

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LAYER, FriendlySantaModel::createLayer);
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(FriendlySantaContent.SANTA.get(), FriendlySantaRenderer::new);
    }
}


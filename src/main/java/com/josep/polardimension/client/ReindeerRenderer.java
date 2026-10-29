package com.josep.polardimension.client;

import com.josep.polardimension.PolarDimension;
import com.josep.polardimension.client.model.ReindeerModel;
import com.josep.polardimension.entity.ReindeerContent;
import com.josep.polardimension.entity.ReindeerEntity;
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
public final class ReindeerRenderer extends MobRenderer<ReindeerEntity, ReindeerModel> {
    private static final ModelLayerLocation LAYER = new ModelLayerLocation(id("reindeer"), "main");
    private static final ResourceLocation TEXTURE = id("textures/entity/reindeer.png");

    public ReindeerRenderer(EntityRendererProvider.Context context) {
        super(context, new ReindeerModel(context.bakeLayer(LAYER)), 0.7F);
        addLayer(new ReindeerArmorLayer(this));
    }

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(PolarDimension.MOD_ID, path); }

    @Override
    public ResourceLocation getTextureLocation(ReindeerEntity entity) { return TEXTURE; }

    @Override
    protected void scale(ReindeerEntity entity, PoseStack pose, float partialTick) {
        if (entity.isBaby()) pose.scale(0.5F, 0.5F, 0.5F);
    }

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LAYER, ReindeerModel::createLayer);
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ReindeerContent.REINDEER.get(), ReindeerRenderer::new);
    }
}


package com.josep.polardimension.client;

import com.josep.polardimension.PolarDimension;
import com.josep.polardimension.client.model.EvilElfModel;
import com.josep.polardimension.entity.EvilElfContent;
import com.josep.polardimension.entity.EvilElfEntity;
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
public final class EvilElfRenderer extends MobRenderer<EvilElfEntity, EvilElfModel> {
    private static final ModelLayerLocation LAYER = new ModelLayerLocation(id("evil_christmas_elf"), "main");
    private static final ResourceLocation TEXTURE = id("textures/entity/evil_christmas_elf.png");

    public EvilElfRenderer(EntityRendererProvider.Context context) {
        super(context, new EvilElfModel(context.bakeLayer(LAYER)), 0.35F);
    }

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(PolarDimension.MOD_ID, path); }

    @Override
    public ResourceLocation getTextureLocation(EvilElfEntity entity) { return TEXTURE; }

    @Override
    protected void scale(EvilElfEntity entity, PoseStack pose, float partialTick) {
        if (entity.isBaby()) pose.scale(0.5F, 0.5F, 0.5F);
    }

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LAYER, EvilElfModel::createLayer);
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EvilElfContent.EVIL_ELF.get(), EvilElfRenderer::new);
    }
}


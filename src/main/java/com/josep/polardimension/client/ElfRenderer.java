package com.josep.polardimension.client;

import com.josep.polardimension.PolarDimension;
import com.josep.polardimension.client.model.ElfModel;
import com.josep.polardimension.entity.ElfContent;
import com.josep.polardimension.entity.ElfEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = PolarDimension.MOD_ID, value = Dist.CLIENT)
public final class ElfRenderer extends MobRenderer<ElfEntity, ElfModel> {
    public static final ModelLayerLocation MALE_LAYER = new ModelLayerLocation(id("christmas_elf"), "main");
    public static final ModelLayerLocation FEMALE_LAYER = new ModelLayerLocation(id("female_christmas_elf"), "main");
    private final ResourceLocation texture;

    public ElfRenderer(EntityRendererProvider.Context context, boolean female) {
        super(context, new ElfModel(context.bakeLayer(female ? FEMALE_LAYER : MALE_LAYER)), 0.3F);
        texture = id("textures/entity/" + (female ? "female_christmas_elf" : "christmas_elf") + ".png");
    }

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(PolarDimension.MOD_ID, path); }

    @Override
    public ResourceLocation getTextureLocation(ElfEntity elf) { return texture; }

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(MALE_LAYER, ElfModel::createMaleLayer);
        event.registerLayerDefinition(FEMALE_LAYER, ElfModel::createFemaleLayer);
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ElfContent.MALE_ELF.get(), context -> new ElfRenderer(context, false));
        event.registerEntityRenderer(ElfContent.FEMALE_ELF.get(), context -> new ElfRenderer(context, true));
    }
}

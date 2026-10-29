package com.josep.polardimension.client.model;

import com.josep.polardimension.PolarDimension;
import com.josep.polardimension.entity.SantaEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class SantaModel extends GeoModel<SantaEntity> {
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(PolarDimension.MOD_ID, path); }
    @Override public ResourceLocation getModelResource(SantaEntity santa) { return id("geo/disheveled_santa.geo.json"); }
    @Override public ResourceLocation getTextureResource(SantaEntity santa) { return id("textures/entity/disheveled_santa.png"); }
    @Override public ResourceLocation getAnimationResource(SantaEntity santa) { return id("animations/disheveled_santa.animation.json"); }
}

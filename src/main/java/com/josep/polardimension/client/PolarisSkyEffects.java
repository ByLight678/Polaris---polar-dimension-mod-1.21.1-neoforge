package com.josep.polardimension.client;

import com.josep.polardimension.PolarDimension;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;

/** Keep vanilla moon, stars, and night fog, with a cloudless view of the auroras. */
@EventBusSubscriber(modid = PolarDimension.MOD_ID, value = Dist.CLIENT)
public final class PolarisSkyEffects extends DimensionSpecialEffects.OverworldEffects {
    @Override
    public float getCloudHeight() { return Float.NaN; }

    @Override
    public float[] getSunriseColor(float timeOfDay, float partialTick) { return null; }

    @SubscribeEvent
    public static void register(RegisterDimensionSpecialEffectsEvent event) {
        event.register(ResourceLocation.fromNamespaceAndPath(PolarDimension.MOD_ID, "polaris"), new PolarisSkyEffects());
    }
}

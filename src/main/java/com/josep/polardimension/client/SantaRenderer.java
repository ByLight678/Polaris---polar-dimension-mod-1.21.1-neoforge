package com.josep.polardimension.client;

import com.josep.polardimension.PolarDimension;
import com.josep.polardimension.client.model.SantaModel;
import com.josep.polardimension.entity.SantaContent;
import com.josep.polardimension.entity.SantaEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

@EventBusSubscriber(modid = PolarDimension.MOD_ID, value = Dist.CLIENT)
public final class SantaRenderer extends GeoEntityRenderer<SantaEntity> {
    public SantaRenderer(EntityRendererProvider.Context context) {
        super(context, new SantaModel());
        shadowRadius = 0.7F;
    }
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(SantaContent.SANTA.get(), SantaRenderer::new);
    }
}

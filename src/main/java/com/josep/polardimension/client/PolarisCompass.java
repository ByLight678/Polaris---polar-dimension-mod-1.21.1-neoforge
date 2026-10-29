package com.josep.polardimension.client;

import com.josep.polardimension.PolarDimension;
import com.josep.polardimension.portal.PortalContent;
import com.josep.polardimension.village.VillageLayout;
import net.minecraft.client.renderer.item.CompassItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = PolarDimension.MOD_ID, value = Dist.CLIENT)
public final class PolarisCompass {
    private PolarisCompass() {}
    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            var angle = ResourceLocation.withDefaultNamespace("angle");
            var vanilla = ItemProperties.getProperty(new net.minecraft.world.item.ItemStack(Items.COMPASS), angle);
            var northPole = new CompassItemPropertyFunction((level, stack, entity) -> GlobalPos.of(
                    PortalContent.POLARIS, new BlockPos(VillageLayout.CENTER_X, 0, VillageLayout.CENTER_Z)));
            ItemProperties.register(Items.COMPASS, angle, (stack, level, entity, seed) -> {
                var owner = entity != null ? entity : stack.getEntityRepresentation();
                var world = level != null ? level : owner != null ? owner.level() : null;
                if (world != null && world.dimension().equals(PortalContent.POLARIS)
                        && !stack.has(DataComponents.LODESTONE_TRACKER)) {
                    return northPole.call(stack, level, entity, seed);
                }
                return vanilla == null ? 0 : vanilla.call(stack, level, entity, seed);
            });
        });
    }
}

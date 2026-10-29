package com.josep.polardimension.portal;

import com.josep.polardimension.PolarDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = PolarDimension.MOD_ID)
public final class PortalActivation {
    @SubscribeEvent
    public static void rightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (!event.getItemStack().is(Items.NETHER_STAR) || !PortalContent.isTravelDimension(level)
                || !level.getBlockState(event.getPos()).is(Blocks.BLUE_ICE) || event.getFace() == null) return;
        BlockPos inside = event.getPos().relative(event.getFace());
        if (!event.getEntity().mayUseItemAt(inside, event.getFace(), event.getItemStack())
                || !level.mayInteract(event.getEntity(), inside)) return;
        PolarisPortalShape.findEmptyPortalShape(level, inside, Direction.Axis.X).ifPresent(shape -> {
            if (!level.isClientSide) {
                shape.createPortalBlocks();
                if (!event.getEntity().getAbilities().instabuild) event.getItemStack().shrink(1);
                level.playSound(null, inside, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 0.6F, 1.2F);
            }
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
        });
    }
}

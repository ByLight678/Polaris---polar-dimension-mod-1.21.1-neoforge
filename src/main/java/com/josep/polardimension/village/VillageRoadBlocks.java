package com.josep.polardimension.village;

import com.josep.polardimension.PolarDimension;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Visually ordinary paving with enough block light to keep snowfall off the walking surface. */
public final class VillageRoadBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(PolarDimension.MOD_ID);
    public static final DeferredBlock<Block> WARM_STONE_BRICKS = BLOCKS.register("warm_stone_bricks", () -> new Block(
            BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS).lootFrom(() -> Blocks.STONE_BRICKS).lightLevel(state -> 15)));
    public static final DeferredBlock<Block> WARM_COBBLESTONE = BLOCKS.register("warm_cobblestone", () -> new Block(
            BlockBehaviour.Properties.ofFullCopy(Blocks.COBBLESTONE).lootFrom(() -> Blocks.COBBLESTONE).lightLevel(state -> 15)));

    private VillageRoadBlocks() {}

    public static void register(IEventBus bus) { BLOCKS.register(bus); }

    public static boolean isWarm(BlockState state) { return state.is(WARM_STONE_BRICKS.get()) || state.is(WARM_COBBLESTONE.get()); }

    public static BlockState warm(BlockState state) {
        if (state.is(Blocks.STONE_BRICKS)) return WARM_STONE_BRICKS.get().defaultBlockState();
        if (state.is(Blocks.COBBLESTONE)) return WARM_COBBLESTONE.get().defaultBlockState();
        return state;
    }
}

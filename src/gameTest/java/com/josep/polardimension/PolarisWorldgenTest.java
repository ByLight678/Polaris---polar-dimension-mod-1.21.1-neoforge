package com.josep.polardimension;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(PolarDimension.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PolarisWorldgenTest {
    @GameTest(template = "empty", timeoutTicks = 600)
    public static void frozenLayersAndOreWhitelist(GameTestHelper helper) {
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION,
                ResourceLocation.fromNamespaceAndPath(PolarDimension.MOD_ID, "polaris"));
        ServerLevel polaris = helper.getLevel().getServer().getLevel(key);
        helper.assertTrue(polaris != null, "Polaris dimension must load");
        int columns = 0;
        int iron = 0;
        int gold = 0;
        int minSurface = Integer.MAX_VALUE;
        int maxSurface = Integer.MIN_VALUE;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int[] sample : new int[][] {{0, 0}, {8, -8}, {-16, 12}, {32, 32}}) {
            LevelChunk chunk = polaris.getChunk(sample[0], sample[1]);
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    int wx = chunk.getPos().getMinBlockX() + x;
                    int wz = chunk.getPos().getMinBlockZ() + z;
                    int y = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z);
                    while (chunk.getBlockState(pos.set(wx, y, wz)).is(Blocks.SNOW)) {
                        y--;
                    }
                    minSurface = Math.min(minSurface, y);
                    maxSurface = Math.max(maxSurface, y);
                    helper.assertTrue(chunk.getBlockState(pos.set(wx, y, wz)).is(Blocks.SNOW_BLOCK),
                            "Ground must be snow at " + pos);
                    int snow = 0;
                    while (chunk.getBlockState(pos.set(wx, y - snow, wz)).is(Blocks.SNOW_BLOCK)) {
                        snow++;
                    }
                    helper.assertTrue(snow >= 3 && snow <= 5, "Snow depth must be 3-5 at " + pos + ": " + snow);
                    int depth = snow;
                    while (chunk.getBlockState(pos.set(wx, y - depth, wz)).is(Blocks.PACKED_ICE)) {
                        depth++;
                    }
                    helper.assertTrue(depth >= 10 && depth <= 15,
                            "Deepslate must start at depth 10-15 at " + pos + ": " + depth);
                    BlockState boundary = chunk.getBlockState(pos.set(wx, y - depth, wz));
                    helper.assertTrue(boundary.is(Blocks.DEEPSLATE) || boundary.isAir()
                                    || !boundary.getFluidState().isEmpty()
                                    || boundary.is(Blocks.DEEPSLATE_IRON_ORE) || boundary.is(Blocks.DEEPSLATE_GOLD_ORE)
                                    || boundary.is(Blocks.TUFF) || boundary.is(Blocks.RAW_IRON_BLOCK),
                            "Packed ice must transition to deepslate or an underground cave at " + pos + ": " + boundary);
                    columns++;
                    for (int underground = chunk.getMinBuildHeight(); underground <= y; underground++) {
                        BlockState state = chunk.getBlockState(pos.set(wx, underground, wz));
                        boolean ironOre = state.is(Blocks.DEEPSLATE_IRON_ORE) || state.is(Blocks.IRON_ORE);
                        boolean goldOre = state.is(Blocks.DEEPSLATE_GOLD_ORE) || state.is(Blocks.GOLD_ORE);
                        if (ironOre) iron++;
                        if (goldOre) gold++;
                        helper.assertTrue(!state.is(BlockTags.COAL_ORES) && !state.is(BlockTags.COPPER_ORES)
                                        && !state.is(BlockTags.DIAMOND_ORES) && !state.is(BlockTags.EMERALD_ORES)
                                        && !state.is(BlockTags.LAPIS_ORES) && !state.is(BlockTags.REDSTONE_ORES),
                                "Unexpected ore at " + pos + ": " + state);
                    }
                }
            }
        }
        helper.assertTrue(iron > 0 && gold > 0, "Sample must contain both iron and gold");
        helper.assertTrue(minSurface >= 63 && maxSurface < 110,
                "Expected rolling plains terrain, got Y=" + minSurface + ".." + maxSurface);
        PolarDimension.LOGGER.info("Polaris validated: {} columns, Y={}..{}, iron={}, gold={}",
                columns, minSurface, maxSurface, iron, gold);
        helper.succeed();
    }
}


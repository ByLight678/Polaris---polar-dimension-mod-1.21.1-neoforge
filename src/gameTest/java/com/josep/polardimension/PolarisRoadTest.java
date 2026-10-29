package com.josep.polardimension;

import com.josep.polardimension.portal.PortalContent;
import com.josep.polardimension.village.VillageLayout;
import com.josep.polardimension.village.VillageRoadBlocks;
import com.josep.polardimension.village.VillageRoadMaintenance;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkEvent;

@GameTestHolder(PolarDimension.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PolarisRoadTest {
    @GameTest(template = "empty", batch = "road_lighting", timeoutTicks = 400)
    public static void legacyRoadCleanupSnowMeltAndLanterns(GameTestHelper helper) {
        ServerLevel polaris = helper.getLevel().getServer().getLevel(PortalContent.POLARIS);
        helper.assertTrue(polaris != null, "Polaris must load");
        int surface = VillageLayout.surfaceY(polaris.getChunkSource().getGenerator(), polaris, polaris.getChunkSource().randomState());
        BlockPos road = new BlockPos(20, surface, -1000);
        var chunk = polaris.getChunk(road.getX() >> 4, road.getZ() >> 4);
        polaris.setChunkForced(chunk.getPos().x, chunk.getPos().z, true);
        // Recreate an old, snowy road in the existing world; no fresh village is required.
        polaris.setBlockAndUpdate(road, Blocks.STONE_BRICKS.defaultBlockState());
        polaris.setBlockAndUpdate(road.above(), Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 4));
        BlockPos lampBase = new BlockPos(21, surface, -1001);
        polaris.setBlockAndUpdate(lampBase, Blocks.SNOW_BLOCK.defaultBlockState());
        polaris.setBlockAndUpdate(lampBase.above(), Blocks.AIR.defaultBlockState());
        polaris.setBlockAndUpdate(lampBase.above(2), Blocks.AIR.defaultBlockState());
        BlockPos decorated = new BlockPos(21, surface, -1000);
        polaris.setBlockAndUpdate(decorated, Blocks.COBBLESTONE.defaultBlockState());
        polaris.setBlockAndUpdate(decorated.above(), Blocks.RED_CARPET.defaultBlockState());
        BlockPos unpaved = new BlockPos(22, surface, -994);
        polaris.setBlockAndUpdate(unpaved, Blocks.SNOW_BLOCK.defaultBlockState());
        polaris.setBlockAndUpdate(unpaved.above(), Blocks.SNOW.defaultBlockState());
        VillageRoadMaintenance.upgradeChunk(polaris, chunk);
        helper.assertTrue(polaris.getBlockState(road).is(VillageRoadBlocks.WARM_STONE_BRICKS.get()), "Old stone-brick roads must upgrade");
        helper.assertTrue(polaris.getBlockState(road.above()).isAir(), "Existing thin snow must clear immediately");
        helper.assertTrue(polaris.getBlockState(decorated).is(VillageRoadBlocks.WARM_COBBLESTONE.get())
                        && polaris.getBlockState(decorated.above()).is(Blocks.RED_CARPET), "Player decoration must be preserved");
        helper.assertTrue(polaris.getBlockState(unpaved).is(Blocks.SNOW_BLOCK), "Full snowy ground must remain unchanged");
        helper.assertTrue(polaris.getBlockState(lampBase.above()).is(Blocks.SPRUCE_FENCE)
                        && polaris.getBlockState(lampBase.above(2)).is(Blocks.LANTERN), "Roadside lantern post must have a supported light");
        helper.assertTrue(VillageRoadMaintenance.upgradeChunk(polaris, chunk) == 0, "Road upgrade must be idempotent");
        helper.assertTrue(VillageRoadBlocks.WARM_STONE_BRICKS.get().getLootTable().equals(Blocks.STONE_BRICKS.getLootTable())
                        && VillageRoadBlocks.WARM_COBBLESTONE.get().getLootTable().equals(Blocks.COBBLESTONE.getLootTable()),
                "Warm paving must retain ordinary paving drops");
        helper.assertTrue(polaris.getBlockState(road).is(BlockTags.MINEABLE_WITH_PICKAXE), "Warm paving must remain pickaxe-minable");
        ServerLevel overworld = helper.getLevel();
        var otherChunk = overworld.getChunk(road.getX() >> 4, road.getZ() >> 4);
        overworld.setBlockAndUpdate(road, Blocks.STONE_BRICKS.defaultBlockState());
        overworld.setBlockAndUpdate(road.above(), Blocks.SNOW.defaultBlockState());
        VillageRoadMaintenance.upgradeChunk(overworld, otherChunk);
        helper.assertTrue(overworld.getBlockState(road).is(Blocks.STONE_BRICKS)
                        && overworld.getBlockState(road.above()).is(Blocks.SNOW), "Overworld roads and snow must remain unchanged");
        BlockPos loadedRoad = new BlockPos(19, surface, -1000);
        polaris.setBlockAndUpdate(loadedRoad, Blocks.STONE_BRICKS.defaultBlockState());
        polaris.setBlockAndUpdate(loadedRoad.above(), Blocks.SNOW.defaultBlockState());
        NeoForge.EVENT_BUS.post(new ChunkEvent.Load(chunk, false));
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(polaris.getBlockState(loadedRoad).is(VillageRoadBlocks.WARM_STONE_BRICKS.get())
                            && polaris.getBlockState(loadedRoad.above()).isAir(), "Chunk-load event must repair existing roads automatically");
            BlockPos snow = road.above();
            helper.assertTrue(polaris.getBrightness(LightLayer.BLOCK, snow) > 11,
                    "Road light must exceed vanilla snow-melting threshold");
            helper.assertTrue(!polaris.getBiome(snow).value().shouldSnow(polaris, snow), "Snowfall must not accumulate on the road");
            polaris.setBlockAndUpdate(snow, Blocks.SNOW.defaultBlockState());
            polaris.getBlockState(snow).randomTick(polaris, snow, RandomSource.create());
            helper.assertTrue(polaris.getBlockState(snow).isAir(), "Vanilla snow random ticks must melt new snow on lit paving");
            helper.assertTrue(polaris.getBlockState(unpaved).is(Blocks.SNOW_BLOCK), "Nearby full snow blocks must survive road lighting");
            PolarDimension.LOGGER.info("Road lighting validated: legacy snow cleared, vanilla melt and snowfall rules, lanterns, unchanged decorations/drops, Polaris-only scope");
            helper.succeed();
        });
    }
}

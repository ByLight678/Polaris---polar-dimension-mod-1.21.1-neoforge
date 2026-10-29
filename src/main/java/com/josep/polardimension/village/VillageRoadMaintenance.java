package com.josep.polardimension.village;

import com.josep.polardimension.PolarDimension;
import com.josep.polardimension.portal.PortalContent;
import java.util.Queue;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Repairs loaded village paving once per load; never scans unloaded chunks or every road every tick. */
@EventBusSubscriber(modid = PolarDimension.MOD_ID)
public final class VillageRoadMaintenance {
    private record PendingChunk(LevelChunk chunk, int attempts) {}
    private static final Queue<PendingChunk> PENDING = new ConcurrentLinkedQueue<>();
    private static final WeakHashMap<ServerLevel, Integer> SURFACE_HEIGHTS = new WeakHashMap<>();

    private VillageRoadMaintenance() {}

    @SubscribeEvent
    public static void chunkLoaded(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension().equals(PortalContent.POLARIS)
                && event.getChunk() instanceof LevelChunk chunk && intersectsVillage(chunk)) PENDING.add(new PendingChunk(chunk, 0));
    }

    private static boolean intersectsVillage(LevelChunk chunk) {
        return chunk.getPos().getMaxBlockX() >= VillageLayout.CENTER_X - VillageLayout.HALF_SIZE
                && chunk.getPos().getMinBlockX() <= VillageLayout.CENTER_X + VillageLayout.HALF_SIZE
                && chunk.getPos().getMaxBlockZ() >= VillageLayout.CENTER_Z - VillageLayout.HALF_SIZE
                && chunk.getPos().getMinBlockZ() <= VillageLayout.CENTER_Z + VillageLayout.HALF_SIZE;
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post event) {
        int work = Math.min(8, PENDING.size());
        for (int budget = 0; budget < work; budget++) {
            PendingChunk pending = PENDING.poll();
            if (pending == null) return;
            LevelChunk chunk = pending.chunk();
            if (chunk.getLevel() instanceof ServerLevel level && level.getServer() == event.getServer()) {
                LevelChunk loaded = level.getChunkSource().getChunkNow(chunk.getPos().x, chunk.getPos().z);
                if (loaded == chunk) upgradeChunk(level, chunk);
                // Load events can precede full chunk availability. Retry without loading neighbors.
                else if (loaded == null && pending.attempts() < 40) PENDING.add(new PendingChunk(chunk, pending.attempts() + 1));
            }
        }
    }

    public static int upgradeChunk(ServerLevel level, LevelChunk chunk) {
        if (!level.dimension().equals(PortalContent.POLARIS) || !intersectsVillage(chunk)) return 0;
        int surface = SURFACE_HEIGHTS.computeIfAbsent(level, current -> VillageLayout.surfaceY(
                current.getChunkSource().getGenerator(), current, current.getChunkSource().randomState()));
        int upgraded = 0;
        for (int x = chunk.getPos().getMinBlockX(); x <= chunk.getPos().getMaxBlockX(); x++) {
            for (int z = chunk.getPos().getMinBlockZ(); z <= chunk.getPos().getMaxBlockZ(); z++) {
                if (VillageLayout.distance(x, z) > VillageLayout.HALF_SIZE) continue;
                BlockPos pos = new BlockPos(x, surface, z);
                BlockState original = chunk.getBlockState(pos);
                boolean street = VillageLayout.isRoad(x - VillageLayout.CENTER_X, z - VillageLayout.CENTER_Z);
                // Template entrance paths and plaza paving are included only when exposed to the sky.
                if (!street && !level.canSeeSky(pos.above())) continue;
                BlockState warm = VillageRoadBlocks.warm(original);
                if (warm != original) {
                    level.setBlockAndUpdate(pos, warm);
                    upgraded++;
                }
                if (VillageRoadBlocks.isWarm(warm) && chunk.getBlockState(pos.above()).is(Blocks.SNOW)) {
                    level.removeBlock(pos.above(), false);
                }
            }
        }
        addLanterns(level, chunk, surface);
        return upgraded;
    }

    private static void addLanterns(ServerLevel level, LevelChunk chunk, int surface) {
        for (int x = chunk.getPos().getMinBlockX(); x <= chunk.getPos().getMaxBlockX(); x++) {
            for (int z = chunk.getPos().getMinBlockZ(); z <= chunk.getPos().getMaxBlockZ(); z++) {
                int localX = x - VillageLayout.CENTER_X;
                int localZ = z - VillageLayout.CENTER_Z;
                if (VillageLayout.distance(x, z) > VillageLayout.HALF_SIZE
                        || VillageLayout.insidePlot(localX, localZ) || VillageLayout.isRoad(localX, localZ)) continue;
                if (!VillageLayout.isLanternSpot(localX, localZ)) continue;
                BlockPos base = new BlockPos(x, surface, z);
                if (!chunk.getBlockState(base).is(Blocks.SNOW_BLOCK)) continue;
                BlockState lower = chunk.getBlockState(base.above());
                if (!(lower.isAir() || lower.is(Blocks.SNOW)) || !chunk.getBlockState(base.above(2)).isAir()) continue;
                boolean nearPaving = false;
                for (int dx = -4; dx <= 4 && !nearPaving; dx++) for (int dz = -4; dz <= 4; dz++) {
                    if (Math.abs(dx) + Math.abs(dz) > 4) continue;
                    int px = x + dx, pz = z + dz;
                    if ((px >> 4) == chunk.getPos().x && (pz >> 4) == chunk.getPos().z
                            && VillageRoadBlocks.isWarm(chunk.getBlockState(new BlockPos(px, surface, pz)))) nearPaving = true;
                }
                if (nearPaving) {
                    level.setBlockAndUpdate(base.above(), Blocks.SPRUCE_FENCE.defaultBlockState());
                    level.setBlockAndUpdate(base.above(2), Blocks.LANTERN.defaultBlockState());
                }
            }
        }
    }
}


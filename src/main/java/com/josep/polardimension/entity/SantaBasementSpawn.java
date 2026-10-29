package com.josep.polardimension.entity;

import com.josep.polardimension.PolarDimension;
import com.josep.polardimension.portal.PortalContent;
import com.josep.polardimension.village.NorthPolePlacement;
import com.josep.polardimension.village.VillageBuildingPiece;
import com.josep.polardimension.village.VillageLayout;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = PolarDimension.MOD_ID)
public final class SantaBasementSpawn {
    private SantaBasementSpawn() {}

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 40 != 0) return;
        ServerLevel level = event.getServer().getLevel(PortalContent.POLARIS);
        if (level == null || level.players().stream().noneMatch(player ->
                VillageLayout.distance(player.getBlockX(), player.getBlockZ()) <= VillageLayout.BLEND_EDGE + 32)) return;
        ensureSpawned(level);
        FriendlySantaSpawn.ensureSpawned(level);
    }

    /** Returns the newly spawned encounter, or null when already recorded/not yet safely placeable. */
    public static SantaEntity ensureSpawned(ServerLevel level) {
        if (!level.dimension().equals(PortalContent.POLARIS) || SantaBossData.get(level).hasSpawned()) return null;
        var structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                .get(ResourceLocation.fromNamespaceAndPath(PolarDimension.MOD_ID, "north_pole_village"));
        if (structure == null) return null;
        // Read the saved structure's actual workshop position, including older village layouts.
        var anchor = level.getChunk(NorthPolePlacement.START_CHUNK.x, NorthPolePlacement.START_CHUNK.z);
        var start = anchor.getStartForStructure(structure);
        if (start == null || !start.isValid()) return null;
        for (var piece : start.getPieces()) {
            if (!(piece instanceof VillageBuildingPiece building)) continue;
            var spawn = building.workshopBossSpawn();
            if (spawn.isEmpty()) continue;
            var pos = spawn.get();
            if (level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) == null
                    || !level.getBlockState(pos.below()).is(Blocks.SPRUCE_PLANKS)) return null;
            SantaEntity santa = SantaContent.SANTA.get().create(level);
            if (santa == null) return null;
            santa.setUUID(java.util.UUID.nameUUIDFromBytes(("polar_dimension:workshop_santa:" + level.getSeed() + ":" + pos.asLong())
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            santa.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 180.0F, 0.0F);
            santa.bindToBasement(pos);
            if (!level.noCollision(santa)) return null;
            santa.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
            if (level.addFreshEntity(santa)) {
                SantaBossData.get(level).recordSpawn(santa.getUUID());
                return santa;
            }
        }
        return null;
    }
}

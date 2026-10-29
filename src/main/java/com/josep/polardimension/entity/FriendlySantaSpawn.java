package com.josep.polardimension.entity;

import com.josep.polardimension.PolarDimension;
import com.josep.polardimension.portal.PortalContent;
import com.josep.polardimension.village.NorthPolePlacement;
import com.josep.polardimension.village.VillageBuildingPiece;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

@EventBusSubscriber(modid = PolarDimension.MOD_ID)
public final class FriendlySantaSpawn {
    private FriendlySantaSpawn() {}
    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
    public static void bossDied(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof SantaEntity boss) || !(boss.level() instanceof ServerLevel level)
                || !level.dimension().equals(PortalContent.POLARIS)) return;
        var data = SantaBossData.get(level);
        if (!boss.getUUID().equals(data.getBossId())) return;
        data.recordDefeat();
        ensureSpawned(level);
    }
    public static FriendlySantaEntity ensureSpawned(ServerLevel level) {
        if (!level.dimension().equals(PortalContent.POLARIS)) return null;
        var data = SantaBossData.get(level);
        if (!data.isDefeated() || data.getFriendlySantaId() != null) return null;
        var structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                .get(ResourceLocation.fromNamespaceAndPath(PolarDimension.MOD_ID, "north_pole_village"));
        if (structure == null) return null;
        var chunk = level.getChunkSource().getChunkNow(NorthPolePlacement.START_CHUNK.x, NorthPolePlacement.START_CHUNK.z);
        if (chunk == null) return null;
        var start = chunk.getStartForStructure(structure);
        if (start == null || !start.isValid()) return null;
        for (var piece : start.getPieces()) {
            if (!(piece instanceof VillageBuildingPiece building)) continue;
            var anchor = building.treeSantaSpawn();
            if (anchor.isEmpty()) continue;
            for (int radius = 0; radius <= 5; radius++) for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    for (int dy = 2; dy >= -3; dy--) {
                        var pos = anchor.get().offset(dx, dy, dz);
                        if (level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) == null
                                || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) continue;
                        var santa = FriendlySantaContent.SANTA.get().create(level);
                        if (santa == null) return null;
                        santa.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 180, 0);
                        if (!level.noCollision(santa) || level.containsAnyLiquid(santa.getBoundingBox())) continue;
                        santa.bindToTree(pos);
                        santa.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
                        if (level.addFreshEntity(santa)) {
                            data.recordFriendlySanta(santa.getUUID());
                            return santa;
                        }
                    }
                }
            }
        }
        return null;
    }
}

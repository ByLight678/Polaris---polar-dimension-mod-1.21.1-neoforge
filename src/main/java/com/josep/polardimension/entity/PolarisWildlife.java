package com.josep.polardimension.entity;

import com.josep.polardimension.PolarDimension;
import com.josep.polardimension.portal.PortalContent;
import com.josep.polardimension.village.VillageLayout;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerSpawnPhantomsEvent;

/** Natural wildlife stays outside the settlement; structure residents and player spawns are untouched. */
@EventBusSubscriber(modid = PolarDimension.MOD_ID)
public final class PolarisWildlife {
    private PolarisWildlife() {}

    @SubscribeEvent
    public static void phantoms(PlayerSpawnPhantomsEvent event) {
        if (event.getEntity().level().dimension().equals(PortalContent.POLARIS)) {
            event.setResult(PlayerSpawnPhantomsEvent.Result.DENY);
        }
    }

    @SubscribeEvent
    public static void position(MobSpawnEvent.PositionCheck event) {
        if (!event.getLevel().getLevel().dimension().equals(PortalContent.POLARIS)
                || (event.getSpawnType() != MobSpawnType.NATURAL && event.getSpawnType() != MobSpawnType.CHUNK_GENERATION)) return;
        var mob = event.getEntity();
        if (mob.getType() != PenguinContent.PENGUIN.get() && mob.getType() != EntityType.POLAR_BEAR) return;
        if (VillageLayout.distance(mob.getBlockX(), mob.getBlockZ()) <= VillageLayout.HALF_SIZE) return;
        // Animal's generic walk-target score penalizes night on non-grass. Keep arctic wildlife
        // spawning under the fixed midnight sky, with the same surface/light/obstruction checks.
        if (PenguinEntity.checkSpawnRules(PenguinContent.PENGUIN.get(), event.getLevel(), event.getSpawnType(),
                mob.blockPosition(), mob.getRandom()) && mob.checkSpawnObstruction(event.getLevel())) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.SUCCEED);
        }
    }

    @SubscribeEvent
    public static void placement(MobSpawnEvent.SpawnPlacementCheck event) {
        if (!event.getLevel().getLevel().dimension().equals(PortalContent.POLARIS)
                || (event.getSpawnType() != MobSpawnType.NATURAL && event.getSpawnType() != MobSpawnType.CHUNK_GENERATION)) return;
        MobCategory category = event.getEntityType().getCategory();
        if (category == MobCategory.MONSTER) {
            if (event.getEntityType() != EntityType.STRAY && event.getEntityType() != EvilSnowmanContent.EVIL_SNOWMAN.get()) {
                event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.FAIL);
            }
            return;
        }
        if (category == MobCategory.MISC) return;
        boolean allowed = event.getEntityType() == PenguinContent.PENGUIN.get() || event.getEntityType() == EntityType.POLAR_BEAR;
        if (!allowed || VillageLayout.distance(event.getPos().getX(), event.getPos().getZ()) <= VillageLayout.HALF_SIZE) {
            event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.FAIL);
        }
    }
}

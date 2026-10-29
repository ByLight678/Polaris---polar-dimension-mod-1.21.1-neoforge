package com.josep.polardimension.entity;

import com.josep.polardimension.PolarDimension;
import com.josep.polardimension.portal.PortalContent;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.Horse;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Replaces vanilla horses in Polaris only, retaining their saved horse data. */
@EventBusSubscriber(modid = PolarDimension.MOD_ID)
public final class PolarisReindeerMigration {
    private static final Queue<Horse> PENDING = new ConcurrentLinkedQueue<>();

    private PolarisReindeerMigration() {}

    @SubscribeEvent
    public static void entityJoined(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension().equals(PortalContent.POLARIS)
                && event.getEntity() instanceof Horse horse && horse.getType() == EntityType.HORSE) {
            PENDING.add(horse);
        }
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post event) {
        Horse horse;
        while ((horse = PENDING.poll()) != null) {
            if (horse.isRemoved() || !horse.isAlive() || !(horse.level() instanceof ServerLevel level)
                    || level.getServer() != event.getServer() || !level.dimension().equals(PortalContent.POLARIS)) continue;
            ReindeerEntity reindeer = ReindeerContent.REINDEER.get().create(level);
            if (reindeer == null) continue;
            CompoundTag saved = new CompoundTag();
            horse.saveWithoutId(saved);
            reindeer.load(saved);
            // The source is kept alive until insertion succeeds, so the replacement needs a fresh UUID.
            reindeer.setUUID(UUID.randomUUID());
            List<Entity> passengers = List.copyOf(horse.getPassengers());
            if (level.addFreshEntity(reindeer)) {
                for (Entity passenger : passengers) {
                    passenger.stopRiding();
                    passenger.startRiding(reindeer, true);
                }
                if (horse.isLeashed() && horse.getLeashHolder() != null) {
                    Entity holder = horse.getLeashHolder();
                    horse.dropLeash(false, false);
                    reindeer.setLeashedTo(holder, true);
                }
                horse.discard();
            }
        }
    }
}

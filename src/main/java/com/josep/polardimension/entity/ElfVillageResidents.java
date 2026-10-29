package com.josep.polardimension.entity;

import com.josep.polardimension.PolarDimension;
import com.josep.polardimension.portal.PortalContent;
import com.josep.polardimension.village.VillageLayout;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Converts only villagers inside the fixed Polaris village, including residents from older saves. */
@EventBusSubscriber(modid = PolarDimension.MOD_ID)
public final class ElfVillageResidents {
    private static final Queue<Villager> PENDING = new ConcurrentLinkedQueue<>();

    private ElfVillageResidents() {}

    public static boolean isVillagePosition(ServerLevel level, net.minecraft.core.BlockPos pos) {
        return level.dimension().equals(PortalContent.POLARIS)
                && VillageLayout.distance(pos.getX(), pos.getZ()) <= VillageLayout.HALF_SIZE;
    }

    @SubscribeEvent
    public static void entityJoined(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (event.getEntity() instanceof ElfEntity elf) {
            if (isVillagePosition(level, elf.blockPosition()) && elf.getVillageHome() == null) {
                elf.bindToVillage(elf.blockPosition());
            } else if (!level.dimension().equals(PortalContent.POLARIS)) {
                // A resident taken through a portal must not navigate toward its old coordinates in another world.
                elf.leaveVillage();
            }
        } else if (event.getEntity() instanceof Villager villager && villager.getType() == EntityType.VILLAGER
                && isVillagePosition(level, villager.blockPosition())) {
            // Defer spawning: entity-load events can run before the chunk is fully ready.
            PENDING.add(villager);
        }
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post event) {
        Villager villager;
        while ((villager = PENDING.poll()) != null) {
            if (villager.isRemoved() || !villager.isAlive() || !(villager.level() instanceof ServerLevel level)
                    || level.getServer() != event.getServer() || !isVillagePosition(level, villager.blockPosition())) continue;
            boolean female = (villager.getUUID().getLeastSignificantBits() & 1L) != 0;
            ElfEntity elf = (female ? ElfContent.FEMALE_ELF : ElfContent.MALE_ELF).get().create(level);
            if (elf == null) continue;
            elf.moveTo(villager.getX(), villager.getY(), villager.getZ(), villager.getYRot(), villager.getXRot());
            elf.bindToVillage(villager.blockPosition());
            elf.setCustomName(villager.getCustomName());
            elf.setCustomNameVisible(villager.isCustomNameVisible());
            elf.setNoAi(villager.isNoAi());
            elf.setInvulnerable(villager.isInvulnerable());
            elf.setSilent(villager.isSilent());
            villager.getTags().forEach(elf::addTag);
            elf.setHealth(Math.max(1.0F, elf.getMaxHealth() * villager.getHealth() / villager.getMaxHealth()));
            if (level.addFreshEntity(elf)) villager.discard();
        }
    }
}


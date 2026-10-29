package com.josep.polardimension;

import com.josep.polardimension.entity.ElfContent;
import com.josep.polardimension.entity.ElfEntity;
import com.josep.polardimension.portal.PortalContent;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(PolarDimension.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PolarisElfTest {
    @GameTest(template = "empty", batch = "elf_migration", timeoutTicks = 400)
    public static void residentsPersistenceAndScopedMigration(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel();
        ServerLevel polaris = overworld.getServer().getLevel(PortalContent.POLARIS);
        helper.assertTrue(polaris != null, "Polaris must load");
        polaris.getChunk(0, -970 >> 4);
        polaris.getChunk(180 >> 4, -1000 >> 4);
        BlockPos inside = new BlockPos(0, polaris.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 0, -970), -970);
        BlockPos outside = new BlockPos(180, polaris.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 180, -1000), -1000);
        polaris.setChunkForced(inside.getX() >> 4, inside.getZ() >> 4, true);
        polaris.setChunkForced(outside.getX() >> 4, outside.getZ() >> 4, true);
        Villager legacy = EntityType.VILLAGER.create(polaris);
        Villager outsideVillager = EntityType.VILLAGER.create(polaris);
        Villager overworldVillager = EntityType.VILLAGER.create(overworld);
        helper.assertTrue(legacy != null && outsideVillager != null && overworldVillager != null, "Test villagers must exist");
        legacy.setUUID(new UUID(748312, 2));
        legacy.moveTo(inside.getX() + 0.5D, inside.getY(), inside.getZ() + 0.5D);
        legacy.setCustomName(Component.literal("Legacy Polaris Resident"));
        legacy.setNoAi(true);
        legacy.setHealth(10.0F);
        polaris.addFreshEntity(legacy);
        outsideVillager.moveTo(outside.getX() + 0.5D, outside.getY(), outside.getZ() + 0.5D);
        outsideVillager.setNoAi(true);
        polaris.addFreshEntity(outsideVillager);
        // Same X/Z coordinates in the Overworld must not trigger conversion.
        overworldVillager.moveTo(inside.getX() + 0.5D, -60, inside.getZ() + 0.5D);
        overworldVillager.setNoAi(true);
        overworld.setChunkForced(inside.getX() >> 4, inside.getZ() >> 4, true);
        overworld.addFreshEntity(overworldVillager);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(legacy.isRemoved(), "Existing Polaris village villager must convert");
            var matches = polaris.getEntitiesOfClass(ElfEntity.class, new AABB(inside).inflate(4),
                    elf -> elf.hasCustomName() && elf.getCustomName().getString().equals("Legacy Polaris Resident"));
            helper.assertTrue(matches.size() == 1, "Migration must produce exactly one elf");
            ElfEntity elf = matches.getFirst();
            helper.assertTrue(!elf.isFemale() && elf.isNoAi() && elf.getHealth() == 10.0F,
                    "Migration must preserve name, AI setting, health, and select a stable variety");
            helper.assertTrue(elf.isVillageResident() && elf.getVillageHome().equals(inside), "Converted elf must bind to its home");
            helper.assertTrue(elf.canWanderTo(inside) && !elf.canWanderTo(outside), "Village residents must reject outside wander targets");
            helper.assertTrue(elf.getTarget() == null && elf.getAttributeValue(Attributes.MAX_HEALTH) == 20.0D,
                    "Elves must be passive, 20-health mobs");
            helper.assertTrue(!elf.removeWhenFarAway(Double.MAX_VALUE), "Elves must not despawn at distance");
            helper.assertTrue(!outsideVillager.isRemoved() && !overworldVillager.isRemoved(),
                    "Villagers outside the Polaris village must remain villagers");
            CompoundTag saved = new CompoundTag();
            elf.saveWithoutId(saved);
            ElfEntity restored = ElfContent.MALE_ELF.get().create(polaris);
            helper.assertTrue(restored != null, "Elf must recreate for save/load check");
            restored.load(saved);
            helper.assertTrue(restored.isVillageResident() && restored.getVillageHome().equals(inside),
                    "Village home must survive save/load");
            helper.assertTrue(!restored.removeWhenFarAway(10000), "Restored elf must remain persistent");
            ElfEntity female = ElfContent.FEMALE_ELF.get().create(polaris);
            helper.assertTrue(female != null && female.isFemale(), "Female variety must have its own entity type");
            helper.assertTrue(!female.isVillageResident() && female.canWanderTo(outside),
                    "Unbound elves must behave as ordinary passive mobs");
            female.bindToVillage(inside);
            CompoundTag transferredData = new CompoundTag();
            female.saveWithoutId(transferredData);
            ElfEntity transferred = ElfContent.FEMALE_ELF.get().create(overworld);
            helper.assertTrue(transferred != null, "Transferred elf must recreate in the destination level");
            transferred.load(transferredData);
            transferred.moveTo(overworldVillager.position());
            overworld.addFreshEntity(transferred);
            helper.assertTrue(!transferred.isVillageResident() && transferred.getVillageHome() == null && !transferred.hasRestriction(),
                    "An elf moved to another dimension must release its old home restriction");
            transferred.discard();
            elf.discard();
            outsideVillager.discard();
            overworldVillager.discard();
            PolarDimension.LOGGER.info("Elf behavior validated: scoped migration, passive attributes, male/female types, bounded wandering, saved home, persistence");
            helper.succeed();
        });
    }
}




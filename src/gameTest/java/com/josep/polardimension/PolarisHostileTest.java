package com.josep.polardimension;

import com.josep.polardimension.entity.EvilSnowmanContent;
import com.josep.polardimension.entity.EvilSnowmanEntity;
import com.josep.polardimension.portal.PortalContent;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerSpawnPhantomsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(PolarDimension.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PolarisHostileTest {
    @GameTest(template = "empty", batch = "evil_snowman", timeoutTicks = 300)
    public static void restrictedHostilesAndSnowmanMelee(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerLevel polaris = level.getServer().getLevel(PortalContent.POLARIS);
        helper.assertTrue(polaris != null, "Polaris must load");
        level.getServer().setDifficulty(Difficulty.NORMAL, true);
        polaris.getChunk(30, -63);
        polaris.updateSkyBrightness();
        BlockPos top = polaris.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(488, 0, -1000));
        BlockPos spawn = SpawnPlacements.getPlacementType(EvilSnowmanContent.EVIL_SNOWMAN.get()).adjustSpawnPosition(polaris, top);
        var list = polaris.getBiome(spawn).value().getMobSettings().getMobs(MobCategory.MONSTER).unwrap();
        var types = new java.util.HashSet<EntityType<?>>();
        list.forEach(entry -> types.add(entry.type));
        helper.assertTrue(types.equals(Set.of(EntityType.STRAY, EvilSnowmanContent.EVIL_SNOWMAN.get())),
                "Only strays and evil snowmen may be in Polaris's hostile spawn list");
        for (var type : types) {
            boolean accepted = false;
            RandomSource random = RandomSource.create(48115L);
            for (int i = 0; i < 256; i++) accepted |= SpawnPlacements.checkSpawnRules(type, polaris, MobSpawnType.NATURAL, spawn, random);
            helper.assertTrue(accepted && SpawnPlacements.isSpawnPositionOk(type, polaris, spawn), "Allowed hostiles must spawn on actual midnight snow: " + type);
        }
        for (var type : new EntityType<?>[] {EntityType.ZOMBIE, EntityType.CREEPER, EntityType.SKELETON, EntityType.SPIDER, EntityType.WITCH}) {
            var denied = new MobSpawnEvent.SpawnPlacementCheck(type, polaris, MobSpawnType.NATURAL, spawn, polaris.random, true);
            NeoForge.EVENT_BUS.post(denied);
            helper.assertTrue(denied.getResult() == MobSpawnEvent.SpawnPlacementCheck.Result.FAIL, "Other natural hostiles must be blocked");
        }
        var manual = new MobSpawnEvent.SpawnPlacementCheck(EntityType.ZOMBIE, polaris, MobSpawnType.SPAWN_EGG, spawn, polaris.random, true);
        NeoForge.EVENT_BUS.post(manual);
        helper.assertTrue(manual.getResult() == MobSpawnEvent.SpawnPlacementCheck.Result.DEFAULT, "Manual spawn eggs must remain available");
        var otherDimension = new MobSpawnEvent.SpawnPlacementCheck(EntityType.ZOMBIE, level, MobSpawnType.NATURAL, spawn, level.random, true);
        NeoForge.EVENT_BUS.post(otherDimension);
        helper.assertTrue(otherDimension.getResult() == MobSpawnEvent.SpawnPlacementCheck.Result.DEFAULT, "Other dimensions must retain hostile spawning");
        var phantoms = new PlayerSpawnPhantomsEvent(FakePlayerFactory.getMinecraft(polaris), 2);
        NeoForge.EVENT_BUS.post(phantoms);
        helper.assertTrue(phantoms.getResult() == PlayerSpawnPhantomsEvent.Result.DENY, "Insomnia phantoms must also be excluded in Polaris");

        EvilSnowmanEntity snowman = EvilSnowmanContent.EVIL_SNOWMAN.get().create(level);
        var zombie = EntityType.ZOMBIE.create(level);
        helper.assertTrue(Math.abs(snowman.getAttributeValue(Attributes.MOVEMENT_SPEED)
                        / zombie.getAttributeValue(Attributes.MOVEMENT_SPEED) - 2.0D) < 0.00001D,
                "Snowman must move at twice the base zombie speed");
        helper.assertTrue(snowman.getAttributeValue(Attributes.ATTACK_DAMAGE) == 6.0D && snowman.getMaxHealth() == 20.0F,
                "Snowman must have iron-sword damage and normal zombie health");
        var victim = EntityType.VILLAGER.create(level);
        helper.assertTrue(snowman.doHurtTarget(victim) && Math.abs(victim.getHealth() - 14.0F) < 0.001F,
                "Real melee hit must deal six damage to an unarmored target");
        CompoundTag saved = new CompoundTag();
        snowman.saveWithoutId(saved);
        var restored = EvilSnowmanContent.EVIL_SNOWMAN.get().create(level);
        restored.load(saved);
        helper.assertTrue(restored.getAttributeValue(Attributes.ATTACK_DAMAGE) == 6.0D
                        && restored.getAttributeValue(Attributes.MOVEMENT_SPEED) == 0.46D, "Combat stats must survive saving");

        BlockPos anchor = helper.absolutePos(new BlockPos(0, 1, 0));
        // Keep all test actors in the same already-visible chunk. Neighboring fresh chunks
        // can still hide entities from AI target searches in GameTestServer.
        BlockPos floor = new BlockPos((anchor.getX() >> 4) * 16 + 2, anchor.getY(), (anchor.getZ() >> 4) * 16 + 2);
        for (int x = -2; x <= 12; x++) for (int z = -2; z <= 8; z++) {
            level.setBlockAndUpdate(floor.offset(x, -1, z), Blocks.SNOW_BLOCK.defaultBlockState());
            for (int y = 0; y < 4; y++) level.setBlockAndUpdate(floor.offset(x, y, z), Blocks.AIR.defaultBlockState());
        }
        var target = EntityType.VILLAGER.create(level);
        target.setNoAi(true);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0D);
        target.setHealth(200.0F);
        target.moveTo(floor.getX() + 4.5D, floor.getY(), floor.getZ() + 3.5D);
        snowman.moveTo(floor.getX() + 1.5D, floor.getY(), floor.getZ() + 3.5D);
        var male = com.josep.polardimension.entity.ElfContent.MALE_ELF.get().create(level);
        var female = com.josep.polardimension.entity.ElfContent.FEMALE_ELF.get().create(level);
        male.setNoAi(true); female.setNoAi(true);
        male.moveTo(floor.getX() + 2.5D, floor.getY(), floor.getZ() + 1.5D);
        female.moveTo(floor.getX() + 3.5D, floor.getY(), floor.getZ() + 5.5D);
        helper.assertTrue(!snowman.canAttack(male) && !snowman.canAttack(female)
                        && !snowman.doHurtTarget(male) && !snowman.doHurtTarget(female),
                "Neither elf variety may be targeted or damaged by snowman melee");
        level.addFreshEntity(male); level.addFreshEntity(female);
        level.addFreshEntity(snowman);
        helper.runAfterDelay(10, () -> snowman.hurt(level.damageSources().mobAttack(male), 1.0F));
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(snowman.getTarget() == null && male.getHealth() == male.getMaxHealth()
                            && female.getHealth() == female.getMaxHealth(),
                    "With only elves nearby, the snowman must not hunt or retaliate against them");
            // Reset idle wandering before the separate, unobstructed chase check.
            snowman.getNavigation().stop();
            snowman.moveTo(floor.getX() + 1.5D, floor.getY(), floor.getZ() + 3.5D);
            level.addFreshEntity(target);
        });
        helper.runAfterDelay(160, () -> {
            helper.assertTrue(snowman.distanceTo(target) < 4.0F && target.getHealth() < 200.0F,
                    "Snowman AI must acquire, chase, and melee a target: distance=" + snowman.distanceTo(target)
                            + ", health=" + target.getHealth() + ", selected=" + snowman.getTarget());
            helper.assertTrue(male.getHealth() == male.getMaxHealth() && female.getHealth() == female.getMaxHealth()
                            && !(snowman.getTarget() instanceof com.josep.polardimension.entity.ElfEntity),
                    "Snowman must ignore closer elves, including retaliation against an elf attacker");
            male.discard(); female.discard();
            snowman.discard(); target.discard();
            PolarDimension.LOGGER.info("Evil snowman validated: midnight spawn rules, two hostile types only, phantom exclusion, 2x zombie speed, six-damage hit, saved stats, actual AI chase/melee");
            helper.succeed();
        });
    }
}

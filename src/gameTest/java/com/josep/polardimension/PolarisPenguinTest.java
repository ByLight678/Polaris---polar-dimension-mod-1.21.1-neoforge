package com.josep.polardimension;

import com.josep.polardimension.entity.PenguinContent;
import com.josep.polardimension.entity.PenguinEntity;
import com.josep.polardimension.portal.PortalContent;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(PolarDimension.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PolarisPenguinTest {
    @GameTest(template = "empty", batch = "penguin", timeoutTicks = 600)
    public static void passivePenguinDropsAndNaturalWildlife(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().getLevel(PortalContent.POLARIS);
        helper.assertTrue(level != null, "Polaris must load");
        level.setDayTime(6000);
        level.setWeatherParameters(6000, 0, false, false);
        level.updateSkyBrightness();
        helper.assertTrue(level.getSkyDarken() >= 10, "Wildlife must spawn under Polaris's permanent midnight lighting");
        var chunk = level.getChunk(25, -63);
        level.setChunkForced(25, -63, true);
        int x = 408, z = -1000;
        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
        BlockPos spawn = SpawnPlacements.getPlacementType(PenguinContent.PENGUIN.get()).adjustSpawnPosition(level, top);
        helper.assertTrue(level.getBlockState(spawn.below()).is(Blocks.SNOW_BLOCK), "Test real generated snow terrain");
        helper.assertTrue(SpawnPlacements.isSpawnPositionOk(PenguinContent.PENGUIN.get(), level, spawn)
                        && SpawnPlacements.checkSpawnRules(PenguinContent.PENGUIN.get(), level, MobSpawnType.NATURAL, spawn, level.random),
                "Penguins must naturally spawn on snowy Polaris terrain");
        helper.assertTrue(SpawnPlacements.isSpawnPositionOk(EntityType.POLAR_BEAR, level, spawn)
                        && SpawnPlacements.checkSpawnRules(EntityType.POLAR_BEAR, level, MobSpawnType.NATURAL, spawn, level.random),
                "Vanilla polar bears must naturally spawn on Polaris snow");
        var settings = level.getBiome(spawn).value().getMobSettings();
        Set<EntityType<?>> passive = new java.util.HashSet<>();
        for (MobCategory category : MobCategory.values()) {
            if (category != MobCategory.MONSTER && category != MobCategory.MISC) {
                settings.getMobs(category).unwrap().forEach(entry -> passive.add(entry.type));
            }
        }
        helper.assertTrue(passive.equals(Set.of(PenguinContent.PENGUIN.get(), EntityType.POLAR_BEAR)),
                "Wild passive spawn lists must contain only penguins and polar bears");
        checkScope(helper, level, EntityType.POLAR_BEAR, MobSpawnType.NATURAL, new BlockPos(0, 90, -1000), false);
        checkScope(helper, level, PenguinContent.PENGUIN.get(), MobSpawnType.CHUNK_GENERATION, new BlockPos(0, 90, -1000), false);
        checkScope(helper, level, EntityType.RABBIT, MobSpawnType.NATURAL, spawn, false);
        checkScope(helper, level, EntityType.BAT, MobSpawnType.NATURAL, spawn, false);
        checkScope(helper, level, EntityType.COW, MobSpawnType.SPAWN_EGG, spawn, true);
        checkScope(helper, level, EntityType.STRAY, MobSpawnType.NATURAL, spawn, true);
        checkScope(helper, helper.getLevel(), EntityType.COW, MobSpawnType.NATURAL, spawn, true);

        PenguinEntity penguin = PenguinContent.PENGUIN.get().create(level);
        helper.assertTrue(penguin != null, "Penguin must create");
        penguin.moveTo(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D);
        helper.assertTrue(net.neoforged.neoforge.event.EventHooks.checkSpawnPosition(penguin, level, MobSpawnType.NATURAL),
                "Penguin position checks must allow snow at midnight");
        var bear = EntityType.POLAR_BEAR.create(level);
        bear.moveTo(penguin.position());
        helper.assertTrue(net.neoforged.neoforge.event.EventHooks.checkSpawnPosition(bear, level, MobSpawnType.NATURAL),
                "Vanilla polar bear position checks must allow snow at midnight");
        helper.assertTrue(penguin.getTarget() == null && penguin.getAttribute(Attributes.ATTACK_DAMAGE) == null,
                "Penguins must be passive without an attack attribute");
        helper.assertTrue(penguin.isFood(new ItemStack(Items.COD)) && penguin.isFood(new ItemStack(Items.SALMON))
                        && !penguin.isFood(new ItemStack(Items.COOKED_COD)), "Raw cod/salmon must be breeding food");
        PenguinEntity child = penguin.getBreedOffspring(level, penguin);
        helper.assertTrue(child != null && child.getType() == PenguinContent.PENGUIN.get(), "Breeding must create penguins");
        child.setAge(-24000);
        CompoundTag saved = new CompoundTag();
        child.saveWithoutId(saved);
        PenguinEntity restored = PenguinContent.PENGUIN.get().create(level);
        restored.load(saved);
        helper.assertTrue(restored.isBaby() && restored.getBbHeight() < penguin.getBbHeight(), "Baby age and dimensions must survive saving");
        checkLoot(helper, level, penguin);

        // Exercise Minecraft's real chunk-generation spawner, including snow-layer position adjustment.
        // Observe real additions: newly forced chunks can still have hidden entity sections,
        // so both spatial and global tracking queries omit them until visibility updates.
        var wildlife = new java.util.concurrent.ConcurrentLinkedQueue<net.minecraft.world.entity.Mob>();
        AABB bounds = new AABB(400, level.getMinBuildHeight(), -1008, 416, level.getMaxBuildHeight(), -992);
        java.util.function.Consumer<net.neoforged.neoforge.event.entity.EntityJoinLevelEvent> joined = event -> {
            if (event.getLevel() == level && event.getEntity() instanceof net.minecraft.world.entity.Mob mob
                    && bounds.intersects(mob.getBoundingBox())) wildlife.add(mob);
        };
        NeoForge.EVENT_BUS.addListener(joined);
        try {
            RandomSource random = RandomSource.create(7439182L);
            for (int attempt = 0; attempt < 256; attempt++) {
                NaturalSpawner.spawnMobsForChunkGeneration(level, level.getBiome(spawn), chunk.getPos(), random);
            }
        } finally {
            NeoForge.EVENT_BUS.unregister(joined);
        }
        helper.assertTrue(wildlife.stream().anyMatch(mob -> mob instanceof PenguinEntity && !mob.isRemoved()),
                "Native chunk-generation spawning must actually produce penguins");
        helper.assertTrue(wildlife.stream().allMatch(mob -> passive.contains(mob.getType())), "Native spawner must produce only allowed wildlife");
        PolarDimension.LOGGER.info("Penguins validated: {} native wildlife additions on snow, polar bear snow rules, scoped spawn lists, passive breeding/save behavior, 128 chicken-loot comparisons", wildlife.size());
        helper.succeed();
    }

    private static void checkScope(GameTestHelper helper, ServerLevel level, EntityType<?> type, MobSpawnType reason, BlockPos pos, boolean allowed) {
        var event = new MobSpawnEvent.SpawnPlacementCheck(type, level, reason, pos, level.random, true);
        NeoForge.EVENT_BUS.post(event);
        helper.assertTrue((event.getResult() != MobSpawnEvent.SpawnPlacementCheck.Result.FAIL) == allowed,
                "Wildlife restriction scope: " + type + "/" + reason + "/" + level.dimension());
    }

    private static void checkLoot(GameTestHelper helper, ServerLevel level, PenguinEntity penguin) {
        var loot = level.getServer().reloadableRegistries();
        LootTable actual = loot.getLootTable(ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.parse("polar_dimension:entities/penguin")));
        LootTable vanilla = loot.getLootTable(ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.parse("minecraft:entities/chicken")));
        var killer = helper.makeMockPlayer(GameType.SURVIVAL);
        for (int looting : new int[] {0, 3}) {
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            if (looting > 0) sword.enchant(level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.LOOTING), looting);
            killer.setItemSlot(EquipmentSlot.MAINHAND, sword);
            for (boolean fire : new boolean[] {false, true}) {
                penguin.setRemainingFireTicks(fire ? 100 : 0);
                LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.THIS_ENTITY, penguin)
                        .withParameter(LootContextParams.ORIGIN, penguin.position())
                        .withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().playerAttack(killer))
                        .withParameter(LootContextParams.ATTACKING_ENTITY, killer)
                        .withParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, killer)
                        .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, killer).create(LootContextParamSets.ENTITY);
                for (long seed = 1; seed <= 32; seed++) {
                    var drops = actual.getRandomItems(params, seed);
                    helper.assertTrue(counts(drops).equals(counts(vanilla.getRandomItems(params, seed))),
                            "Penguin loot must match chicken exactly for fire=" + fire + ", Looting=" + looting);
                    helper.assertTrue(drops.stream().anyMatch(stack -> stack.is(fire ? Items.COOKED_CHICKEN : Items.CHICKEN)),
                            "Meat must cook only when burning");
                }
            }
        }
    }

    private static Map<Item, Integer> counts(java.util.List<ItemStack> stacks) {
        Map<Item, Integer> counts = new HashMap<>();
        stacks.forEach(stack -> counts.merge(stack.getItem(), stack.getCount(), Integer::sum));
        return counts;
    }
}

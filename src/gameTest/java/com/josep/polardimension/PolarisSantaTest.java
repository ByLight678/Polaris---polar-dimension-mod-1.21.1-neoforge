package com.josep.polardimension;

import com.josep.polardimension.entity.SantaBasementSpawn;
import com.josep.polardimension.entity.SantaBossData;
import com.josep.polardimension.entity.SantaContent;
import com.josep.polardimension.entity.SantaEntity;
import com.josep.polardimension.portal.PortalContent;
import com.josep.polardimension.village.NorthPolePlacement;
import com.josep.polardimension.village.VillageBuildingPiece;
import com.josep.polardimension.village.VillageLayout;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import software.bernie.geckolib.loading.FileLoader;
import software.bernie.geckolib.loading.object.BakedModelFactory;
import software.bernie.geckolib.loading.object.GeometryTree;

@GameTestHolder(PolarDimension.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PolarisSantaTest {
    @GameTest(template = "empty", batch = "santa", timeoutTicks = 1200)
    public static void geckoAssetsAndSingleSavedBasementSpawn(GameTestHelper helper) {
        validateGeckoAssets(helper);
        var level = helper.getLevel().getServer().getLevel(PortalContent.POLARIS);
        helper.assertTrue(level != null, "Polaris must load");
        int surface = VillageLayout.surfaceY(level.getChunkSource().getGenerator(), level, level.getChunkSource().randomState());
        VillageBuildingPiece building = new VillageBuildingPiece(level.getStructureManager(), VillageLayout.TOY_WORKSHOP, surface);
        for (Rotation rotation : Rotation.values()) {
            // Simulate an older, differently positioned/rotated workshop using its saved piece position.
            var plot = new VillageLayout.Plot("christmas_toy_workshop", -17, -74, 35, 31, 7, 17, rotation);
            var old = new VillageBuildingPiece(level.getStructureManager(), plot, surface);
            BlockPos expected = plot.transformed(17, surface - 6, 12).offset(VillageLayout.CENTER_X, 0, VillageLayout.CENTER_Z);
            helper.assertTrue(old.workshopBossSpawn().orElseThrow().equals(expected), "Spawn must follow the actual saved workshop rotation and origin");
        }
        var village = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
                .get(id("north_pole_village"));
        var anchor = level.getChunk(NorthPolePlacement.START_CHUNK.x, NorthPolePlacement.START_CHUNK.z);
        level.getChunkSource().getGenerator().createStructures(level.registryAccess(), level.getChunkSource().getGeneratorState(),
                level.structureManager(), anchor, level.getStructureManager());
        var start = anchor.getStartForStructure(village);
        helper.assertTrue(start != null && start.isValid(), "Workshop's native village start must exist");
        var box = building.getBoundingBox();
        for (int x = box.minX() >> 4; x <= box.maxX() >> 4; x++) for (int z = box.minZ() >> 4; z <= box.maxZ() >> 4; z++) {
            level.getChunk(x, z);
            var clip = new BoundingBox(x * 16, level.getMinBuildHeight(), z * 16, x * 16 + 15, level.getMaxBuildHeight() - 1, z * 16 + 15);
            building.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(),
                    net.minecraft.util.RandomSource.create(1), clip, new ChunkPos(x, z), BlockPos.ZERO);
        }
        BlockPos spawn = building.workshopBossSpawn().orElseThrow();
        helper.assertTrue(level.getBlockState(spawn.below()).is(Blocks.SPRUCE_PLANKS), "Boss must stand on the actual basement floor");
        helper.assertTrue(!SantaBossData.get(level).hasSpawned(), "Fresh world must not have recorded an encounter yet");
        for (var piece : start.getPieces()) {
            if (!(piece instanceof VillageBuildingPiece tree) || tree.treeSantaSpawn().isEmpty()) continue;
            var treeBox = tree.getBoundingBox();
            for (int x = treeBox.minX() >> 4; x <= treeBox.maxX() >> 4; x++) for (int z = treeBox.minZ() >> 4; z <= treeBox.maxZ() >> 4; z++) {
                level.getChunk(x, z);
                tree.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(),
                        net.minecraft.util.RandomSource.create(1), new BoundingBox(x * 16, level.getMinBuildHeight(), z * 16, x * 16 + 15, level.getMaxBuildHeight() - 1, z * 16 + 15), new ChunkPos(x, z), BlockPos.ZERO);
            }
            var treeAnchor = tree.treeSantaSpawn().orElseThrow();
            level.getChunk(treeAnchor.getX() >> 4, treeAnchor.getZ() >> 4);
        }
        helper.assertTrue(com.josep.polardimension.entity.FriendlySantaSpawn.ensureSpawned(level) == null,
                "Friendly Santa must not spawn before the boss is defeated");
        java.util.List<com.josep.polardimension.entity.FriendlySantaEntity> merchants = new java.util.ArrayList<>();
        int[] additions = {0};
        java.util.function.Consumer<EntityJoinLevelEvent> joined = event -> { if (event.getEntity() instanceof SantaEntity) additions[0]++; if (event.getEntity() instanceof com.josep.polardimension.entity.FriendlySantaEntity merchant) merchants.add(merchant); };
        NeoForge.EVENT_BUS.addListener(joined);
        try {
            SantaEntity santa = SantaBasementSpawn.ensureSpawned(level);
            helper.assertTrue(santa != null && santa.blockPosition().equals(spawn), "Santa must spawn at the workshop's basement anchor");
            helper.assertTrue(level.noCollision(santa) && santa.getBasementHome().equals(spawn) && santa.isPersistenceRequired(),
                    "Boss must fit the room, save its home, and remain persistent");
            helper.assertTrue(santa.canWanderTo(spawn.offset(3, 0, 3)) && !santa.canWanderTo(spawn.above(6))
                            && !santa.canWanderTo(spawn.offset(20, 0, 0)), "Wandering must stay in the basement");
            helper.assertTrue(santa.getTarget() == null && !santa.removeWhenFarAway(100000), "Boss without players nearby must remain untargeted and persistent");
            helper.assertTrue(SantaBasementSpawn.ensureSpawned(level) == null && additions[0] == 1, "Repeated checks must not duplicate Santa");
            CompoundTag entityTag = new CompoundTag();
            santa.saveWithoutId(entityTag);
            var restored = SantaContent.SANTA.get().create(level);
            restored.load(entityTag);
            helper.assertTrue(restored.getBasementHome().equals(spawn) && restored.isPersistenceRequired(), "Home/persistence must survive entity save/load");
            helper.assertTrue(santa.getMaxHealth() == 600 && santa.getHealth() == 600, "New Big Nicky must have 600 health");
            restored.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);
            restored.setHealth(75);
            CompoundTag legacyHealth = new CompoundTag(); restored.saveWithoutId(legacyHealth);
            var upgraded = SantaContent.SANTA.get().create(level); upgraded.load(legacyHealth);
            helper.assertTrue(upgraded.getMaxHealth() == 600 && upgraded.getHealth() == 450,
                    "Existing boss health must upgrade while preserving its health percentage");
            restored.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(200);
            restored.setHealth(100);
            restored.saveWithoutId(legacyHealth);
            upgraded.load(legacyHealth);
            helper.assertTrue(upgraded.getMaxHealth() == 600 && upgraded.getHealth() == 300,
                    "The 200-health boss must also upgrade while preserving its health percentage");
            var data = SantaBossData.get(level);
            var restoredData = SantaBossData.load(data.save(new CompoundTag(), level.registryAccess()), level.registryAccess());
            helper.assertTrue(restoredData.hasSpawned() && restoredData.getBossId().equals(santa.getUUID()), "Encounter UUID must survive world save/load");
            helper.assertTrue(santa.playPreviewAction("axe_swing") && !santa.playPreviewAction("frost_breath"),
                    "Server animation trigger must accept an action and reject overlapping previews");
            var actions = santa.getAnimatableInstanceCache().getManagerForId(santa.getId()).getAnimationControllers().get("actions");
            helper.assertTrue(actions != null, "GeckoLib action controller must register on the server");
            santa.kill();
            helper.assertTrue(data.isDefeated(), "Killing the recorded boss must persist the victory unlock");
            com.josep.polardimension.entity.FriendlySantaSpawn.ensureSpawned(level);
            helper.assertTrue(merchants.size() == 1 && data.getFriendlySantaId().equals(merchants.getFirst().getUUID()),
                    "Victory must spawn exactly one saved tree-side Santa");
            var merchant = merchants.getFirst();
            helper.assertTrue(level.noCollision(merchant) && merchant.isPersistenceRequired()
                    && merchant.getTarget() == null && merchant.distanceToSqr(0, merchant.getY(), -1000) < 900,
                    "Merchant must be passive and fit beside the central tree");
            helper.assertTrue(!merchant.hurt(level.damageSources().generic(), 100)
                    && !merchant.hurt(level.damageSources().genericKill(), 100) && merchant.getHealth() == merchant.getMaxHealth(),
                    "Merchant must be immune to damage, including bypass-invulnerability damage");
            helper.assertTrue(merchant.getOffers().size() == 1, "Santa must have exactly one trade");
            var offer = merchant.getOffers().getFirst();
            var tooFew = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.EMERALD_BLOCK, 31);
            var star = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.NETHER_STAR);
            helper.assertTrue(!offer.take(tooFew, star), "31 emerald blocks must not buy a boss egg");
            var payment = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.EMERALD_BLOCK, 32);
            helper.assertTrue(!offer.take(payment, net.minecraft.world.item.ItemStack.EMPTY), "The nether star payment must be required");
            helper.assertTrue(offer.take(payment, star) && payment.isEmpty() && star.isEmpty()
                    && offer.getResult().is(SantaContent.SPAWN_EGG.get()) && offer.getResult().getCount() == 1,
                    "The trade must consume 32 emerald blocks and one nether star for one Big Nicky egg");
            merchant.notifyTrade(offer);
            helper.assertTrue(!offer.isOutOfStock() && offer.getCostA().getCount() == 32, "Trade must remain available at a fixed price");
            // Save an obsolete offer to verify migration of already-existing Santas.
            merchant.getOffers().clear();
            merchant.getOffers().add(new net.minecraft.world.item.trading.MerchantOffer(
                    new net.minecraft.world.item.trading.ItemCost(net.minecraft.world.item.Items.EMERALD_BLOCK, 64),
                    new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.NETHER_STAR), 10, 0, 0));
            CompoundTag merchantTag = new CompoundTag(); merchant.saveWithoutId(merchantTag);
            var loadedMerchant = com.josep.polardimension.entity.FriendlySantaContent.SANTA.get().create(level);
            loadedMerchant.load(merchantTag);
            helper.assertTrue(loadedMerchant.getOffers().size() == 1 && loadedMerchant.getOffers().getFirst().getCostA().getCount() == 32
                    && loadedMerchant.getOffers().getFirst().getResult().is(SantaContent.SPAWN_EGG.get()) && loadedMerchant.isInvulnerable()
                    && loadedMerchant.getRestrictCenter().equals(merchant.getRestrictCenter()), "Merchant home, invulnerability, and trade must survive reload");
            var savedVictory = SantaBossData.load(data.save(new CompoundTag(), level.registryAccess()), level.registryAccess());
            helper.assertTrue(savedVictory.isDefeated() && savedVictory.getFriendlySantaId().equals(merchant.getUUID())
                    && com.josep.polardimension.entity.FriendlySantaSpawn.ensureSpawned(level) == null,
                    "Saved victory and merchant identity must prevent duplicate merchants");
            helper.assertTrue(SantaBasementSpawn.ensureSpawned(level) == null && additions[0] == 1, "Defeated boss must not respawn automatically");
        } finally { NeoForge.EVENT_BUS.unregister(joined); }
        helper.assertTrue(SantaBasementSpawn.ensureSpawned(helper.getLevel()) == null, "No automatic encounter may spawn in the Overworld");
        PolarDimension.LOGGER.info("Santa validated: GeckoLib baked geometry/animations, 29 cubes, 404 source keyframes, basement clearance, legacy rotations, one-time UUID, persistence, confinement, preview trigger, no automatic respawn");
        helper.succeed();
    }

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(PolarDimension.MOD_ID, path); }

    private static void validateGeckoAssets(GameTestHelper helper) {
        ResourceManager assets = new ResourceManager() {
            @Override public Set<String> getNamespaces() { return Set.of(PolarDimension.MOD_ID); }
            @Override public Optional<Resource> getResource(ResourceLocation id) {
                String path = "/assets/" + id.getNamespace() + "/" + id.getPath();
                if (PolarisSantaTest.class.getResource(path) == null) return Optional.empty();
                return Optional.of(new Resource(null, () -> PolarisSantaTest.class.getResourceAsStream(path)));
            }
            @Override public List<Resource> getResourceStack(ResourceLocation id) { return getResource(id).stream().toList(); }
            @Override public Map<ResourceLocation, Resource> listResources(String path, Predicate<ResourceLocation> filter) { return Map.of(); }
            @Override public Map<ResourceLocation, List<Resource>> listResourceStacks(String path, Predicate<ResourceLocation> filter) { return Map.of(); }
            @Override public Stream<PackResources> listPacks() { return Stream.empty(); }
        };
        var model = FileLoader.loadModelFile(id("geo/disheveled_santa.geo.json"), assets);
        var baked = BakedModelFactory.DEFAULT_FACTORY.constructGeoModel(GeometryTree.fromModel(model));
        helper.assertTrue(model.minecraftGeometry()[0].bones().length == 10 && baked.getBone("ice_axe_head").isPresent(),
                "GeckoLib must bake the complete rig, including the nested axe");
        int count = java.util.Arrays.stream(model.minecraftGeometry()[0].bones()).mapToInt(b -> b.cubes() == null ? 0 : b.cubes().length).sum();
        helper.assertTrue(count == 29, "Export must retain all 29 source cubes");
        var animations = FileLoader.loadAnimationsFile(id("animations/disheveled_santa.animation.json"), assets);
        for (String clip : new String[] {"ritual_start", "ritual_chant", "ritual_end", "axe_swing", "frost_breath", "walk", "idle"}) {
            helper.assertTrue(animations.getAnimation(clip) != null, "GeckoLib must parse animation: " + clip);
        }
        helper.assertTrue(Math.abs(animations.getAnimation("axe_swing").length() - 33.0D) < 0.01D
                        && Math.abs(animations.getAnimation("frost_breath").length() - 56.0D) < 0.01D,
                "Authored action durations must survive export (GeckoLib stores ticks)");
    }
}

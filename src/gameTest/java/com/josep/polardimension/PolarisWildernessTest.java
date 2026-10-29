package com.josep.polardimension;

import com.josep.polardimension.portal.PortalContent;
import com.josep.polardimension.worldgen.WildernessStructure;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(PolarDimension.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PolarisWildernessTest {
    @GameTest(template = "empty", batch = "wilderness", timeoutTicks = 2400)
    public static void uncommonNativeStructuresOnSnow(GameTestHelper helper) throws Exception {
        var level = helper.getLevel().getServer().getLevel(PortalContent.POLARIS);
        helper.assertTrue(level != null, "Polaris must load");
        var lootKey = ResourceKey.create(Registries.LOOT_TABLE, id("chests/polaris_wilderness"));
        var supplies = level.getServer().reloadableRegistries().getLootTable(lootKey);
        var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, net.minecraft.world.phys.Vec3.ZERO)
                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
        Set<net.minecraft.world.item.Item> seen = new HashSet<>();
        for (long seed = 1; seed <= 100; seed++) {
            var drops = supplies.getRandomItems(params, seed);
            helper.assertTrue(drops.size() >= 2 && drops.size() <= 4, "Each supply container must roll 2-4 rewards");
            for (var stack : drops) {
                seen.add(stack.getItem());
                helper.assertTrue(stack.is(net.minecraft.world.item.Items.COMPASS) || stack.is(net.minecraft.world.item.Items.COBWEB)
                        || stack.is(net.minecraft.world.item.Items.SNOWBALL), "Supplies must contain only the requested items");
            }
        }
        helper.assertTrue(seen.size() == 3, "All three requested supply items must be obtainable");
        var generator = level.getChunkSource().getGenerator();
        var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        var pool = level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET).get(id("polaris_wilderness"));
        helper.assertTrue(pool != null && pool.structures().size() == 2, "Both wilderness structures must share a placement pool");
        var placement = (RandomSpreadStructurePlacement) pool.placement();
        helper.assertTrue(placement.spacing() == 24 && placement.separation() == 10, "Uncommon structures must have wide random spacing");
        helper.assertTrue(!placement.getPotentialStructureChunk(0, 4, 4).equals(placement.getPotentialStructureChunk(918273L, 4, 4)),
                "Wilderness placement must vary by world seed");
        for (String name : new String[] {"abandoned_snowy_campsite", "snowy_igloo"}) {
            validateTemplate(helper, name);
            var structure = (WildernessStructure) registry.get(id(name));
            helper.assertTrue(structure != null, "Structure must register: " + name);
            var context = new Structure.GenerationContext(level.registryAccess(), generator, generator.getBiomeSource(),
                    level.getChunkSource().randomState(), level.getStructureManager(), level.getSeed(), new ChunkPos(0, -63), level, biome -> true);
            helper.assertTrue(structure.findGenerationPoint(context).isEmpty(), "Wilderness pieces must never enter the village buffer");
            var other = helper.getLevel();
            var otherGenerator = other.getChunkSource().getGenerator();
            var otherContext = new Structure.GenerationContext(other.registryAccess(), otherGenerator, otherGenerator.getBiomeSource(),
                    other.getChunkSource().randomState(), other.getStructureManager(), other.getSeed(), new ChunkPos(80, 80), other, biome -> true);
            helper.assertTrue(structure.findGenerationPoint(otherContext).isEmpty(), "Wilderness structures must not generate in the Overworld");
        }
        Set<String> found = new HashSet<>();
        for (int region = 2; region < 34 && found.size() < 2; region++) {
            // Vanilla accepts chunk coordinates here and divides them by spacing internally.
            ChunkPos candidate = placement.getPotentialStructureChunk(level.getSeed(), region * placement.spacing(), placement.spacing());
            if (!placement.isStructureChunk(level.getChunkSource().getGeneratorState(), candidate.x, candidate.z)) continue;
            var chunk = level.getChunk(candidate.x, candidate.z);
            // GameTestServer disables automatic structures; invoke the actual native generation path.
            generator.createStructures(level.registryAccess(), level.getChunkSource().getGeneratorState(), level.structureManager(), chunk,
                    level.getStructureManager());
            // Mirror the chunk pipeline's cache notification after the test manually adds starts.
            level.onStructureStartsAvailable(chunk);
            for (String name : new String[] {"abandoned_snowy_campsite", "snowy_igloo"}) {
                Structure structure = registry.get(id(name));
                StructureStart start = chunk.getStartForStructure(structure);
                if (start != null && start.isValid()) PolarDimension.LOGGER.info("Wilderness start: {} at {}", name, candidate);
                if (start == null || !start.isValid() || !found.add(name)) continue;
                helper.assertTrue(start.getPieces().size() == 1, "Each wilderness structure must contain one saved template piece");
                BoundingBox box = start.getBoundingBox();
                helper.assertTrue(box.minX() > 256 || box.maxX() < -256 || box.minZ() > -744 || box.maxZ() < -1256,
                        "Entire wilderness footprint must stay outside village buffer");
                for (int x = box.minX() >> 4; x <= box.maxX() >> 4; x++) for (int z = box.minZ() >> 4; z <= box.maxZ() >> 4; z++) {
                    level.getChunk(x, z);
                    var clip = new BoundingBox(x * 16, level.getMinBuildHeight(), z * 16, x * 16 + 15, level.getMaxBuildHeight() - 1, z * 16 + 15);
                    start.placeInChunk(level, level.structureManager(), generator, net.minecraft.util.RandomSource.create(1), clip, new ChunkPos(x, z));
                }
                for (int x = box.minX(); x <= box.maxX(); x++) for (int z = box.minZ(); z <= box.maxZ(); z++) {
                    var base = new BlockPos(x, box.minY(), z);
                    helper.assertTrue(level.getBlockState(base).is(net.minecraft.world.level.block.Blocks.SNOW_BLOCK)
                                    && level.getBlockState(base.below()).isSolid(), "Whole template foundation must meet solid ground");
                }
                int supplyContainers = 0;
                for (int x = box.minX(); x <= box.maxX(); x++) for (int y = box.minY(); y <= box.maxY(); y++) for (int z = box.minZ(); z <= box.maxZ(); z++) {
                    var pos = new BlockPos(x, y, z);
                    var state = level.getBlockState(pos);
                    if (!state.is(net.minecraft.world.level.block.Blocks.CHEST) && !state.is(net.minecraft.world.level.block.Blocks.BARREL)) continue;
                    if (level.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity container) {
                        helper.assertTrue(lootKey.equals(container.getLootTable()), "Each wilderness chest/barrel must receive the supply table");
                        supplyContainers++;
                    }
                }
                helper.assertTrue(supplyContainers == (name.equals("snowy_igloo") ? 1 : 4), "Every template supply container must be wired");
                var serialization = StructurePieceSerializationContext.fromLevel(level);
                CompoundTag saved = start.getPieces().getFirst().createTag(serialization);
                var restored = BuiltInRegistries.STRUCTURE_PIECE.get(ResourceLocation.parse(saved.getString("id"))).load(serialization, saved);
                helper.assertTrue(restored.getBoundingBox().equals(box), "Rotated wilderness piece must survive save/load");
                var holder = registry.getHolderOrThrow(ResourceKey.create(Registries.STRUCTURE, id(name)));
                var located = generator.findNearestMapStructure(level, HolderSet.direct(holder), candidate.getMiddleBlockPosition(80), 1, false);
                helper.assertTrue(located != null, "Native /locate must find generated wilderness structure: " + name);
            }
        }
        helper.assertTrue(found.size() == 2, "Native random placement must generate both wilderness templates; found " + found);
        PolarDimension.LOGGER.info("Polaris wilderness validated: both imported templates, native random starts and /locate, surface foundations, saved rotation, village buffer, Polaris-only scope");
        helper.succeed();
    }

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(PolarDimension.MOD_ID, path); }

    private static void validateTemplate(GameTestHelper helper, String name) throws Exception {
        try (var stream = PolarisWildernessTest.class.getResourceAsStream("/data/polar_dimension/structure/wilderness/" + name + ".nbt")) {
            helper.assertTrue(stream != null, "Missing wilderness template: " + name);
            var data = NbtIo.readCompressed(stream, NbtAccounter.unlimitedHeap());
            helper.assertTrue(data.getInt("DataVersion") == 3955, "Wilderness assets must target 1.21.1");
            for (Tag tag : data.getList("palette", Tag.TAG_COMPOUND)) {
                var state = (CompoundTag) tag;
                var id = ResourceLocation.parse(state.getString("Name"));
                helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(id), "Unknown block: " + id);
                var block = BuiltInRegistries.BLOCK.get(id);
                var properties = state.getCompound("Properties");
                for (String key : properties.getAllKeys()) {
                    var property = block.getStateDefinition().getProperty(key);
                    helper.assertTrue(property != null && property.getValue(properties.getString(key)).isPresent(), "Invalid 1.21.1 block property: " + id + "/" + key);
                }
            }
        }
    }
}

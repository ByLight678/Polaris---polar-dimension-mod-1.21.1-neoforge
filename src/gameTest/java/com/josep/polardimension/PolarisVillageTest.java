package com.josep.polardimension;

import com.josep.polardimension.portal.PortalContent;
import com.josep.polardimension.entity.ElfEntity;
import com.josep.polardimension.village.NorthPolePlacement;
import com.josep.polardimension.village.VillageContent;
import com.josep.polardimension.village.VillageLayout;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayDeque;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(PolarDimension.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PolarisVillageTest {
    @GameTest(template = "empty", timeoutTicks = 2400)
    public static void fixedVillageTemplatesAndGeneration(GameTestHelper helper) throws Exception {
        ServerLevel level = helper.getLevel().getServer().getLevel(PortalContent.POLARIS);
        helper.assertTrue(level != null, "Polaris must load");
        Set<String> checked = new HashSet<>();
        Map<String, int[]> populations = new HashMap<>();
        for (VillageLayout.Plot plot : VillageLayout.PLOTS) {
            if (checked.add(plot.template())) populations.put(plot.template(), validateTemplate(helper, plot.template()));
            for (VillageLayout.Plot other : VillageLayout.PLOTS) {
                if (plot == other) continue;
                helper.assertTrue(plot.x() + plot.sizeX() <= other.x() || other.x() + other.sizeX() <= plot.x()
                                || plot.z() + plot.sizeZ() <= other.z() || other.z() + other.sizeZ() <= plot.z(),
                        "Village plots overlap: " + plot + " and " + other);
            }
        }
        helper.assertTrue(checked.size() == 10 && VillageLayout.PLOTS.size() == 29, "Use all ten assets in 29 plots");
        validateLayout(helper);
        int expectedMale = VillageLayout.PLOTS.stream().mapToInt(plot -> populations.get(plot.template())[0]).sum();
        int expectedFemale = VillageLayout.PLOTS.stream().mapToInt(plot -> populations.get(plot.template())[1]).sum();
        NorthPolePlacement placement = new NorthPolePlacement();
        for (long seed : new long[] {0, 1, -1, 123456789}) {
            helper.assertTrue(placement.getPotentialStructureChunk(seed, 400, -200).equals(NorthPolePlacement.START_CHUNK),
                    "Village location must be seed independent");
        }
        Structure structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                .get(ResourceLocation.fromNamespaceAndPath(PolarDimension.MOD_ID, "north_pole_village"));
        helper.assertTrue(structure != null, "Village structure must register");
        level.getChunk(NorthPolePlacement.START_CHUNK.x, NorthPolePlacement.START_CHUNK.z);
        // GameTestServer hardcodes generateStructures=false. Exercise the native start-generation
        // and chunk-clipped placement methods explicitly while keeping actual Polaris terrain.
        level.getChunkSource().getGenerator().createStructures(level.registryAccess(), level.getChunkSource().getGeneratorState(),
                level.structureManager(), level.getChunk(NorthPolePlacement.START_CHUNK.x, NorthPolePlacement.START_CHUNK.z),
                level.getStructureManager());
        StructureStart start = level.getChunk(NorthPolePlacement.START_CHUNK.x, NorthPolePlacement.START_CHUNK.z).getStartForStructure(structure);
        helper.assertTrue(start != null && start.isValid() && start.getPieces().size() == 30,
                "Exactly one native village start with roads and 29 template pieces");
        for (int x = -8; x <= 8; x++) {
            for (int z = -71; z <= -55; z++) {
                boolean anchor = x == NorthPolePlacement.START_CHUNK.x && z == NorthPolePlacement.START_CHUNK.z;
                helper.assertTrue(placement.isStructureChunk(level.getChunkSource().getGeneratorState(), x, z) == anchor,
                        "Only the north-pole chunk may start a village");
            }
        }
        int surface = VillageLayout.surfaceY(level.getChunkSource().getGenerator(), level,
                level.getChunkSource().randomState());
        for (int x = -7; x <= 7; x++) {
            for (int z = -70; z <= -56; z++) {
                var chunk = level.getChunk(x, z);
                var clip = new net.minecraft.world.level.levelgen.structure.BoundingBox(x * 16, level.getMinBuildHeight(),
                        z * 16, x * 16 + 15, level.getMaxBuildHeight() - 1, z * 16 + 15);
                start.placeInChunk(level, level.structureManager(), level.getChunkSource().getGenerator(),
                        net.minecraft.util.RandomSource.create(chunk.getPos().toLong()), clip, chunk.getPos());
                level.setChunkForced(x, z, true);
            }
        }
        var villageLoot = net.minecraft.resources.ResourceKey.create(Registries.LOOT_TABLE,
                ResourceLocation.fromNamespaceAndPath(PolarDimension.MOD_ID, "chests/polaris_village"));
        int containers = 0;
        for (int x = -7; x <= 7; x++) for (int z = -70; z <= -56; z++) {
            for (var entity : level.getChunk(x, z).getBlockEntities().values()) {
                var state = entity.getBlockState();
                if ((state.is(Blocks.CHEST) || state.is(Blocks.BARREL))
                        && entity instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity container) {
                    helper.assertTrue(villageLoot.equals(container.getLootTable()), "Village supply containers must use their own loot table");
                    containers++;
                }
            }
        }
        helper.assertTrue(containers == 28, "All 28 imported village containers must receive loot");
        var loot = level.getServer().reloadableRegistries().getLootTable(villageLoot);
        var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, net.minecraft.world.phys.Vec3.ZERO)
                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
        var allowed = java.util.Set.of(net.minecraft.world.item.Items.IRON_INGOT, net.minecraft.world.item.Items.GOLD_NUGGET,
                net.minecraft.world.item.Items.COOKIE, net.minecraft.world.item.Items.PUMPKIN_PIE, net.minecraft.world.item.Items.CAKE);
        java.util.Set<net.minecraft.world.item.Item> seen = new java.util.HashSet<>();
        for (long seed = 1; seed <= 100; seed++) {
            var drops = loot.getRandomItems(params, seed);
            helper.assertTrue(drops.size() >= 3 && drops.size() <= 6, "Village containers must roll 3-6 rewards");
            for (var stack : drops) {
                helper.assertTrue(allowed.contains(stack.getItem()), "Only requested village supplies may drop");
                seen.add(stack.getItem());
            }
        }
        helper.assertTrue(seen.equals(allowed), "All five village supply items must be obtainable");
        helper.assertTrue(level.getBlockState(new BlockPos(0, surface + 2, -1000)).is(Blocks.SPRUCE_LOG),
                "Central tree must be exactly at X=0 Z=-1000");
        helper.assertTrue(com.josep.polardimension.village.VillageRoadBlocks.isWarm(
                        level.getBlockState(new BlockPos(20, surface, -1000))), "Plaza must join the snow-free road network");
        long workshopPieces = start.getPieces().stream()
                .map(piece -> piece.createTag(StructurePieceSerializationContext.fromLevel(level)))
                .filter(tag -> tag.getString("Template").equals(VillageContent.template(VillageLayout.TOY_WORKSHOP.template()).toString()))
                .count();
        helper.assertTrue(workshopPieces == 1, "Generated village must contain exactly one toy workshop");
        BlockPos basementFloor = VillageLayout.TOY_WORKSHOP.transformed(17, surface - 7, 12)
                .offset(VillageLayout.CENTER_X, 0, VillageLayout.CENTER_Z);
        helper.assertTrue(level.getBlockState(basementFloor).is(Blocks.SPRUCE_PLANKS), "Unique workshop must retain its basement floor");
        helper.assertTrue(level.getBlockState(basementFloor.above(2)).isAir()
                        && level.getBlockState(basementFloor.above(3)).isAir(),
                "Workshop basement must retain space for the future miniboss");
        for (VillageLayout.Plot plot : VillageLayout.PLOTS) {
            BlockPos foundation = plot.transformed(0, surface, 0).offset(0, 0, -1000);
            helper.assertTrue(!level.getBlockState(foundation).isAir(), "Building must have a supported plot: " + plot);
            var piece = start.getPieces().get(VillageLayout.PLOTS.indexOf(plot) + 1);
            var context = StructurePieceSerializationContext.fromLevel(level);
            CompoundTag saved = piece.createTag(context);
            StructurePieceType type = BuiltInRegistries.STRUCTURE_PIECE.get(ResourceLocation.parse(saved.getString("id")));
            var restored = type.load(context, saved);
            helper.assertTrue(restored.getBoundingBox().equals(piece.getBoundingBox()), "Rotated template bounds must survive save/load");
        }
        // A fixture crossing a chunk edge can only be attached after its neighboring wall chunk is placed.
        for (var piece : start.getPieces()) if (piece instanceof com.josep.polardimension.village.VillageBuildingPiece building) building.lightInterior(level, null);
        var lightingStream = PolarisVillageTest.class.getResourceAsStream("/data/polar_dimension/structure/village/interior_lighting.json");
        var lighting = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(lightingStream, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        java.util.List<BlockPos> roomSamples = new java.util.ArrayList<>();
        int fixtures = 0;
        for (var plot : VillageLayout.PLOTS) {
            if (!lighting.has(plot.template())) continue;
            var plan = lighting.getAsJsonObject(plot.template());
            for (var value : plan.getAsJsonArray("torches")) {
                var local = value.getAsJsonObject().getAsJsonArray("pos");
                var pos = plot.transformed(local.get(0).getAsInt(), surface - plot.surfaceLayer() + local.get(1).getAsInt(), local.get(2).getAsInt())
                        .offset(VillageLayout.CENTER_X, 0, VillageLayout.CENTER_Z);
                var state = level.getBlockState(pos);
                helper.assertTrue(state.is(Blocks.WALL_TORCH) && state.canSurvive(level, pos), "Interior torches must be supported after every building rotation: " + plot.template() + " at " + pos);
                fixtures++;
            }
            if (java.util.Set.of("christmas_cottage", "christmas_cranberry_cottage", "christmas_alpine_house", "christmas_townhouse", "christmas_lodge").contains(plot.template())) {
                helper.assertTrue(!plan.getAsJsonArray("torches").isEmpty(), "Every house style must receive interior fixtures");
                for (var value : plan.getAsJsonArray("samples")) {
                    var local = value.getAsJsonArray();
                    roomSamples.add(plot.transformed(local.get(0).getAsInt(), surface - plot.surfaceLayer() + local.get(1).getAsInt(), local.get(2).getAsInt())
                            .offset(VillageLayout.CENTER_X, 0, VillageLayout.CENTER_Z));
                }
            }
        }
        helper.assertTrue(fixtures > 100 && !roomSamples.isEmpty(), "Whole village must receive interior lighting coverage");
        var firstPlot = VillageLayout.PLOTS.stream().filter(plot -> plot.template().equals("christmas_cottage")).findFirst().orElseThrow();
        var firstLocal = lighting.getAsJsonObject(firstPlot.template()).getAsJsonArray("torches").get(0).getAsJsonObject().getAsJsonArray("pos");
        var retrofitPos = firstPlot.transformed(firstLocal.get(0).getAsInt(), surface - firstPlot.surfaceLayer() + firstLocal.get(1).getAsInt(), firstLocal.get(2).getAsInt())
                .offset(VillageLayout.CENTER_X, 0, VillageLayout.CENTER_Z);
        var firstPiece = (com.josep.polardimension.village.VillageBuildingPiece) start.getPieces().get(VillageLayout.PLOTS.indexOf(firstPlot) + 1);
        level.setBlockAndUpdate(retrofitPos, Blocks.STONE.defaultBlockState());
        firstPiece.lightInterior(level, null);
        helper.assertTrue(level.getBlockState(retrofitPos).is(Blocks.STONE), "Retrofitting must preserve player blocks");
        level.setBlockAndUpdate(retrofitPos, Blocks.AIR.defaultBlockState());
        firstPiece.lightInterior(level, null);
        helper.assertTrue(level.getBlockState(retrofitPos).is(Blocks.WALL_TORCH), "Existing village pieces must support additive lighting upgrades");
        // Entity sections may be loaded on the following tick; block generation is checked synchronously.
        helper.runAfterDelay(20, () -> {
            for (var sample : roomSamples) helper.assertTrue(level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, sample) >= 7,
                    "House interior must have block light at least 7: " + sample + " actual=" + level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, sample));
            int count = level.getEntitiesOfClass(ElfEntity.class, new AABB(-120, surface - 10, -1120, 120, surface + 45, -880)).size();
            helper.assertTrue(count == expectedMale + expectedFemale, "Village must retain every template resident, got " + count);
            AABB villageBounds = new AABB(-120, surface - 10, -1120, 120, surface + 45, -880);
            var elves = level.getEntitiesOfClass(ElfEntity.class, villageBounds);
            helper.assertTrue(elves.stream().filter(ElfEntity::isFemale).count() == expectedFemale,
                    "Fresh village must preserve both varieties from imported templates");
            helper.assertTrue(elves.stream().allMatch(elf -> elf.isVillageResident() && elf.getVillageHome() != null),
                    "Every elf must bind to its village home");
            helper.assertTrue(level.getEntitiesOfClass(Villager.class, villageBounds).isEmpty(),
                    "Polaris village must contain elves instead of villagers");
            helper.assertTrue(level.getEntitiesOfClass(com.josep.polardimension.entity.ReindeerEntity.class, villageBounds).size() == 4,
                    "Stable must contain four reindeer");
            helper.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.animal.IronGolem.class, villageBounds).size() == 2,
                    "Plaza must contain two iron golems");
            helper.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, villageBounds).stream()
                            .noneMatch(item -> item.getItem().is(net.minecraft.world.item.Items.LANTERN)
                                    || item.getItem().is(net.minecraft.world.item.Items.WHITE_CARPET)
                                    || item.getItem().is(net.minecraft.world.item.Items.RED_CARPET)),
                    "Lanterns and carpet decorations must remain supported");
            PolarDimension.LOGGER.info("Polaris village validated: 29 compact plots, 10 templates, connected branching paths, fixed X=0 Z=-1000, surface={}, elves={}", surface, count);
            try {
                com.google.gson.JsonObject report = new com.google.gson.JsonObject();
                report.addProperty("surface_y", surface);
                com.google.gson.JsonArray plots = new com.google.gson.JsonArray();
                for (VillageLayout.Plot plot : VillageLayout.PLOTS) {
                    com.google.gson.JsonObject item = new com.google.gson.JsonObject();
                    item.addProperty("template", plot.template()); item.addProperty("x", plot.x()); item.addProperty("z", plot.z());
                    item.addProperty("width", plot.sizeX()); item.addProperty("depth", plot.sizeZ());
                    BlockPos entrance = plot.transformed(plot.entranceX(), 0, plot.depth() - 1);
                    item.addProperty("entry_x", entrance.getX()); item.addProperty("entry_z", entrance.getZ());
                    plots.add(item);
                }
                report.add("plots", plots);
                com.google.gson.JsonArray roads = new com.google.gson.JsonArray();
                for (int x = -116; x <= 116; x++) for (int z = -116; z <= 116; z++) {
                    if (VillageLayout.isRoad(x, z)) { com.google.gson.JsonArray point = new com.google.gson.JsonArray(); point.add(x); point.add(z); roads.add(point); }
                }
                report.add("roads", roads);
                java.nio.file.Path path = java.nio.file.Path.of("village-report/layout.json");
                java.nio.file.Files.createDirectories(path.getParent());
                java.nio.file.Files.writeString(path, report.toString());
            } catch (java.io.IOException error) { throw new RuntimeException(error); }
            helper.succeed();
        });
    }

    private static void validateLayout(GameTestHelper helper) {
        Set<BlockPos> roads = new HashSet<>();
        for (int x = -VillageLayout.HALF_SIZE; x <= VillageLayout.HALF_SIZE; x++) {
            for (int z = -VillageLayout.HALF_SIZE; z <= VillageLayout.HALF_SIZE; z++) {
                if (VillageLayout.isRoad(x, z)) {
                    helper.assertTrue(!VillageLayout.insidePlot(x, z), "Paths must not cut through buildings");
                    roads.add(new BlockPos(x, 0, z));
                }
            }
        }
        Set<BlockPos> connected = new HashSet<>();
        ArrayDeque<BlockPos> pending = new ArrayDeque<>();
        pending.add(new BlockPos(19, 0, 0));
        while (!pending.isEmpty()) {
            BlockPos pos = pending.remove();
            if (!roads.contains(pos) || !connected.add(pos)) continue;
            for (var direction : net.minecraft.core.Direction.Plane.HORIZONTAL) pending.add(pos.relative(direction));
        }
        helper.assertTrue(connected.size() == roads.size(), "Every branch must connect to the plaza without isolated paths");
        for (var plot : VillageLayout.PLOTS) {
            BlockPos entrance = plot.transformed(plot.entranceX(), 0, plot.depth() - 1);
            BlockPos outside = entrance.relative(plot.rotation().rotate(net.minecraft.core.Direction.SOUTH));
            helper.assertTrue(connected.contains(outside), "Entrance must reach the connected paths: " + plot.template());
        }
        int minX = VillageLayout.PLOTS.stream().mapToInt(VillageLayout.Plot::x).min().orElseThrow();
        int minZ = VillageLayout.PLOTS.stream().mapToInt(VillageLayout.Plot::z).min().orElseThrow();
        int maxX = VillageLayout.PLOTS.stream().mapToInt(p -> p.x() + p.sizeX()).max().orElseThrow();
        int maxZ = VillageLayout.PLOTS.stream().mapToInt(p -> p.z() + p.sizeZ()).max().orElseThrow();
        helper.assertTrue((maxX - minX) * (maxZ - minZ) < 41000, "Same building count must use a substantially more compact footprint");
        for (String house : new String[] {"christmas_alpine_house", "christmas_cranberry_cottage"}) {
            helper.assertTrue(VillageLayout.PLOTS.stream().filter(p -> p.template().equals(house)).count() >= 4,
                    "Both new houses must be distributed throughout the village");
        }
        helper.assertTrue(VillageLayout.PLOTS.stream().map(VillageLayout.Plot::rotation).distinct().count() == 4,
                "Buildings must face varied directions");
    }

    private static int[] validateTemplate(GameTestHelper helper, String name) throws Exception {
        String resource = "/data/polar_dimension/structure/village/" + name + ".nbt";
        try (InputStream stream = PolarisVillageTest.class.getResourceAsStream(resource)) {
            helper.assertTrue(stream != null, "Missing template: " + name);
            CompoundTag data = NbtIo.readCompressed(stream, NbtAccounter.unlimitedHeap());
            helper.assertTrue(data.getInt("DataVersion") == 3955, "Template must target 1.21.1");
            for (Tag tag : data.getList("palette", Tag.TAG_COMPOUND)) {
                CompoundTag state = (CompoundTag) tag;
                ResourceLocation id = ResourceLocation.parse(state.getString("Name"));
                helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(id), "Unknown 1.21.1 block " + id);
                Block block = BuiltInRegistries.BLOCK.get(id);
                CompoundTag properties = state.getCompound("Properties");
                for (String key : properties.getAllKeys()) {
                    var property = block.getStateDefinition().getProperty(key);
                    helper.assertTrue(property != null && property.getValue(properties.getString(key)).isPresent(),
                            "Unknown 1.21.1 property " + id + "/" + key);
                }
            }
            int[] population = new int[2];
            for (Tag tag : data.getList("entities", Tag.TAG_COMPOUND)) {
                String id = ((CompoundTag) tag).getCompound("nbt").getString("id");
                if (id.equals("polar_dimension:christmas_elf")) population[0]++;
                if (id.equals("polar_dimension:female_christmas_elf")) population[1]++;
            }
            return population;
        }
    }
}









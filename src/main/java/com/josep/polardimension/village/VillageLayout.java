package com.josep.polardimension.village;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;

/** Reproducible compact plan; chunk exploration order cannot change plots or paths. */
public final class VillageLayout {
    public static final int CENTER_X = 0;
    public static final int CENTER_Z = -1000;
    // Keep the old terrain/residency envelope compatible with existing saves.
    public static final int HALF_SIZE = 116;
    public static final int BLEND_EDGE = 144;
    private static final JsonObject PLAN = loadPlan();
    public static final List<Plot> PLOTS = createPlots();
    public static final Plot TOY_WORKSHOP = PLOTS.stream()
            .filter(plot -> plot.template().equals("christmas_toy_workshop")).findFirst().orElseThrow();
    private static final Set<Long> ROADS = createRoads();
    private static final Set<Long> LANTERNS = readPoints("lanterns");

    private VillageLayout() {}

    private static JsonObject loadPlan() {
        try (var stream = VillageLayout.class.getResourceAsStream("/data/polar_dimension/structure/village/layout.json")) {
            if (stream == null) throw new IllegalStateException("Missing North Pole village layout");
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException error) {
            throw new IllegalStateException("Cannot read North Pole village layout", error);
        }
    }

    private static List<Plot> createPlots() {
        List<Plot> plots = new ArrayList<>();
        for (var element : PLAN.getAsJsonArray("plots")) {
            JsonObject p = element.getAsJsonObject();
            plots.add(new Plot(p.get("template").getAsString(), p.get("x").getAsInt(), p.get("z").getAsInt(),
                    p.get("width").getAsInt(), p.get("depth").getAsInt(), p.get("surface_layer").getAsInt(),
                    p.get("entrance_x").getAsInt(), Rotation.valueOf(p.get("rotation").getAsString())));
        }
        if (plots.stream().filter(plot -> plot.template().equals("christmas_toy_workshop")).count() != 1) {
            throw new IllegalStateException("The North Pole village must contain exactly one toy workshop");
        }
        return List.copyOf(plots);
    }

    private static long key(int x, int z) { return ((long) x << 32) ^ (z & 0xffffffffL); }

    private static Set<Long> readPoints(String name) {
        Set<Long> points = new HashSet<>();
        for (var element : PLAN.getAsJsonArray(name)) {
            JsonArray point = element.getAsJsonArray();
            points.add(key(point.get(0).getAsInt(), point.get(1).getAsInt()));
        }
        return Set.copyOf(points);
    }

    private static Set<Long> createRoads() {
        Set<Long> roads = new HashSet<>();
        for (var element : PLAN.getAsJsonArray("path_centers")) {
            JsonArray point = element.getAsJsonArray();
            int x = point.get(0).getAsInt(), z = point.get(1).getAsInt();
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                int px = x + dx, pz = z + dz;
                if (Math.abs(px) <= HALF_SIZE && Math.abs(pz) <= HALF_SIZE && !insidePlot(px, pz)) roads.add(key(px, pz));
            }
        }
        return Set.copyOf(roads);
    }

    public static int surfaceY(ChunkGenerator generator, LevelHeightAccessor heights, RandomState random) {
        return generator.getBaseHeight(CENTER_X, CENTER_Z, Heightmap.Types.OCEAN_FLOOR_WG, heights, random) - 1;
    }

    public static int distance(int x, int z) { return Math.max(Math.abs(x - CENTER_X), Math.abs(z - CENTER_Z)); }
    public static boolean insidePlot(int x, int z) { return PLOTS.stream().anyMatch(plot -> plot.contains(x, z)); }
    public static boolean isRoad(int x, int z) { return ROADS.contains(key(x, z)); }
    public static boolean isLanternSpot(int x, int z) { return LANTERNS.contains(key(x, z)); }

    public record Plot(String template, int x, int z, int width, int depth, int surfaceLayer,
                       int entranceX, Rotation rotation) {
        public int sizeX() { return rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90 ? depth : width; }
        public int sizeZ() { return rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90 ? width : depth; }
        public boolean contains(int px, int pz) { return px >= x && px < x + sizeX() && pz >= z && pz < z + sizeZ(); }
        public BlockPos transformed(int localX, int y, int localZ) {
            return switch (rotation) {
                case NONE -> new BlockPos(x + localX, y, z + localZ);
                case CLOCKWISE_90 -> new BlockPos(x + depth - 1 - localZ, y, z + localX);
                case CLOCKWISE_180 -> new BlockPos(x + width - 1 - localX, y, z + depth - 1 - localZ);
                case COUNTERCLOCKWISE_90 -> new BlockPos(x + localZ, y, z + width - 1 - localX);
            };
        }
    }
}

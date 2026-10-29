package com.josep.polardimension.village;

import com.google.gson.JsonParser;
import com.josep.polardimension.PolarDimension;
import com.josep.polardimension.portal.PortalContent;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Additive light fixtures for fresh pieces and already-generated village houses. */
@EventBusSubscriber(modid = PolarDimension.MOD_ID)
public final class VillageInteriorLighting {
    private record Fixture(BlockPos pos, Direction facing, ResourceLocation support) {}
    private static final Map<String, List<Fixture>> FIXTURES = load();
    private VillageInteriorLighting() {}
    private static Map<String, List<Fixture>> load() {
        Map<String, List<Fixture>> result = new HashMap<>();
        try (var stream = VillageInteriorLighting.class.getResourceAsStream("/data/polar_dimension/structure/village/interior_lighting.json")) {
            if (stream == null) throw new IllegalStateException("Missing village interior lighting plan");
            var data = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            for (var entry : data.entrySet()) {
                var fixtures = new java.util.ArrayList<Fixture>();
                for (var value : entry.getValue().getAsJsonObject().getAsJsonArray("torches")) {
                    var fixture = value.getAsJsonObject(); var pos = fixture.getAsJsonArray("pos");
                    fixtures.add(new Fixture(new BlockPos(pos.get(0).getAsInt(), pos.get(1).getAsInt(), pos.get(2).getAsInt()),
                            Direction.byName(fixture.get("facing").getAsString()), ResourceLocation.parse(fixture.get("support").getAsString())));
                }
                result.put(VillageContent.template(entry.getKey()).toString(), List.copyOf(fixtures));
            }
        } catch (java.io.IOException exception) { throw new IllegalStateException("Cannot load interior lighting", exception); }
        return Map.copyOf(result);
    }
    public static void apply(ServerLevelAccessor level, String template, BlockPos origin, StructurePlaceSettings settings, BoundingBox clip) {
        if (!level.getLevel().dimension().equals(PortalContent.POLARIS)) return;
        for (var fixture : FIXTURES.getOrDefault(template, List.of())) {
            var pos = StructureTemplate.calculateRelativePosition(settings, fixture.pos()).offset(origin);
            if (clip != null && !clip.isInside(pos)) continue;
            if (level.getLevel().getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) == null || !level.getBlockState(pos).isAir()) continue;
            var state = Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, fixture.facing()).rotate(settings.getRotation());
            var support = pos.relative(state.getValue(WallTorchBlock.FACING).getOpposite());
            if (level.getLevel().getChunkSource().getChunkNow(support.getX() >> 4, support.getZ() >> 4) == null) continue;
            if (!BuiltInRegistries.BLOCK.getKey(level.getBlockState(support).getBlock()).equals(fixture.support()) || !state.canSurvive(level, pos)) continue;
            level.setBlock(pos, state, 3);
        }
    }
    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 40 != 0) return;
        var level = event.getServer().getLevel(PortalContent.POLARIS);
        if (level == null || level.players().stream().noneMatch(player -> VillageLayout.distance(player.getBlockX(), player.getBlockZ()) <= VillageLayout.BLEND_EDGE + 32)) return;
        var anchor = level.getChunk(NorthPolePlacement.START_CHUNK.x, NorthPolePlacement.START_CHUNK.z);
        var structure = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
                .get(ResourceLocation.fromNamespaceAndPath(PolarDimension.MOD_ID, "north_pole_village"));
        if (structure == null) return;
        var start = anchor.getStartForStructure(structure);
        if (start == null || !start.isValid()) return;
        for (var piece : start.getPieces()) if (piece instanceof VillageBuildingPiece building) building.lightInterior(level, null);
    }
}

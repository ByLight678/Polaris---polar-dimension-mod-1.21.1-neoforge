package com.josep.polardimension.worldgen;

import com.josep.polardimension.village.VillageLayout;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

public final class WildernessStructure extends Structure {
    public static final MapCodec<WildernessStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            settingsCodec(instance), ResourceLocation.CODEC.fieldOf("template").forGetter(s -> s.template),
            Codec.intRange(0, 16).fieldOf("surface_layer").forGetter(s -> s.surfaceLayer)
    ).apply(instance, WildernessStructure::new));
    private final ResourceLocation template;
    private final int surfaceLayer;

    public WildernessStructure(StructureSettings settings, ResourceLocation template, int surfaceLayer) {
        super(settings); this.template = template; this.surfaceLayer = surfaceLayer;
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        if (!(context.chunkGenerator() instanceof PolarisChunkGenerator)) return Optional.empty();
        var size = context.structureTemplateManager().getOrCreate(template).getSize();
        Rotation rotation = Rotation.getRandom(context.random());
        boolean turn = rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90;
        int sx = turn ? size.getZ() : size.getX(), sz = turn ? size.getX() : size.getZ();
        int x = context.chunkPos().getMiddleBlockX() - sx / 2;
        int z = context.chunkPos().getMiddleBlockZ() - sz / 2;
        // Reject the full footprint, including a generous buffer beyond the village terrain blend.
        if (x <= VillageLayout.CENTER_X + 256 && x + sx >= VillageLayout.CENTER_X - 256
                && z <= VillageLayout.CENTER_Z + 256 && z + sz >= VillageLayout.CENTER_Z - 256) return Optional.empty();
        int low = Integer.MAX_VALUE, high = Integer.MIN_VALUE;
        for (int px : new int[] {x, x + sx / 2, x + sx - 1}) for (int pz : new int[] {z, z + sz / 2, z + sz - 1}) {
            int y = context.chunkGenerator().getBaseHeight(px, pz, Heightmap.Types.OCEAN_FLOOR_WG,
                    context.heightAccessor(), context.randomState()) - 1;
            low = Math.min(low, y); high = Math.max(high, y);
        }
        if (high - low > 8 || size.getY() == 0) return Optional.empty();
        var plot = new VillageLayout.Plot(template.getPath(), x, z, size.getX(), size.getZ(), surfaceLayer, size.getX() / 2, rotation);
        int surface = high;
        return Optional.of(new GenerationStub(new BlockPos(x + sx / 2, surface, z + sz / 2), builder ->
                builder.addPiece(new WildernessPiece(context.structureTemplateManager(), template, plot, surface))));
    }

    @Override
    public StructureType<?> type() { return WildernessContent.STRUCTURE.get(); }
}

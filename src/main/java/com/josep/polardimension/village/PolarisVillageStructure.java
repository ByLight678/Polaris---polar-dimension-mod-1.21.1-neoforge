package com.josep.polardimension.village;

import com.josep.polardimension.worldgen.PolarisChunkGenerator;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

public final class PolarisVillageStructure extends Structure {
    public static final MapCodec<PolarisVillageStructure> CODEC = simpleCodec(PolarisVillageStructure::new);

    public PolarisVillageStructure(StructureSettings settings) { super(settings); }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        if (!(context.chunkGenerator() instanceof PolarisChunkGenerator)
                || !context.chunkPos().equals(NorthPolePlacement.START_CHUNK)) return Optional.empty();
        int surface = VillageLayout.surfaceY(context.chunkGenerator(), context.heightAccessor(), context.randomState());
        return Optional.of(new GenerationStub(new BlockPos(VillageLayout.CENTER_X, surface, VillageLayout.CENTER_Z), builder -> {
            builder.addPiece(new VillageRoadPiece(surface));
            for (VillageLayout.Plot plot : VillageLayout.PLOTS) {
                builder.addPiece(new VillageBuildingPiece(context.structureTemplateManager(), plot, surface));
            }
        }));
    }

    @Override
    public StructureType<?> type() { return VillageContent.VILLAGE.get(); }
}

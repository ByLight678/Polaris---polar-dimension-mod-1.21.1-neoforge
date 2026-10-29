package com.josep.polardimension.village;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;

/** A single candidate, independent of world seed. Extending random spread keeps /locate compatible. */
public final class NorthPolePlacement extends RandomSpreadStructurePlacement {
    public static final MapCodec<NorthPolePlacement> CODEC = MapCodec.unit(NorthPolePlacement::new);
    public static final ChunkPos START_CHUNK = new ChunkPos(VillageLayout.CENTER_X >> 4, VillageLayout.CENTER_Z >> 4);

    public NorthPolePlacement() {
        super(1, 0, RandomSpreadType.LINEAR, 117031);
    }

    @Override
    public ChunkPos getPotentialStructureChunk(long seed, int regionX, int regionZ) {
        return START_CHUNK;
    }

    @Override
    protected boolean isPlacementChunk(ChunkGeneratorStructureState state, int x, int z) {
        return x == START_CHUNK.x && z == START_CHUNK.z;
    }

    @Override
    public BlockPos getLocatePos(ChunkPos chunk) {
        return new BlockPos(VillageLayout.CENTER_X, 0, VillageLayout.CENTER_Z);
    }

    @Override
    public StructurePlacementType<?> type() {
        return VillageContent.PLACEMENT.get();
    }
}

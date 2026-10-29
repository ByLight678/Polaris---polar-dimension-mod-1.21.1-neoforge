package com.josep.polardimension.village;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public final class VillageRoadPiece extends StructurePiece {
    private final int surface;

    public VillageRoadPiece(int surface) {
        super(VillageContent.ROADS.get(), 0, new BoundingBox(-VillageLayout.HALF_SIZE, surface,
                VillageLayout.CENTER_Z - VillageLayout.HALF_SIZE, VillageLayout.HALF_SIZE, surface + 5,
                VillageLayout.CENTER_Z + VillageLayout.HALF_SIZE));
        this.surface = surface;
    }

    public VillageRoadPiece(CompoundTag tag) {
        super(VillageContent.ROADS.get(), tag);
        surface = tag.getInt("Surface");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) { tag.putInt("Surface", surface); }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox clip, ChunkPos chunk, BlockPos anchor) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = Math.max(clip.minX(), boundingBox.minX()); x <= Math.min(clip.maxX(), boundingBox.maxX()); x++) {
            for (int z = Math.max(clip.minZ(), boundingBox.minZ()); z <= Math.min(clip.maxZ(), boundingBox.maxZ()); z++) {
                int localZ = z - VillageLayout.CENTER_Z;
                if (VillageLayout.isRoad(x, localZ)) {
                    BlockState paving = Math.floorMod(x + z, 5) == 0 ? VillageRoadBlocks.WARM_COBBLESTONE.get().defaultBlockState() : VillageRoadBlocks.WARM_STONE_BRICKS.get().defaultBlockState();
                    level.setBlock(pos.set(x, surface, z), paving, 2);
                    level.setBlock(pos.set(x, surface + 1, z), Blocks.AIR.defaultBlockState(), 2);
                } else if (VillageLayout.isLanternSpot(x, localZ)) {
                    level.setBlock(pos.set(x, surface + 1, z), Blocks.SPRUCE_FENCE.defaultBlockState(), 2);
                    level.setBlock(pos.set(x, surface + 2, z), Blocks.LANTERN.defaultBlockState(), 2);
                }
            }
        }
    }
}


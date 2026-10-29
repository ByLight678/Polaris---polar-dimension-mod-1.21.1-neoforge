package com.josep.polardimension.worldgen;

import com.josep.polardimension.village.VillageLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public final class WildernessPiece extends TemplateStructurePiece {
    public WildernessPiece(StructureTemplateManager manager, ResourceLocation template, VillageLayout.Plot plot, int surface) {
        super(WildernessContent.PIECE.get(), 0, manager, template, template.toString(), settings(plot.rotation()),
                plot.transformed(0, surface - plot.surfaceLayer(), 0));
    }

    public WildernessPiece(StructureTemplateManager manager, CompoundTag tag) {
        super(WildernessContent.PIECE.get(), tag, manager, id -> settings(Rotation.valueOf(tag.getString("Rotation"))));
    }

    private static StructurePlaceSettings settings(Rotation rotation) {
        return new StructurePlaceSettings().setRotation(rotation).setIgnoreEntities(true)
                .addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        super.addAdditionalSaveData(context, tag);
        tag.putString("Rotation", placeSettings.getRotation().name());
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox clip, ChunkPos chunk, BlockPos anchor) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = Math.max(clip.minX(), boundingBox.minX()); x <= Math.min(clip.maxX(), boundingBox.maxX()); x++) {
            for (int z = Math.max(clip.minZ(), boundingBox.minZ()); z <= Math.min(clip.maxZ(), boundingBox.maxZ()); z++) {
                for (int y = boundingBox.minY() - 1; y >= boundingBox.minY() - 12; y--) {
                    pos.set(x, y, z);
                    var state = level.getBlockState(pos);
                    if (!state.isAir() && !state.is(Blocks.SNOW) && state.getFluidState().isEmpty()) break;
                    if (clip.isInside(pos)) level.setBlock(pos, Blocks.SNOW_BLOCK.defaultBlockState(), 2);
                }
            }
        }
        super.postProcess(level, structures, generator, random, clip, chunk, anchor);
        if (level.getLevel().dimension().equals(com.josep.polardimension.portal.PortalContent.POLARIS)) {
            var loot = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
                    ResourceLocation.fromNamespaceAndPath("polar_dimension", "chests/polaris_wilderness"));
            for (var block : new net.minecraft.world.level.block.Block[] {Blocks.CHEST, Blocks.BARREL}) {
                for (var info : template.filterBlocks(templatePosition, placeSettings, block)) {
                    if (clip.isInside(info.pos()) && level.getBlockEntity(info.pos()) instanceof
                            net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity container) {
                        container.setLootTable(loot, random.nextLong());
                    }
                }
            }
        }
    }

    @Override
    protected void handleDataMarker(String name, BlockPos pos, ServerLevelAccessor level, RandomSource random, BoundingBox box) {}
}

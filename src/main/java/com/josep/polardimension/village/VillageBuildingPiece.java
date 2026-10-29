package com.josep.polardimension.village;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public final class VillageBuildingPiece extends TemplateStructurePiece {
    public VillageBuildingPiece(StructureTemplateManager manager, VillageLayout.Plot plot, int surface) {
        super(VillageContent.BUILDING.get(), 0, manager, VillageContent.template(plot.template()),
                VillageContent.template(plot.template()).toString(), settings(plot.rotation()),
                plot.transformed(0, surface - plot.surfaceLayer(), 0).offset(VillageLayout.CENTER_X, 0, VillageLayout.CENTER_Z));
    }

    public VillageBuildingPiece(StructureTemplateManager manager, CompoundTag tag) {
        super(VillageContent.BUILDING.get(), tag, manager, location -> settings(Rotation.valueOf(tag.getString("Rotation"))));
    }

    private static StructurePlaceSettings settings(Rotation rotation) {
        return new StructurePlaceSettings().setRotation(rotation).setIgnoreEntities(false)
                .addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        super.addAdditionalSaveData(context, tag);
        tag.putString("Rotation", placeSettings.getRotation().name());
    }

    @Override
    public void postProcess(net.minecraft.world.level.WorldGenLevel level, net.minecraft.world.level.StructureManager structures,
                            net.minecraft.world.level.chunk.ChunkGenerator generator, RandomSource random,
                            BoundingBox clip, net.minecraft.world.level.ChunkPos chunk, BlockPos anchor) {
        super.postProcess(level, structures, generator, random, clip, chunk, anchor);
        lightInterior(level, clip);
        if (!level.getLevel().dimension().equals(com.josep.polardimension.portal.PortalContent.POLARIS)) return;
        var loot = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("polar_dimension", "chests/polaris_village"));
        for (var block : new net.minecraft.world.level.block.Block[] {
                net.minecraft.world.level.block.Blocks.CHEST, net.minecraft.world.level.block.Blocks.BARREL}) {
            for (var info : template.filterBlocks(templatePosition, placeSettings, block)) {
                if (clip.isInside(info.pos()) && level.getBlockEntity(info.pos()) instanceof
                        net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity container) {
                    container.setLootTable(loot, random.nextLong());
                }
            }
        }
    }

    @Override
    protected void handleDataMarker(String name, BlockPos pos, ServerLevelAccessor level, RandomSource random, BoundingBox box) {}

    public void lightInterior(ServerLevelAccessor level, BoundingBox clip) {
        VillageInteriorLighting.apply(level, templateName, templatePosition, placeSettings, clip);
    }

    public java.util.Optional<BlockPos> treeSantaSpawn() {
        if (!templateName.equals(VillageContent.template("christmas_town_tree").toString())) return java.util.Optional.empty();
        return java.util.Optional.of(net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate
                .calculateRelativePosition(placeSettings, new BlockPos(17, 1, 36)).offset(templatePosition));
    }

    public java.util.Optional<BlockPos> workshopBossSpawn() {
        if (!templateName.equals(VillageContent.template("christmas_toy_workshop").toString())) return java.util.Optional.empty();
        return java.util.Optional.of(net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate
                .calculateRelativePosition(placeSettings, new BlockPos(17, 1, 12)).offset(templatePosition));
    }
}

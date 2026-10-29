package com.josep.polardimension.village;

import com.josep.polardimension.PolarDimension;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class VillageContent {
    private static final DeferredRegister<StructureType<?>> STRUCTURES = DeferredRegister.create(Registries.STRUCTURE_TYPE, PolarDimension.MOD_ID);
    private static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, PolarDimension.MOD_ID);
    private static final DeferredRegister<StructurePlacementType<?>> PLACEMENTS = DeferredRegister.create(Registries.STRUCTURE_PLACEMENT, PolarDimension.MOD_ID);
    public static final DeferredHolder<StructureType<?>, StructureType<PolarisVillageStructure>> VILLAGE =
            STRUCTURES.register("north_pole_village", () -> () -> PolarisVillageStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> BUILDING = PIECES.register("village_building",
            () -> (context, tag) -> new VillageBuildingPiece(context.structureTemplateManager(), tag));
    public static final DeferredHolder<StructurePieceType, StructurePieceType> ROADS = PIECES.register("village_roads",
            () -> (context, tag) -> new VillageRoadPiece(tag));
    public static final DeferredHolder<StructurePlacementType<?>, StructurePlacementType<NorthPolePlacement>> PLACEMENT =
            PLACEMENTS.register("north_pole", () -> () -> NorthPolePlacement.CODEC);

    private VillageContent() {}

    public static void register(IEventBus bus) {
        STRUCTURES.register(bus);
        PIECES.register(bus);
        PLACEMENTS.register(bus);
    }

    public static ResourceLocation template(String name) {
        return ResourceLocation.fromNamespaceAndPath(PolarDimension.MOD_ID, "village/" + name);
    }
}

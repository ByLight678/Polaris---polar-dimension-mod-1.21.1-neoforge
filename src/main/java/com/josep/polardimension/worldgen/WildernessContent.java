package com.josep.polardimension.worldgen;

import com.josep.polardimension.PolarDimension;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class WildernessContent {
    private static final DeferredRegister<StructureType<?>> STRUCTURES = DeferredRegister.create(Registries.STRUCTURE_TYPE, PolarDimension.MOD_ID);
    private static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, PolarDimension.MOD_ID);
    public static final DeferredHolder<StructureType<?>, StructureType<WildernessStructure>> STRUCTURE =
            STRUCTURES.register("polaris_wilderness", () -> () -> WildernessStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> PIECE = PIECES.register("wilderness_template",
            () -> (context, tag) -> new WildernessPiece(context.structureTemplateManager(), tag));

    private WildernessContent() {}

    public static void register(IEventBus bus) { STRUCTURES.register(bus); PIECES.register(bus); }
}

package com.josep.polardimension.portal;

import com.josep.polardimension.PolarDimension;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.block.SoundType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class PortalContent {
    public static final ResourceKey<Level> POLARIS = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(PolarDimension.MOD_ID, "polaris"));
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(PolarDimension.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, PolarDimension.MOD_ID);
    private static final DeferredRegister<PoiType> POIS =
            DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, PolarDimension.MOD_ID);

    public static final DeferredBlock<PolarisPortalBlock> PORTAL = BLOCKS.register("polaris_portal",
            () -> new PolarisPortalBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                    .noCollission().noOcclusion().strength(-1.0F).sound(SoundType.GLASS)
                    .lightLevel(state -> 11).pushReaction(PushReaction.BLOCK).noLootTable()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PolarisPortalBlockEntity>> PORTAL_ENTITY =
            BLOCK_ENTITIES.register("polaris_portal", () -> BlockEntityType.Builder.of(
                    PolarisPortalBlockEntity::new, PORTAL.get()).build(null));
    public static final DeferredHolder<PoiType, PoiType> PORTAL_POI = POIS.register("polaris_portal",
            () -> new PoiType(Set.copyOf(PORTAL.get().getStateDefinition().getPossibleStates()), 0, 1));

    private PortalContent() {}

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        BLOCK_ENTITIES.register(bus);
        POIS.register(bus);
    }

    public static boolean isTravelDimension(Level level) {
        return level.dimension().equals(Level.OVERWORLD) || level.dimension().equals(POLARIS);
    }
}

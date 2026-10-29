package com.josep.polardimension.entity;

import com.josep.polardimension.PolarDimension;
import com.josep.polardimension.portal.PortalContent;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class PenguinContent {
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, PolarDimension.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(PolarDimension.MOD_ID);
    public static final DeferredHolder<EntityType<?>, EntityType<PenguinEntity>> PENGUIN = ENTITIES.register("penguin",
            () -> EntityType.Builder.of(PenguinEntity::new, MobCategory.CREATURE).sized(0.65F, 1.125F).eyeHeight(0.96F)
                    .clientTrackingRange(10).build(PolarDimension.MOD_ID + ":penguin"));
    public static final DeferredHolder<Item, DeferredSpawnEggItem> SPAWN_EGG = ITEMS.register("penguin_spawn_egg",
            () -> new DeferredSpawnEggItem(PENGUIN, 0x202833, 0xE2DCCA, new Item.Properties()));

    private PenguinContent() {}

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        ITEMS.register(bus);
        bus.addListener(PenguinContent::attributes);
        bus.addListener(PenguinContent::placements);
        bus.addListener(PenguinContent::creativeItems);
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(PENGUIN.get(), PenguinEntity.createAttributes().build());
    }

    private static void placements(RegisterSpawnPlacementsEvent event) {
        event.register(PENGUIN.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                PenguinEntity::checkSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        // Vanilla's alternate bear block tag only includes ice, not Polaris's solid snow.
        // Add a dimension-scoped option instead of changing bear spawning in other dimensions.
        event.register(EntityType.POLAR_BEAR, (type, level, reason, pos, random) ->
                level.getLevel().dimension().equals(PortalContent.POLARIS)
                        && PenguinEntity.checkSpawnRules(type, level, reason, pos, random), RegisterSpawnPlacementsEvent.Operation.OR);
    }

    private static void creativeItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) event.accept(SPAWN_EGG.get());
    }
}

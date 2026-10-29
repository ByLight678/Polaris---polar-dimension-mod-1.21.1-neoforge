package com.josep.polardimension.entity;

import com.josep.polardimension.PolarDimension;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class EvilSnowmanContent {
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, PolarDimension.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(PolarDimension.MOD_ID);
    public static final DeferredHolder<EntityType<?>, EntityType<EvilSnowmanEntity>> EVIL_SNOWMAN = ENTITIES.register("evil_snowman",
            () -> EntityType.Builder.of(EvilSnowmanEntity::new, MobCategory.MONSTER).sized(0.9F, 2.25F).eyeHeight(1.65F)
                    .clientTrackingRange(10)
                    .build(PolarDimension.MOD_ID + ":evil_snowman"));
    public static final DeferredHolder<Item, DeferredSpawnEggItem> SPAWN_EGG = ITEMS.register("evil_snowman_spawn_egg",
            () -> new DeferredSpawnEggItem(EVIL_SNOWMAN, 0xE9F4F4, 0xB51628, new Item.Properties()));

    private EvilSnowmanContent() {}

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        ITEMS.register(bus);
        bus.addListener(EvilSnowmanContent::attributes);
        bus.addListener(EvilSnowmanContent::placements);
        bus.addListener(EvilSnowmanContent::creativeItems);
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(EVIL_SNOWMAN.get(), EvilSnowmanEntity.createAttributes().build());
    }

    private static void placements(RegisterSpawnPlacementsEvent event) {
        event.register(EVIL_SNOWMAN.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private static void creativeItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) event.accept(SPAWN_EGG.get());
    }
}


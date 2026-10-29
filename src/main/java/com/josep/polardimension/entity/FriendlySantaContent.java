package com.josep.polardimension.entity;

import com.josep.polardimension.PolarDimension;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class FriendlySantaContent {
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, PolarDimension.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(PolarDimension.MOD_ID);
    public static final DeferredHolder<EntityType<?>, EntityType<FriendlySantaEntity>> SANTA = ENTITIES.register("giant_santa",
            () -> EntityType.Builder.of(FriendlySantaEntity::new, MobCategory.CREATURE).sized(1.8F, 3.45F).eyeHeight(2.65F)
                    .clientTrackingRange(10)
                    .build(PolarDimension.MOD_ID + ":giant_santa"));
    public static final DeferredHolder<Item, DeferredSpawnEggItem> SPAWN_EGG = ITEMS.register("giant_santa_spawn_egg",
            () -> new DeferredSpawnEggItem(SANTA, 0xC72B30, 0xFFFFFF, new Item.Properties()));

    private FriendlySantaContent() {}

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        ITEMS.register(bus);
        bus.addListener(FriendlySantaContent::attributes);
        bus.addListener(FriendlySantaContent::creativeItems);
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(SANTA.get(), FriendlySantaEntity.createAttributes().build());
    }

    private static void creativeItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) event.accept(SPAWN_EGG.get());
    }
}




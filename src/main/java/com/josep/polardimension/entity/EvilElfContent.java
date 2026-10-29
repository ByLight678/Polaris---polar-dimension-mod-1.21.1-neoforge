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

public final class EvilElfContent {
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, PolarDimension.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(PolarDimension.MOD_ID);
    public static final DeferredHolder<EntityType<?>, EntityType<EvilElfEntity>> EVIL_ELF = ENTITIES.register("evil_christmas_elf",
            () -> EntityType.Builder.of(EvilElfEntity::new, MobCategory.MONSTER).sized(0.6F, 1.7F).eyeHeight(1.2F)
                    .clientTrackingRange(10)
                    .build(PolarDimension.MOD_ID + ":evil_christmas_elf"));
    public static final DeferredHolder<Item, DeferredSpawnEggItem> SPAWN_EGG = ITEMS.register("evil_christmas_elf_spawn_egg",
            () -> new DeferredSpawnEggItem(EVIL_ELF, 0x7E1720, 0xAEB2B3, new Item.Properties()));

    private EvilElfContent() {}

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        ITEMS.register(bus);
        bus.addListener(EvilElfContent::attributes);
        bus.addListener(EvilElfContent::creativeItems);
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(EVIL_ELF.get(), EvilElfEntity.createAttributes().build());
    }

    private static void creativeItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) event.accept(SPAWN_EGG.get());
    }
}




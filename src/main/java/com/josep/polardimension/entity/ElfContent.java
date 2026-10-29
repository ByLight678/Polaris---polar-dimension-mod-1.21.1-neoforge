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

public final class ElfContent {
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, PolarDimension.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(PolarDimension.MOD_ID);
    public static final DeferredHolder<EntityType<?>, EntityType<ElfEntity>> MALE_ELF = ENTITIES.register("christmas_elf",
            () -> EntityType.Builder.of(ElfEntity::new, MobCategory.CREATURE).sized(0.6F, 1.7F).eyeHeight(1.06F)
                    .clientTrackingRange(8).build(PolarDimension.MOD_ID + ":christmas_elf"));
    public static final DeferredHolder<EntityType<?>, EntityType<ElfEntity>> FEMALE_ELF = ENTITIES.register("female_christmas_elf",
            () -> EntityType.Builder.of(ElfEntity::new, MobCategory.CREATURE).sized(0.6F, 1.7F).eyeHeight(1.06F)
                    .clientTrackingRange(8).build(PolarDimension.MOD_ID + ":female_christmas_elf"));
    public static final DeferredHolder<Item, DeferredSpawnEggItem> MALE_EGG = ITEMS.register("christmas_elf_spawn_egg",
            () -> new DeferredSpawnEggItem(MALE_ELF, 0x277143, 0xA73837, new Item.Properties()));
    public static final DeferredHolder<Item, DeferredSpawnEggItem> FEMALE_EGG = ITEMS.register("female_christmas_elf_spawn_egg",
            () -> new DeferredSpawnEggItem(FEMALE_ELF, 0xB33D49, 0x347447, new Item.Properties()));

    private ElfContent() {}

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        ITEMS.register(bus);
        bus.addListener(ElfContent::attributes);
        bus.addListener(ElfContent::creativeItems);
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(MALE_ELF.get(), ElfEntity.createAttributes().build());
        event.put(FEMALE_ELF.get(), ElfEntity.createAttributes().build());
    }

    private static void creativeItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(MALE_EGG.get());
            event.accept(FEMALE_EGG.get());
        }
    }
}

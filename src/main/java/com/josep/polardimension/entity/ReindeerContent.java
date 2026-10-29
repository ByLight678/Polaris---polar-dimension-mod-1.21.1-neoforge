package com.josep.polardimension.entity;

import com.josep.polardimension.PolarDimension;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ReindeerContent {
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, PolarDimension.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(PolarDimension.MOD_ID);
    public static final DeferredHolder<EntityType<?>, EntityType<ReindeerEntity>> REINDEER = ENTITIES.register("reindeer",
            () -> EntityType.Builder.of(ReindeerEntity::new, MobCategory.CREATURE).sized(1.3964844F, 1.6F).eyeHeight(1.52F)
                    .passengerAttachments(new Vec3(0.0D, 1.6D, 0.125D)).clientTrackingRange(10)
                    .build(PolarDimension.MOD_ID + ":reindeer"));
    public static final DeferredHolder<Item, DeferredSpawnEggItem> SPAWN_EGG = ITEMS.register("reindeer_spawn_egg",
            () -> new DeferredSpawnEggItem(REINDEER, 0x6A4B35, 0xD4C8A6, new Item.Properties()));

    private ReindeerContent() {}

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        ITEMS.register(bus);
        bus.addListener(ReindeerContent::attributes);
        bus.addListener(ReindeerContent::creativeItems);
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(REINDEER.get(), AbstractHorse.createBaseHorseAttributes().build());
    }

    private static void creativeItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) event.accept(SPAWN_EGG.get());
    }
}

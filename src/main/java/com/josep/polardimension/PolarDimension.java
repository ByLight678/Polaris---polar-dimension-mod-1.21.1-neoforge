package com.josep.polardimension;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.josep.polardimension.worldgen.PolarisChunkGenerator;
import com.josep.polardimension.worldgen.WildernessContent;
import com.josep.polardimension.portal.PortalContent;
import com.josep.polardimension.village.VillageContent;
import com.josep.polardimension.village.VillageRoadBlocks;
import com.josep.polardimension.entity.ElfContent;
import com.josep.polardimension.entity.ReindeerContent;
import com.josep.polardimension.entity.PenguinContent;
import com.josep.polardimension.entity.EvilSnowmanContent;
import com.josep.polardimension.entity.SantaContent;
import com.josep.polardimension.entity.EvilElfContent;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

@Mod(PolarDimension.MOD_ID)
public final class PolarDimension {
    public static final String MOD_ID = "polar_dimension";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, MOD_ID);

    static {
        CHUNK_GENERATORS.register("polaris", () -> PolarisChunkGenerator.CODEC);
    }

    public PolarDimension(IEventBus modEventBus) {
        CHUNK_GENERATORS.register(modEventBus);
        PortalContent.register(modEventBus);
        VillageContent.register(modEventBus);
        WildernessContent.register(modEventBus);
        VillageRoadBlocks.register(modEventBus);
        ElfContent.register(modEventBus);
        ReindeerContent.register(modEventBus);
        PenguinContent.register(modEventBus);
        EvilSnowmanContent.register(modEventBus);
        SantaContent.register(modEventBus);
        EvilElfContent.register(modEventBus);
        com.josep.polardimension.entity.FriendlySantaContent.register(modEventBus);
        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Polar Dimension initialized");
    }
}








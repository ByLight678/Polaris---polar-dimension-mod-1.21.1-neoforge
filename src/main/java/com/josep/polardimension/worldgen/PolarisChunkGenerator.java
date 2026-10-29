package com.josep.polardimension.worldgen;

import com.mojang.serialization.MapCodec;
import com.josep.polardimension.village.VillageLayout;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

/** Paints the frozen crust relative to each column's actual terrain surface. */
public final class PolarisChunkGenerator extends NoiseBasedChunkGenerator {
    public static final MapCodec<PolarisChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(PolarisChunkGenerator::getBiomeSource),
            NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(PolarisChunkGenerator::generatorSettings)
    ).apply(instance, PolarisChunkGenerator::new));

    public PolarisChunkGenerator(BiomeSource biomeSource, Holder<NoiseGeneratorSettings> settings) {
        super(biomeSource, settings);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structures, RandomState random, ChunkAccess chunk) {
        super.buildSurface(level, structures, random, chunk);
        applyFrozenCrust(random, chunk, true);
    }

    @Override
    public void applyCarvers(WorldGenRegion level, long seed, RandomState random, BiomeManager biomes,
                             StructureManager structures, ChunkAccess chunk, GenerationStep.Carving step) {
        super.applyCarvers(level, seed, random, biomes, structures, chunk, step);
        // Carvers can replace packed ice. Restore the shallow crust before ore decoration.
        applyFrozenCrust(random, chunk, false);
    }

    private void applyFrozenCrust(RandomState random, ChunkAccess chunk, boolean gradeVillage) {
        NormalNoise snowDepthNoise = random.getOrCreateNoise(Noises.SURFACE);
        NormalNoise iceDepthNoise = random.getOrCreateNoise(Noises.SURFACE_SECONDARY);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean nearVillage = VillageLayout.distance(chunk.getPos().getMiddleBlockX(), chunk.getPos().getMiddleBlockZ())
                <= VillageLayout.BLEND_EDGE + 16;
        int villageSurface = gradeVillage && nearVillage ? VillageLayout.surfaceY(this, chunk, random) : 0;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunk.getPos().getMinBlockX() + x;
                int worldZ = chunk.getPos().getMinBlockZ() + z;
                int surface = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
                if (gradeVillage && nearVillage) {
                    int distance = VillageLayout.distance(worldX, worldZ);
                    if (distance < VillageLayout.BLEND_EDGE) {
                        double blend = Mth.clamp((distance - VillageLayout.HALF_SIZE)
                                / (double) (VillageLayout.BLEND_EDGE - VillageLayout.HALF_SIZE), 0.0, 1.0);
                        blend = blend * blend * (3.0 - 2.0 * blend);
                        int target = (int) Math.round(Mth.lerp(blend, villageSurface, surface));
                        for (int y = surface; y > target; y--) {
                            chunk.setBlockState(pos.set(worldX, y, worldZ), Blocks.AIR.defaultBlockState(), false);
                        }
                        for (int y = surface + 1; y <= target; y++) {
                            chunk.setBlockState(pos.set(worldX, y, worldZ), Blocks.DEEPSLATE.defaultBlockState(), false);
                        }
                        surface = target;
                    }
                }
                int snowDepth = 3 + Mth.clamp((int) Math.floor((snowDepthNoise.getValue(worldX, 0, worldZ) + 1) * 1.5), 0, 2);
                int deepslateDepth = 10 + Mth.clamp((int) Math.floor((iceDepthNoise.getValue(worldX, 0, worldZ) + 1) * 3), 0, 5);
                for (int depth = 0; depth < deepslateDepth && surface - depth >= chunk.getMinBuildHeight(); depth++) {
                    pos.set(worldX, surface - depth, worldZ);
                    // Seal shallow cave pockets to keep the crust continuous; retain bedrock.
                    if (!chunk.getBlockState(pos).is(Blocks.BEDROCK)) {
                        BlockState state = depth < snowDepth
                                ? Blocks.SNOW_BLOCK.defaultBlockState() : Blocks.PACKED_ICE.defaultBlockState();
                        chunk.setBlockState(pos, state, false);
                    }
                }
            }
        }
    }
}



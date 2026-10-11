package com.aearost.aranarthcore.objects;

import com.aearost.aranarthcore.utils.PlotUtils;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;

import java.util.List;
import java.util.Random;

/**
 * Generates the Creative plots world as an endless grid of plots and roads.
 */
public class PlotChunkGenerator extends ChunkGenerator {

    @Override
    public void generateNoise(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, ChunkData chunkData) {
        PlotPattern pattern = PlotUtils.getPattern();
        int top = Math.max(PlotPattern.SURFACE_Y, pattern.getMaxY());
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                PlotPattern.Column column = pattern.resolve(chunkX * 16 + x, chunkZ * 16 + z, PlotUtils.MERGE_LOOKUP);
                for (int y = chunkData.getMinHeight(); y <= top; y++) {
                    chunkData.setBlock(x, y, z, pattern.getBlock(column, y));
                }
            }
        }
    }

    @Override
    public BiomeProvider getDefaultBiomeProvider(WorldInfo worldInfo) {
        return new BiomeProvider() {
            @Override
            public Biome getBiome(WorldInfo worldInfo, int x, int y, int z) {
                return Biome.PLAINS;
            }

            @Override
            public List<Biome> getBiomes(WorldInfo worldInfo) {
                return List.of(Biome.PLAINS);
            }
        };
    }

    @Override
    public Location getFixedSpawnLocation(World world, Random random) {
        return PlotUtils.getSpawnLocation(world);
    }

    @Override
    public boolean shouldGenerateNoise() {
        return false;
    }

    @Override
    public boolean shouldGenerateSurface() {
        return false;
    }

    @Override
    public boolean shouldGenerateBedrock() {
        return false;
    }

    @Override
    public boolean shouldGenerateCaves() {
        return false;
    }

    @Override
    public boolean shouldGenerateDecorations() {
        return false;
    }

    @Override
    public boolean shouldGenerateMobs() {
        return false;
    }

    @Override
    public boolean shouldGenerateStructures() {
        return false;
    }
}

/*
 *    Copyright 2017 Benjamin K (darkevilmac)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.elytradev.teckle.common.worldgen;

import com.elytradev.teckle.common.TeckleConfiguration;
import com.elytradev.teckle.common.TeckleMod;
import com.elytradev.teckle.common.TeckleObjects;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkGeneratorSettings;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.feature.WorldGenMinable;
import net.minecraft.world.gen.feature.WorldGenerator;
import net.minecraftforge.fml.common.IWorldGenerator;

import java.util.Random;

/**
 * Created by darkevilmac on 4/21/2017.
 */
public class NikoliteOreGenerator implements IWorldGenerator {
    private WorldGenMinable nikoliteGen;

    protected void genOre(World worldIn, int chunkX, int chunkZ, Random random, int blockCount, WorldGenerator generator, int minHeight, int maxHeight) {
        if (maxHeight < minHeight) {
            int i = minHeight;
            minHeight = maxHeight;
            maxHeight = i;
        } else if (maxHeight == minHeight) {
            if (minHeight < 255) {
                ++maxHeight;
            } else {
                --minHeight;
            }
        }

        for (int j = 0; j < blockCount; ++j) {
            BlockPos blockPos = new BlockPos(chunkX * 16, 0, chunkZ * 16);
            blockPos = blockPos.add(random.nextInt(16), random.nextInt(maxHeight - minHeight) + minHeight, random.nextInt(16));
            generator.generate(worldIn, random, blockPos);
        }
    }


    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator chunkGenerator, IChunkProvider chunkProvider) {
        TeckleConfiguration configuration = TeckleMod.CONFIG;
        if (world.provider.getDimension() == 0) {
            if (nikoliteGen == null) {
                this.nikoliteGen = new WorldGenMinable(TeckleObjects.blockNikoliteOre.getDefaultState(), configuration.nikoliteSize);
            }

            this.genOre(world, chunkX, chunkZ, random, configuration.nikoliteCount, this.nikoliteGen, configuration.nikoliteMinHeight, configuration.nikoliteMaxHeight);
        }
    }
}

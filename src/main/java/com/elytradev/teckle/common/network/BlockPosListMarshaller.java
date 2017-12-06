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

package com.elytradev.teckle.common.network;

import com.elytradev.concrete.network.Marshaller;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;

import java.util.ArrayList;

/**
 * Simple block position list marshaller, holding onto this in case it comes in handy.
 */
public class BlockPosListMarshaller implements Marshaller<ArrayList<BlockPos>> {

    public static final String MARSHALLER_NAME = "com.elytradev.teckle.common.network.BlockPosListMarshaller";
    public static final BlockPosListMarshaller INSTANCE = new BlockPosListMarshaller();

    @Override
    public ArrayList<BlockPos> unmarshal(ByteBuf in) {
        ArrayList<BlockPos> out = new ArrayList<>();
        int size = ByteBufUtils.readVarInt(in, 3);

        for (int i = 0; i < size; i++) {
            out.add(BlockPos.fromLong(in.readLong()));
        }

        return out;
    }

    @Override
    public void marshal(ByteBuf out, ArrayList<BlockPos> blockPoss) {
        ByteBufUtils.writeVarInt(out, blockPoss.size(), 3);

        for (BlockPos blockPos : blockPoss) {
            out.writeLong(blockPos.toLong());
        }
    }
}

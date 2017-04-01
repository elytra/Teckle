package com.elytradev.teckle.common.network;

import com.elytradev.concrete.Marshaller;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;

import java.util.ArrayList;
import java.util.List;

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

        for (int i = 0; i < blockPoss.size(); i++) {
            out.writeLong(blockPoss.get(i).toLong());
        }
    }
}

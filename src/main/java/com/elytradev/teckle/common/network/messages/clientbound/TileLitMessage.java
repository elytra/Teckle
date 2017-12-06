package com.elytradev.teckle.common.network.messages.clientbound;

import com.elytradev.concrete.network.Message;
import com.elytradev.concrete.network.NetworkContext;
import com.elytradev.concrete.network.annotation.type.ReceivedOn;
import com.elytradev.teckle.common.network.TeckleNetworking;
import com.elytradev.teckle.common.network.messages.TeckleMessage;
import com.elytradev.teckle.common.tile.TileLitNetworkMember;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Sends a message to clients whenever a tile lights up, used on the sorting machine and the retriever.
 */
@ReceivedOn(Side.CLIENT)
public class TileLitMessage extends TeckleMessage {

    public boolean isLit;
    public BlockPos networkMemberPos;

    public TileLitMessage(NetworkContext ctx) {
    }

    public TileLitMessage(TileLitNetworkMember networkMember) {
        networkMember.setLit(networkMember.isLit() || networkMember.getWorld().isBlockPowered(networkMember.getPos()));
        this.isLit = networkMember.isLit();
        this.networkMemberPos = networkMember.getPos();
    }

    @Override
    protected void handle(EntityPlayer sender) {
        if (sender != null && sender.world != null) {
            TileLitNetworkMember networkMember = (TileLitNetworkMember) sender.world.getTileEntity(networkMemberPos);
            networkMember.setLit(isLit);
        }
    }
}

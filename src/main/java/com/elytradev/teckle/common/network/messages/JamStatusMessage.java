package com.elytradev.teckle.common.network.messages;

import com.elytradev.concrete.network.Message;
import com.elytradev.concrete.network.NetworkContext;
import com.elytradev.concrete.network.annotation.type.ReceivedOn;
import com.elytradev.teckle.common.network.TeckleNetworking;
import com.elytradev.teckle.common.tile.base.TileNetworkMember;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Handles jam messages for clients.
 */
@ReceivedOn(Side.CLIENT)
public class JamStatusMessage extends Message {

    public boolean isJammed;
    public BlockPos networkMemberPos;

    public JamStatusMessage(NetworkContext ctx) {
        super(ctx);
    }

    public JamStatusMessage(TileNetworkMember networkMember) {
        super(TeckleNetworking.NETWORK);
        networkMember.setJammed(networkMember.isJammed() || networkMember.getWorld().isBlockPowered(networkMember.getPos()));
        this.isJammed = networkMember.isJammed();
        this.networkMemberPos = networkMember.getPos();
    }

    @Override
    protected void handle(EntityPlayer sender) {
        if (sender != null && sender.world != null) {
            TileNetworkMember networkMember = (TileNetworkMember) sender.world.getTileEntity(networkMemberPos);
            networkMember.setJammed(isJammed);
        }
    }
}
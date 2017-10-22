package com.elytradev.teckle.common.container;

import com.elytradev.teckle.common.tile.retriever.TileRetriever;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;

public class ContainerRetriever extends Container {

    public TileRetriever retriever;
    public EntityPlayer player;

    public ContainerRetriever(TileRetriever retriever, EntityPlayer player) {
        this.retriever = retriever;
        this.player = player;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return retriever.isUsableByPlayer(player);
    }
}

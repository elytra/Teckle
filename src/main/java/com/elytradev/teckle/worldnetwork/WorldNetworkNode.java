package com.elytradev.teckle.worldnetwork;

import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * A node in a worldnetwork, contains the position and the current travellers.
 */
public class WorldNetworkNode {

    public static final WorldNetworkNode NONE = new WorldNetworkNode();

    public BlockPos position;
    private List<WorldNetworkTraveller> travellers;
}

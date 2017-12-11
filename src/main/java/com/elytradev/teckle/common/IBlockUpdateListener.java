package com.elytradev.teckle.common;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorldEventListener;

import javax.annotation.Nullable;

/**
 * A version of IWorldEventListener that defaults all methods besides notifyBlockUpdate, used for lambdas.
 */
public interface IBlockUpdateListener extends IWorldEventListener {

    @Override
    default void notifyLightSet(BlockPos pos) {

    }

    @Override
    default void markBlockRangeForRenderUpdate(int x1, int y1, int z1, int x2, int y2, int z2) {

    }

    @Override
    default void playSoundToAllNearExcept(@Nullable EntityPlayer player, SoundEvent soundIn, SoundCategory category, double x, double y, double z, float volume, float pitch) {

    }

    @Override
    default void playRecord(SoundEvent soundIn, BlockPos pos) {

    }

    @Override
    default void spawnParticle(int particleID, boolean ignoreRange, double xCoord, double yCoord, double zCoord, double xSpeed, double ySpeed, double zSpeed, int... parameters) {

    }

    @Override
    default void spawnParticle(int id, boolean ignoreRange, boolean p_190570_3_, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, int... parameters) {

    }

    @Override
    default void onEntityAdded(Entity entityIn) {

    }

    @Override
    default void onEntityRemoved(Entity entityIn) {

    }

    @Override
    default void broadcastSound(int soundID, BlockPos pos, int data) {

    }

    @Override
    default void playEvent(EntityPlayer player, int type, BlockPos blockPosIn, int data) {

    }

    @Override
    default void sendBlockBreakProgress(int breakerId, BlockPos pos, int progress) {

    }
}

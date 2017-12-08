package com.elytradev.teckle.common.network.messages.clientbound;

import com.elytradev.concrete.network.NetworkContext;
import com.elytradev.concrete.network.annotation.field.MarshalledAs;
import com.elytradev.concrete.network.annotation.type.ReceivedOn;
import com.elytradev.teckle.common.network.messages.TeckleMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Objects;

/**
 * Handles debug messages for developers, currently just for me.
 */
@ReceivedOn(Side.CLIENT)
public class DebugReceiverMessage extends TeckleMessage {

    public static boolean ACTIVE;
    @MarshalledAs("string")
    public String message = "";

    public DebugReceiverMessage(NetworkContext ctx) {
        super();
    }

    public DebugReceiverMessage(String message) {
        this.message = message;
    }

    @Override
    protected void handle(EntityPlayer receiver) {
        if (receiver != null && Objects.equals(receiver.getGameProfile().getName(), "darkevilmac") && ACTIVE) {
            Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new TextComponentString(message));
        }
    }
}

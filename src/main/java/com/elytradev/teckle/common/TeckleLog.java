package com.elytradev.teckle.common;

import com.elytradev.teckle.common.network.messages.DebugReceiverMessage;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.launchwrapper.Launch;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;

public class TeckleLog {

    public static Logger LOG;
    public static boolean INDEV = (Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment");
    @Nullable
    public static EntityPlayer developerPlayer;

    public TeckleLog(Logger modLog) {
        LOG = modLog;
    }

    public void debug(String s, Object... o) {
        if (INDEV) {
            LOG.info("[DEBUG] " + s, o);
        }

        if (developerPlayer != null) {
            new DebugReceiverMessage("§b" + (String.format(s, o))).sendTo(developerPlayer);
        }
    }

    public void debug(int i) {
        debug(String.valueOf(i));
    }

    public void error(String s, Object... o) {
        LOG.error(s, o);

        if (developerPlayer != null) {
            new DebugReceiverMessage("§c" + (String.format(s, o))).sendTo(developerPlayer);
        }
    }

    public void info(String s, Object... o) {
        LOG.info(s, o);
    }

    public void warn(String s, Object... o) {
        LOG.warn(s, o);
    }
}

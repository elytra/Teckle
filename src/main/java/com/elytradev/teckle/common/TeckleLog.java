package com.elytradev.teckle.common;

import com.elytradev.teckle.common.network.messages.DebugReceiverMessage;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.launchwrapper.Launch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TeckleLog {

    public static final Logger LOG = LogManager.getLogger("teckle");
    public static final boolean INDEV = (Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment");

    public static EntityPlayer developerPlayer;

    public static void debug(Object message) {
        if (INDEV)
            LOG.debug(message);

        if (developerPlayer != null)
            new DebugReceiverMessage(String.valueOf(message)).sendTo(developerPlayer);
    }

    public static void debug(Object message, Throwable t) {
        if (INDEV)
            LOG.debug(message, t);

        if (developerPlayer != null)
            new DebugReceiverMessage(String.format(String.valueOf(message), t)).sendTo(developerPlayer);
    }

    public static void debug(String message) {
        if (INDEV)
            LOG.debug(message);

        if (developerPlayer != null)
            new DebugReceiverMessage(String.valueOf(message)).sendTo(developerPlayer);
    }

    public static void debug(String message, Object... params) {
        if (INDEV)
            LOG.info(message, params);

        if (developerPlayer != null)
            new DebugReceiverMessage(String.format(String.valueOf(message), params)).sendTo(developerPlayer);
    }

    public static void debug(String message, Throwable t) {
        if (INDEV)
            LOG.debug(message, t);

        if (developerPlayer != null)
            new DebugReceiverMessage(String.format(String.valueOf(message), t)).sendTo(developerPlayer);
    }

    public static void error(Object message) {
        LOG.error(message);

        if (developerPlayer != null)
            new DebugReceiverMessage(String.valueOf(message)).sendTo(developerPlayer);
    }

    public static void error(Object message, Throwable t) {
        LOG.error(message, t);

        if (developerPlayer != null)
            new DebugReceiverMessage(String.format(String.valueOf(message), t)).sendTo(developerPlayer);
    }

    public static void error(String message) {
        LOG.error(message);

        if (developerPlayer != null)
            new DebugReceiverMessage(String.valueOf(message)).sendTo(developerPlayer);
    }

    public static void error(String message, Object... params) {
        LOG.error(message, params);

        if (developerPlayer != null)
            new DebugReceiverMessage(String.format(String.valueOf(message), params)).sendTo(developerPlayer);
    }


    public static void error(String message, Throwable t) {
        LOG.error(message, t);

        if (developerPlayer != null)
            new DebugReceiverMessage(String.format(String.valueOf(message), t)).sendTo(developerPlayer);
    }

    public static void fatal(Object message) {
        LOG.fatal(message);
    }

    public static void fatal(Object message, Throwable t) {
        LOG.fatal(message, t);
    }

    public static void fatal(String message) {
        LOG.fatal(message);
    }

    public static void fatal(String message, Object... params) {
        LOG.fatal(message, params);
    }

    public static void fatal(String message, Throwable t) {
        LOG.fatal(message, t);
    }

    public static void info(Object message) {
        LOG.info(message);
    }

    public static void info(Object message, Throwable t) {
        LOG.info(message, t);
    }

    public static void info(String message) {
        LOG.info(message);
    }

    public static void info(String message, Object... params) {
        LOG.info(message, params);
    }

    public static void info(String message, Throwable t) {
        LOG.info(message, t);
    }

    public static void warn(Object message) {
        LOG.warn(message);
    }

    public static void warn(Object message, Throwable t) {
        LOG.warn(message, t);
    }

    public static void warn(String message) {
        LOG.warn(message);
    }

    public static void warn(String message, Object... params) {
        LOG.warn(message, params);
    }
}

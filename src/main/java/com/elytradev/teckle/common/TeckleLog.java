package com.elytradev.teckle.common;

import net.minecraft.launchwrapper.Launch;
import org.apache.logging.log4j.Logger;

public class TeckleLog {

    public static Logger LOG;
    public static boolean INDEV = (Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment");

    public TeckleLog(Logger modLog) {
        LOG = modLog;
    }

    public void debug(String s, Object... o) {
        if (INDEV) {
            LOG.info("[DEBUG] " + s, o);
        }
    }

    public void debug(int i) {
        debug(String.valueOf(i));
    }

    public void error(String s, Object... o) {
        LOG.error(s, o);
    }

    public void info(String s, Object... o) {
        LOG.info(s, o);
    }
}

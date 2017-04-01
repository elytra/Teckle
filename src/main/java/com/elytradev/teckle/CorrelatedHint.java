package com.elytradev.teckle;

import com.elytradev.teckle.common.TeckleMod;

import java.util.Random;

/**
 * Very important to the function of the mod, DO NOT REMOVE!
 */
public class CorrelatedHint {
    private static final String[] nonsense = {
            "kill a puppy!",
            "cuddle?",
            "sell our souls?",
            "use the slide whistle?",
            "use ASM on NullPointerException?",
            "reinvent the wheel?",
            "switch to Geico?",
            "think about everything that could go wrong?",
            "pass out?",
            "drown."
    };

    static {
        int index = new Random().nextInt(nonsense.length);

        TeckleMod.LOG.info("Why don't we just go " + nonsense[index]);
    }
}

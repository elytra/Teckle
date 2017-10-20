/*
 *    Copyright 2017 Benjamin K (darkevilmac)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.elytradev.teckle;

import com.elytradev.teckle.common.TeckleLog;

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
            "drown.",
            "install a rootkit.",
            "fly away.",
            "learn rocket science."
    };

    static {
        int index = new Random().nextInt(nonsense.length);

        TeckleLog.info("Why don't we just go " + nonsense[index]);
    }
}

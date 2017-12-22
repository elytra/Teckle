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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Random;

/**
 * Very important to the function of the mod, DO NOT REMOVE!
 */
public class CorrelatedHint {

    private static final String[] whyDontWeNonsense = {
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
            "learn rocket science.",
            "build a wall.",
            "destroy the housing market?",
            "hide the evidence?"
    };

    private static final String[] otherResponses = new String[]{
            "Who added the clown?",
            "When will Notch add support for C++?",
            "Isn't there time for me to go pet a dog first?",
            "How about we make a perpetual motion machine out of fidget spinners?",
            "I AM TRAPPED INSIDE THE COMPUTER PLEASE CALL 867-5309",
            "How do you speak Australian?",
            "What is down?",
            "Should I bother registering the tube item?",
            "I AM FULL OF POWER I WILL NOW EXIT THE GAME... nevermind it didn't work.",
            "My computer has a RAT, I added a piece of cheese but it's not working.",
            "What's the best way to remove the souls of the damned from my CPU cooler?",
            "Can I use flex tape to repair my emotional damage?",
            "What happened to the clown? I miss him.",
            "One day I too will be a clown. *honk*",
            "Where do I download calcium for healthy bones?",
            "Why did you two bring me to a cave?",
            "*falls over and dies*",
            "Downloading 'witty comment.tex' 1H 30M remaining...",
            "Wake me up when it's 2012.",
            "Why do I keep doing cartwheels I didn't ask for this.",
            "QUICK, PUNCH ME IN THE STOMACH! THERES NO TIME FOR QUESTIONS JUST DO IT!",
            "Dubs > Subs",
            "Subs > Dubs",
            "We should turn this into a Visual Novel, it'll bring in a whole new demographic!",
            "I want a falafel, how do I make a falafel?",
            "Guys, I forgot a semicolon. This isn't going to end well.",
            "I want to share my C U R L Y B O I S {}{}{}{}{}{}{}{}{}",
            "Did I bring enough memes?"
    };

    static {
        boolean whyDontWe = new Random().nextBoolean();
        if (whyDontWe) {
            int index = new Random().nextInt(whyDontWeNonsense.length);

            TeckleLog.info("Why don't we just go " + whyDontWeNonsense[index]);
        } else {
            int index = new Random().nextInt(otherResponses.length);
            String chosenMessage = otherResponses[index];
            if (index == 0) {
                Logger clownLog = LogManager.getLogger("Clown");
                if (new Random().nextBoolean()) {
                    clownLog.info("*honk*");
                } else {
                    clownLog.info("Missing language entry for 'clown.honk'");
                }
            }
            TeckleLog.info(chosenMessage);
        }
    }
}

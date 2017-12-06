package com.elytradev.teckle.common.network.messages;

import com.elytradev.concrete.network.Message;
import com.elytradev.teckle.common.network.TeckleNetworking;

public abstract class TeckleMessage extends Message {
    public TeckleMessage() {
        super(TeckleNetworking.NETWORK);
    }
}

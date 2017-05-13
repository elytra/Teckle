package com.elytradev.notconcreteipromise.resgen;

/**
 * Thrown when a ConcreteResourcePack can't find a valid fallback pack to use.
 */
public class MissingRealpackException extends RuntimeException {
    public MissingRealpackException(String modID) {
        super("Missing valid fallback pack for " + modID);
    }
}

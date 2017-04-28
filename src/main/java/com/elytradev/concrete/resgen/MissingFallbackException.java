package com.elytradev.concrete.resgen;

/**
 * Thrown when a SimpleResourcePack can't find a valid fallback pack to use.
 */
public class MissingFallbackException extends RuntimeException {
    public MissingFallbackException(String modID) {
        super("Missing valid fallback pack for " + modID);
    }
}

package com.elytradev.teckle.common.exception;

public class MissingOreException extends RuntimeException {
    public MissingOreException(String data) {
        super("Teckle is missing ores, " + data.replaceAll("\n", ""));
    }
}

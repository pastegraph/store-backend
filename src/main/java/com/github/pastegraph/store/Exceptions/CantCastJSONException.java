package com.github.pastegraph.store.Exceptions;

public class CantCastJSONException extends Exception {
    public CantCastJSONException(String message) {
        super(message + "\n");
    }
}

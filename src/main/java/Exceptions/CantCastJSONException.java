package Exceptions;

public class CantCastJSONException extends Exception {
    public CantCastJSONException(String message) {
        super(message + "\n");
    }
}

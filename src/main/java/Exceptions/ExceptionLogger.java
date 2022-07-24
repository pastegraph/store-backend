package Exceptions;

public class ExceptionLogger {

    public static void log(Throwable e) {
        System.err.println(e.getMessage());
        e.printStackTrace(System.err);
    }
}

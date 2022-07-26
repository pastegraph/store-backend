package com.github.pastegraph.store.Exceptions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ExceptionLogger {

    static Path logPath = Path.of(System.getProperty("user.home") + File.separator + "pastegraphLogs.txt");

    public static synchronized void log(Throwable e) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd h:mm");
        StringBuilder log = new StringBuilder();
        log.append("\n**********\n");
        log.append(simpleDateFormat.format(new Date())).append("\n").append(e.getMessage());
        for (StackTraceElement temp : e.getStackTrace()) {
            log.append(temp.toString()).append("\n");
        }

        try {
            Files.writeString(logPath, log, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }
}

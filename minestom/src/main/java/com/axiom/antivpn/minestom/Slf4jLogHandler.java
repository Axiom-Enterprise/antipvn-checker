package com.axiom.antivpn.minestom;

import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

final class Slf4jLogHandler extends Handler {

    private final @NotNull org.slf4j.Logger target;
    private final @NotNull SimpleFormatter formatter = new SimpleFormatter();

    private Slf4jLogHandler(@NotNull org.slf4j.Logger target) {
        this.target = target;
        setLevel(Level.ALL);
    }

    static @NotNull Logger create(@NotNull String name) {
        Logger logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.ALL);
        logger.addHandler(new Slf4jLogHandler(LoggerFactory.getLogger(name)));
        return logger;
    }

    @Override
    public void publish(@NotNull LogRecord record) {
        String message = formatter.formatMessage(record);
        Throwable thrown = record.getThrown();
        int level = record.getLevel().intValue();
        if (level >= Level.SEVERE.intValue()) {
            target.error(message, thrown);
        } else if (level >= Level.WARNING.intValue()) {
            target.warn(message, thrown);
        } else if (level >= Level.INFO.intValue()) {
            target.info(message, thrown);
        } else {
            target.debug(message, thrown);
        }
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }
}

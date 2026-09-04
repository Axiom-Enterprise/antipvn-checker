package com.axiom.antivpn.nukkit;

import cn.nukkit.plugin.PluginLogger;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

final class NukkitLogBridge extends Handler {

    private final @NotNull PluginLogger target;
    private final @NotNull SimpleFormatter formatter = new SimpleFormatter();

    private NukkitLogBridge(@NotNull PluginLogger target) {
        this.target = target;
        setLevel(Level.ALL);
    }

    static @NotNull Logger create(@NotNull PluginLogger target) {
        Logger logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.ALL);
        logger.addHandler(new NukkitLogBridge(target));
        return logger;
    }

    @Override
    public void publish(@NotNull LogRecord record) {
        String message = formatter.formatMessage(record);
        Throwable thrown = record.getThrown();
        int level = record.getLevel().intValue();
        if (level >= Level.SEVERE.intValue()) {
            if (thrown != null) target.error(message, thrown); else target.error(message);
        } else if (level >= Level.WARNING.intValue()) {
            if (thrown != null) target.warning(message, thrown); else target.warning(message);
        } else if (level >= Level.INFO.intValue()) {
            if (thrown != null) target.info(message, thrown); else target.info(message);
        } else {
            if (thrown != null) target.debug(message, thrown); else target.debug(message);
        }
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }
}

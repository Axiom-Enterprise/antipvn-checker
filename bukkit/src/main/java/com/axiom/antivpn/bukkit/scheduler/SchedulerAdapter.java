package com.axiom.antivpn.bukkit.scheduler;

import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

/** Thread model abstraction: classic Bukkit main thread vs Folia regions. Selected once at startup. */
public interface SchedulerAdapter {

    void runAsync(@NotNull Runnable task);

    /** Global/main-thread context: console commands, server-wide state. */
    void runGlobal(@NotNull Runnable task);

    /** Entity-owned context: kicks and anything touching the entity itself. */
    void runForEntity(@NotNull Entity entity, @NotNull Runnable task);

    static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}

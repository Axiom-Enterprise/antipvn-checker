package com.axiom.antivpn.bukkit.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public final class BukkitSchedulerAdapter implements SchedulerAdapter {

    private final @NotNull Plugin plugin;

    public BukkitSchedulerAdapter(@NotNull Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void runAsync(@NotNull Runnable task) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    @Override
    public void runGlobal(@NotNull Runnable task) {
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    @Override
    public void runForEntity(@NotNull Entity entity, @NotNull Runnable task) {
        runGlobal(task);
    }
}

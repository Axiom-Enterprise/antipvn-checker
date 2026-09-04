package com.axiom.antivpn.bukkit.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/** Only instantiated when Folia is detected, so Paper-only scheduler classes never load elsewhere. */
public final class FoliaSchedulerAdapter implements SchedulerAdapter {

    private final @NotNull Plugin plugin;

    public FoliaSchedulerAdapter(@NotNull Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void runAsync(@NotNull Runnable task) {
        Bukkit.getAsyncScheduler().runNow(plugin, scheduled -> task.run());
    }

    @Override
    public void runGlobal(@NotNull Runnable task) {
        if (Bukkit.isGlobalTickThread()) {
            task.run();
        } else {
            Bukkit.getGlobalRegionScheduler().run(plugin, scheduled -> task.run());
        }
    }

    @Override
    public void runForEntity(@NotNull Entity entity, @NotNull Runnable task) {
        if (Bukkit.isOwnedByCurrentRegion(entity)) {
            task.run();
        } else {
            entity.getScheduler().run(plugin, scheduled -> task.run(), null);
        }
    }
}

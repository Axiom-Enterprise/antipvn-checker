package com.axiom.antivpn.bukkit;

import com.axiom.antivpn.common.platform.Platform;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.logging.Logger;

public final class BukkitPlatform implements Platform {

    private final @NotNull JavaPlugin plugin;
    private final Executor asyncExecutor;

    public BukkitPlatform(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
        this.asyncExecutor = runnable -> Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
    }

    @Override public @NotNull String getPlatformName() { return "Bukkit"; }
    @Override public @NotNull Logger getPluginLogger() { return plugin.getLogger(); }
    @Override public @NotNull Path getDataFolder() { return plugin.getDataFolder().toPath(); }
    @Override public @NotNull Executor getAsyncExecutor() { return asyncExecutor; }

    @Override
    public void runAsync(@NotNull Runnable task) {
        if (runFoliaScheduler("getAsyncScheduler", "runNow", task)) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    @Override
    public void runSync(@NotNull Runnable task) {
        if (runFoliaScheduler("getGlobalRegionScheduler", "run", task)) return;
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    @Override
    public void kickPlayer(@NotNull UUID uuid, @NotNull String message) {
        runSync(() -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.kickPlayer(message);
            }
        });
    }

    @Override
    public void sendMessage(@NotNull UUID uuid, @NotNull String message) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            player.sendMessage(message);
        }
    }

    @Override
    public void broadcastPermission(@NotNull String permission, @NotNull String message) {
        Bukkit.broadcast(message, permission);
    }

    @Override
    public void dispatchConsoleCommand(@NotNull String command) {
        runSync(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
    }

    @Override
    public boolean isPlayerOnline(@NotNull UUID uuid) {
        return Bukkit.getPlayer(uuid) != null;
    }

    @Override
    public String getPlayerIp(@NotNull UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.getAddress() != null) {
            return player.getAddress().getAddress().getHostAddress();
        }
        return null;
    }

    @Override
    public boolean hasPermission(@NotNull UUID uuid, @NotNull String permission) {
        Player player = Bukkit.getPlayer(uuid);
        return player != null && player.hasPermission(permission);
    }

    public @NotNull JavaPlugin getPlugin() {
        return plugin;
    }

    private boolean runFoliaScheduler(@NotNull String getter, @NotNull String methodName, @NotNull Runnable task) {
        try {
            Object scheduler = Bukkit.class.getMethod(getter).invoke(null);
            for (var method : scheduler.getClass().getMethods()) {
                if (!method.getName().equals(methodName) || method.getParameterCount() != 2) continue;
                Consumer<Object> callback = ignored -> task.run();
                method.invoke(scheduler, plugin, callback);
                return true;
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
        return false;
    }
}

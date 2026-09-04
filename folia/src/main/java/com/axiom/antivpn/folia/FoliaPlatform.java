package com.axiom.antivpn.folia;

import com.axiom.antivpn.common.platform.Platform;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

public final class FoliaPlatform implements Platform {

    private final @NotNull JavaPlugin plugin;
    private final @NotNull AsyncScheduler async;
    private final @NotNull GlobalRegionScheduler global;

    public FoliaPlatform(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
        this.async = Bukkit.getAsyncScheduler();
        this.global = Bukkit.getGlobalRegionScheduler();
    }

    @Override public @NotNull String getPlatformName() { return "Folia"; }
    @Override public @NotNull Logger getPluginLogger() { return plugin.getLogger(); }
    @Override public @NotNull Path getDataFolder() { return plugin.getDataFolder().toPath(); }
    @Override public @NotNull Executor getAsyncExecutor() { return this::runAsync; }

    @Override
    public void runAsync(@NotNull Runnable task) {
        async.runNow(plugin, scheduled -> task.run());
    }

    @Override
    public void runSync(@NotNull Runnable task) {
        if (Bukkit.isGlobalTickThread()) {
            task.run();
        } else {
            global.run(plugin, scheduled -> task.run());
        }
    }

    public void runForEntity(@NotNull Entity entity, @NotNull Runnable task) {
        if (Bukkit.isOwnedByCurrentRegion(entity)) {
            task.run();
        } else {
            entity.getScheduler().run(plugin, scheduled -> task.run(), null);
        }
    }

    @Override
    public void kickPlayer(@NotNull UUID uuid, @NotNull String message) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;
        runForEntity(player, () -> {
            if (player.isOnline()) {
                player.kick(LegacyComponentSerializer.legacySection().deserialize(message));
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
        for (Player player : Bukkit.getOnlinePlayers()) {
            runForEntity(player, () -> {
                if (player.isOnline() && player.hasPermission(permission)) {
                    player.sendMessage(message);
                }
            });
        }
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
}

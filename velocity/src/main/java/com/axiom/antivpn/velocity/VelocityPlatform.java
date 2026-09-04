package com.axiom.antivpn.velocity;

import com.axiom.antivpn.common.color.ColorParser;
import com.axiom.antivpn.common.platform.Platform;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

public final class VelocityPlatform implements Platform {

    private final @NotNull Object plugin;
    private final @NotNull ProxyServer server;
    private final @NotNull Logger logger;
    private final @NotNull Path dataFolder;
    private final @NotNull Executor asyncExecutor;

    public VelocityPlatform(@NotNull Object plugin, @NotNull ProxyServer server, @NotNull Logger logger, @NotNull Path dataFolder) {
        this.plugin = plugin;
        this.server = server;
        this.logger = logger;
        this.dataFolder = dataFolder;
        this.asyncExecutor = task -> server.getScheduler().buildTask(plugin, task).schedule();
    }

    @Override public @NotNull String getPlatformName() { return "Velocity"; }
    @Override public @NotNull Logger getPluginLogger() { return logger; }
    @Override public @NotNull Path getDataFolder() { return dataFolder; }
    @Override public @NotNull Executor getAsyncExecutor() { return asyncExecutor; }

    @Override
    public void runAsync(@NotNull Runnable task) {
        asyncExecutor.execute(task);
    }

    @Override
    public void runSync(@NotNull Runnable task) {
        task.run();
    }

    @Override
    public void kickPlayer(@NotNull UUID uuid, @NotNull String message) {
        Player player = server.getPlayer(uuid).orElse(null);
        if (player != null) {
            player.disconnect(ColorParser.toComponent(message));
        }
    }

    @Override
    public void sendMessage(@NotNull UUID uuid, @NotNull String message) {
        Player player = server.getPlayer(uuid).orElse(null);
        if (player != null) {
            player.sendMessage(ColorParser.toComponent(message));
        }
    }

    @Override
    public void broadcastPermission(@NotNull String permission, @NotNull String message) {
        Component component = ColorParser.toComponent(message);
        for (Player player : server.getAllPlayers()) {
            if (player.hasPermission(permission)) {
                player.sendMessage(component);
            }
        }
    }

    @Override
    public void dispatchConsoleCommand(@NotNull String command) {
        server.getCommandManager().executeAsync(server.getConsoleCommandSource(), command);
    }

    @Override
    public boolean isPlayerOnline(@NotNull UUID uuid) {
        return server.getPlayer(uuid).isPresent();
    }

    @Override
    public String getPlayerIp(@NotNull UUID uuid) {
        Player player = server.getPlayer(uuid).orElse(null);
        return player == null ? null : player.getRemoteAddress().getAddress().getHostAddress();
    }

    @Override
    public boolean hasPermission(@NotNull UUID uuid, @NotNull String permission) {
        Player player = server.getPlayer(uuid).orElse(null);
        return player != null && player.hasPermission(permission);
    }

    public @NotNull ProxyServer getServer() {
        return server;
    }

    public @NotNull Object getPlugin() {
        return plugin;
    }
}

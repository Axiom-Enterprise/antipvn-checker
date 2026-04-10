package com.axiom.antivpn.velocity;

import com.axiom.antivpn.common.platform.Platform;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

public final class VelocityPlatform implements Platform {

    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.builder()
            .character('§')
            .hexColors()
            .build();

    private final @NotNull Object plugin;
    private final @NotNull ProxyServer server;
    private final @NotNull Logger logger;
    private final @NotNull Path dataFolder;

    public VelocityPlatform(@NotNull Object plugin, @NotNull ProxyServer server, @NotNull Logger logger, @NotNull Path dataFolder) {
        this.plugin = plugin;
        this.server = server;
        this.logger = logger;
        this.dataFolder = dataFolder;
    }

    @Override public @NotNull String getPlatformName() { return "Velocity"; }
    @Override public @NotNull Logger getPluginLogger() { return logger; }
    @Override public @NotNull Path getDataFolder() { return dataFolder; }

    @Override
    public @NotNull Executor getAsyncExecutor() {
        return runnable -> server.getScheduler().buildTask(plugin, runnable).schedule();
    }

    @Override
    public void runAsync(@NotNull Runnable task) {
        server.getScheduler().buildTask(plugin, task).schedule();
    }

    @Override
    public void runSync(@NotNull Runnable task) {
        task.run();
    }

    @Override
    public void kickPlayer(@NotNull UUID uuid, @NotNull String message) {
        server.getPlayer(uuid).ifPresent(player ->
                player.disconnect(SERIALIZER.deserialize(message)));
    }

    @Override
    public void sendMessage(@NotNull UUID uuid, @NotNull String message) {
        server.getPlayer(uuid).ifPresent(player ->
                player.sendMessage(SERIALIZER.deserialize(message)));
    }

    @Override
    public void broadcastPermission(@NotNull String permission, @NotNull String message) {
        var component = SERIALIZER.deserialize(message);
        for (Player player : server.getAllPlayers()) {
            if (player.hasPermission(permission)) {
                player.sendMessage(component);
            }
        }
    }

    @Override
    public boolean isPlayerOnline(@NotNull UUID uuid) {
        return server.getPlayer(uuid).isPresent();
    }

    @Override
    public String getPlayerIp(@NotNull UUID uuid) {
        return server.getPlayer(uuid)
                .map(p -> p.getRemoteAddress().getAddress().getHostAddress())
                .orElse(null);
    }

    @Override
    public boolean hasPermission(@NotNull UUID uuid, @NotNull String permission) {
        return server.getPlayer(uuid)
                .map(p -> p.hasPermission(permission))
                .orElse(false);
    }

    public @NotNull ProxyServer getServer() {
        return server;
    }
}

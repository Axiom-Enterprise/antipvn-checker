package com.axiom.antivpn.minestom;

import com.axiom.antivpn.common.color.ColorParser;
import com.axiom.antivpn.common.platform.Platform;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

final class MinestomPlatform implements Platform {

    private final @NotNull Path dataFolder;
    private final @NotNull PermissionChecker permissions;
    private final @NotNull Logger logger = Slf4jLogHandler.create("AxiomAntiVPN");
    private final @NotNull ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    MinestomPlatform(@NotNull Path dataFolder, @NotNull PermissionChecker permissions) {
        this.dataFolder = dataFolder;
        this.permissions = permissions;
    }

    @Override public @NotNull String getPlatformName() { return "Minestom"; }
    @Override public @NotNull Logger getPluginLogger() { return logger; }
    @Override public @NotNull Path getDataFolder() { return dataFolder; }
    @Override public @NotNull Executor getAsyncExecutor() { return executor; }

    @Override
    public void runAsync(@NotNull Runnable task) {
        executor.execute(task);
    }

    @Override
    public void runSync(@NotNull Runnable task) {
        MinecraftServer.getSchedulerManager().scheduleNextTick(task);
    }

    @Override
    public void kickPlayer(@NotNull UUID uuid, @NotNull String message) {
        Player player = player(uuid);
        if (player == null) return;
        Component component = ColorParser.toComponent(message);
        runSync(() -> player.kick(component));
    }

    @Override
    public void sendMessage(@NotNull UUID uuid, @NotNull String message) {
        Player player = player(uuid);
        if (player != null) {
            player.sendMessage(ColorParser.toComponent(message));
        }
    }

    @Override
    public void broadcastPermission(@NotNull String permission, @NotNull String message) {
        Component component = ColorParser.toComponent(message);
        for (Player player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            if (permissions.hasPermission(player, permission)) {
                player.sendMessage(component);
            }
        }
    }

    @Override
    public void dispatchConsoleCommand(@NotNull String command) {
        runSync(() -> MinecraftServer.getCommandManager().executeServerCommand(command));
    }

    @Override
    public boolean isPlayerOnline(@NotNull UUID uuid) {
        return player(uuid) != null;
    }

    @Override
    public String getPlayerIp(@NotNull UUID uuid) {
        Player player = player(uuid);
        if (player == null) return null;
        SocketAddress address = player.getPlayerConnection().getRemoteAddress();
        return address instanceof InetSocketAddress inet && inet.getAddress() != null ? inet.getAddress().getHostAddress() : null;
    }

    @Override
    public boolean hasPermission(@NotNull UUID uuid, @NotNull String permission) {
        Player player = player(uuid);
        return player != null && permissions.hasPermission(player, permission);
    }

    void close() {
        executor.shutdown();
    }

    private static Player player(@NotNull UUID uuid) {
        return MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(uuid);
    }
}

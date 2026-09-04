package com.axiom.antivpn.nukkit;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.plugin.PluginBase;
import com.axiom.antivpn.common.platform.Platform;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

final class NukkitPlatform implements Platform {

    private final @NotNull PluginBase plugin;
    private final @NotNull Server server;
    private final @NotNull Logger logger;
    private final @NotNull Executor asyncExecutor;

    NukkitPlatform(@NotNull PluginBase plugin) {
        this.plugin = plugin;
        this.server = plugin.getServer();
        this.logger = NukkitLogBridge.create(plugin.getLogger());
        this.asyncExecutor = task -> server.getScheduler().scheduleTask(plugin, task, true);
    }

    @Override public @NotNull String getPlatformName() { return "NukkitX"; }
    @Override public @NotNull Logger getPluginLogger() { return logger; }
    @Override public @NotNull Path getDataFolder() { return plugin.getDataFolder().toPath(); }
    @Override public @NotNull Executor getAsyncExecutor() { return asyncExecutor; }

    @Override
    public void runAsync(@NotNull Runnable task) {
        asyncExecutor.execute(task);
    }

    @Override
    public void runSync(@NotNull Runnable task) {
        if (server.isPrimaryThread()) {
            task.run();
        } else {
            server.getScheduler().scheduleTask(plugin, task);
        }
    }

    @Override
    public void kickPlayer(@NotNull UUID uuid, @NotNull String message) {
        String rendered = BedrockText.render(message);
        runSync(() -> {
            Player player = server.getPlayer(uuid).orElse(null);
            if (player != null) {
                player.kick(rendered, false);
            }
        });
    }

    @Override
    public void sendMessage(@NotNull UUID uuid, @NotNull String message) {
        String rendered = BedrockText.render(message);
        runSync(() -> {
            Player player = server.getPlayer(uuid).orElse(null);
            if (player != null) {
                player.sendMessage(rendered);
            }
        });
    }

    @Override
    public void broadcastPermission(@NotNull String permission, @NotNull String message) {
        String rendered = BedrockText.render(message);
        runSync(() -> server.broadcast(rendered, permission));
    }

    @Override
    public void dispatchConsoleCommand(@NotNull String command) {
        runSync(() -> server.dispatchCommand(server.getConsoleSender(), command));
    }

    @Override
    public boolean isPlayerOnline(@NotNull UUID uuid) {
        return server.getPlayer(uuid).isPresent();
    }

    @Override
    public String getPlayerIp(@NotNull UUID uuid) {
        Player player = server.getPlayer(uuid).orElse(null);
        return player == null ? null : player.getAddress();
    }

    @Override
    public boolean hasPermission(@NotNull UUID uuid, @NotNull String permission) {
        Player player = server.getPlayer(uuid).orElse(null);
        return player != null && player.hasPermission(permission);
    }
}

package com.axiom.antivpn.bungee;

import com.axiom.antivpn.common.platform.Platform;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public final class BungeePlatform implements Platform {

    private final @NotNull Plugin plugin;
    private final Executor asyncExecutor;

    public BungeePlatform(@NotNull Plugin plugin) {
        this.plugin = plugin;
        this.asyncExecutor = runnable -> ProxyServer.getInstance().getScheduler().runAsync(plugin, runnable);
    }

    @Override public @NotNull String getPlatformName() { return "BungeeCord"; }
    @Override public @NotNull Logger getPluginLogger() { return plugin.getLogger(); }
    @Override public @NotNull Path getDataFolder() { return plugin.getDataFolder().toPath(); }
    @Override public @NotNull Executor getAsyncExecutor() { return asyncExecutor; }

    @Override
    public void runAsync(@NotNull Runnable task) {
        ProxyServer.getInstance().getScheduler().runAsync(plugin, task);
    }

    @Override
    public void runSync(@NotNull Runnable task) {
        task.run();
    }

    @Override
    public void kickPlayer(@NotNull UUID uuid, @NotNull String message) {
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(uuid);
        if (player != null) {
            player.disconnect(TextComponent.fromLegacy(message));
        }
    }

    @Override
    public void sendMessage(@NotNull UUID uuid, @NotNull String message) {
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(uuid);
        if (player != null) {
            player.sendMessage(TextComponent.fromLegacy(message));
        }
    }

    @Override
    public void broadcastPermission(@NotNull String permission, @NotNull String message) {
        for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
            if (player.hasPermission(permission)) {
                player.sendMessage(TextComponent.fromLegacy(message));
            }
        }
    }

    @Override
    public boolean isPlayerOnline(@NotNull UUID uuid) {
        return ProxyServer.getInstance().getPlayer(uuid) != null;
    }

    @Override
    public String getPlayerIp(@NotNull UUID uuid) {
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(uuid);
        if (player != null && player.getAddress() != null) {
            return player.getAddress().getAddress().getHostAddress();
        }
        return null;
    }

    @Override
    public boolean hasPermission(@NotNull UUID uuid, @NotNull String permission) {
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(uuid);
        return player != null && player.hasPermission(permission);
    }

    public @NotNull Plugin getPlugin() {
        return plugin;
    }
}

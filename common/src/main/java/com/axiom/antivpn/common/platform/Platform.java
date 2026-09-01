package com.axiom.antivpn.common.platform;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

public interface Platform {

    @NotNull String getPlatformName();

    @NotNull Logger getPluginLogger();

    @NotNull Path getDataFolder();

    @NotNull Executor getAsyncExecutor();

    void runAsync(@NotNull Runnable task);

    void runSync(@NotNull Runnable task);

    void kickPlayer(@NotNull UUID uuid, @NotNull String message);

    void sendMessage(@NotNull UUID uuid, @NotNull String message);

    void broadcastPermission(@NotNull String permission, @NotNull String message);

    void dispatchConsoleCommand(@NotNull String command);

    boolean isPlayerOnline(@NotNull UUID uuid);

    String getPlayerIp(@NotNull UUID uuid);

    boolean hasPermission(@NotNull UUID uuid, @NotNull String permission);
}

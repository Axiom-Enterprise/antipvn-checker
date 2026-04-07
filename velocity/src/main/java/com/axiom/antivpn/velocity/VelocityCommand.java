package com.axiom.antivpn.velocity;

import com.axiom.antivpn.common.AntiVpnEngine;
import com.axiom.antivpn.common.config.Messages;
import com.axiom.antivpn.common.util.IpUtil;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class VelocityCommand implements SimpleCommand {

    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.builder()
            .character('\u00A7')
            .hexColors()
            .build();

    private final @NotNull AntiVpnEngine engine;

    public VelocityCommand(@NotNull AntiVpnEngine engine) {
        this.engine = engine;
    }

    @Override
    public void execute(@NotNull Invocation invocation) {
        var source = invocation.source();
        String[] args = invocation.arguments();
        Messages messages = engine.getMessages();

        if (!source.hasPermission("antivpn.admin")) {
            source.sendMessage(SERIALIZER.deserialize(messages.format(messages.getNoPermission())));
            return;
        }

        if (args.length == 0) {
            send(invocation, messages.format(messages.getUsageHelp()));
            return;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "check" -> handleCheck(invocation, args, messages);
            case "whitelist" -> handleWhitelist(invocation, args, messages);
            case "status" -> handleStatus(invocation, messages);
            case "cache" -> handleCache(invocation, args, messages);
            case "reload" -> handleReload(invocation, messages);
            default -> send(invocation, messages.format(messages.getUsageHelp()));
        }
    }

    private void send(@NotNull Invocation invocation, @NotNull String message) {
        invocation.source().sendMessage(SERIALIZER.deserialize(message));
    }

    private void handleCheck(@NotNull Invocation invocation, @NotNull String[] args, @NotNull Messages messages) {
        if (args.length < 2) {
            send(invocation, messages.format(messages.getUsageHelp()));
            return;
        }

        String ip = args[1];
        if (!IpUtil.isValidIp(ip)) {
            send(invocation, messages.format(messages.getInvalidIp()));
            return;
        }

        send(invocation, messages.format("{prefix}&7Checking IP &f" + ip + "&7..."));
        engine.checkIp(ip).thenAccept(response ->
                send(invocation, messages.format(messages.getCheckResult(), response))
        ).exceptionally(ex -> {
            send(invocation, messages.format("{prefix}&cFailed to check IP: " + ex.getMessage()));
            return null;
        });
    }

    private void handleWhitelist(@NotNull Invocation invocation, @NotNull String[] args, @NotNull Messages messages) {
        if (args.length < 3) {
            send(invocation, messages.format(messages.getUsageHelp()));
            return;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        String target = args[2];
        ProxyServer server = ((VelocityPlatform) engine.getPlatform()).getServer();

        switch (action) {
            case "add" -> {
                if (IpUtil.isValidIp(target)) {
                    if (engine.getWhitelist().addIp(target)) {
                        send(invocation, messages.formatWith(messages.getWhitelistAdd(), "{target}", target));
                    } else {
                        send(invocation, messages.formatWith(messages.getWhitelistAlready(), "{target}", target));
                    }
                } else {
                    var player = server.getPlayer(target);
                    if (player.isPresent()) {
                        if (engine.getWhitelist().addPlayer(player.get().getUniqueId())) {
                            send(invocation, messages.formatWith(messages.getWhitelistAdd(), "{target}", target));
                        } else {
                            send(invocation, messages.formatWith(messages.getWhitelistAlready(), "{target}", target));
                        }
                    } else {
                        send(invocation, messages.format(messages.getPlayerNotFound()));
                    }
                }
            }
            case "remove" -> {
                if (IpUtil.isValidIp(target)) {
                    if (engine.getWhitelist().removeIp(target)) {
                        send(invocation, messages.formatWith(messages.getWhitelistRemove(), "{target}", target));
                    } else {
                        send(invocation, messages.formatWith(messages.getWhitelistNotFound(), "{target}", target));
                    }
                } else {
                    var player = server.getPlayer(target);
                    if (player.isPresent()) {
                        if (engine.getWhitelist().removePlayer(player.get().getUniqueId())) {
                            send(invocation, messages.formatWith(messages.getWhitelistRemove(), "{target}", target));
                        } else {
                            send(invocation, messages.formatWith(messages.getWhitelistNotFound(), "{target}", target));
                        }
                    } else {
                        send(invocation, messages.format(messages.getPlayerNotFound()));
                    }
                }
            }
            default -> send(invocation, messages.format(messages.getUsageHelp()));
        }
    }

    private void handleStatus(@NotNull Invocation invocation, @NotNull Messages messages) {
        engine.getStatus().thenAccept(status -> {
            if (status.isOperational()) {
                double latency = 0;
                var apiService = status.services().get("api");
                if (apiService != null) {
                    latency = apiService.responseTimeMs();
                }
                send(invocation, messages.format(messages.getStatusOnline())
                        .replace("{latency}", String.format("%.1f", latency)));
            } else {
                send(invocation, messages.format(messages.getStatusOffline()));
            }
        }).exceptionally(ex -> {
            send(invocation, messages.format(messages.getStatusOffline()));
            return null;
        });
    }

    private void handleCache(@NotNull Invocation invocation, @NotNull String[] args, @NotNull Messages messages) {
        if (args.length < 2 || !"clear".equalsIgnoreCase(args[1])) {
            send(invocation, messages.format(messages.getUsageHelp()));
            return;
        }

        long size = engine.getCache().size();
        engine.clearCache();
        send(invocation, messages.formatWith(messages.getCacheCleared(), "{size}", String.valueOf(size)));
    }

    private void handleReload(@NotNull Invocation invocation, @NotNull Messages messages) {
        VelocityPlatform platform = (VelocityPlatform) engine.getPlatform();
        VelocityConfig mainConfig = new VelocityConfig(platform.getDataFolder().resolve("config.yml"), platform.getPluginLogger());
        VelocityConfig messagesConfig = new VelocityConfig(platform.getDataFolder().resolve("messages.yml"), platform.getPluginLogger());
        engine.reload(mainConfig, messagesConfig);
        send(invocation, messages.format(messages.getReloadSuccess()));
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(@NotNull Invocation invocation) {
        String[] args = invocation.arguments();
        if (!invocation.source().hasPermission("antivpn.admin")) {
            return CompletableFuture.completedFuture(List.of());
        }

        List<String> completions = new ArrayList<>();
        if (args.length <= 1) {
            completions.add("check");
            completions.add("whitelist");
            completions.add("status");
            completions.add("cache");
            completions.add("reload");
        } else if (args.length == 2) {
            if ("whitelist".equalsIgnoreCase(args[0])) {
                completions.add("add");
                completions.add("remove");
            } else if ("cache".equalsIgnoreCase(args[0])) {
                completions.add("clear");
            }
        } else if (args.length == 3 && "whitelist".equalsIgnoreCase(args[0])) {
            ProxyServer server = ((VelocityPlatform) engine.getPlatform()).getServer();
            for (Player p : server.getAllPlayers()) {
                completions.add(p.getUsername());
            }
        }

        String lastArg = args.length > 0 ? args[args.length - 1].toLowerCase(Locale.ROOT) : "";
        completions.removeIf(s -> !s.toLowerCase(Locale.ROOT).startsWith(lastArg));
        return CompletableFuture.completedFuture(completions);
    }

    @Override
    public boolean hasPermission(@NotNull Invocation invocation) {
        return invocation.source().hasPermission("antivpn.admin");
    }
}

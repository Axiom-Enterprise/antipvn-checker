package com.axiom.antivpn.bungee;

import com.axiom.antivpn.common.AntiVpnEngine;
import com.axiom.antivpn.common.config.Messages;
import com.axiom.antivpn.common.util.IpUtil;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class BungeeCommand extends Command implements TabExecutor {

    private final @NotNull AntiVpnEngine engine;

    public BungeeCommand(@NotNull AntiVpnEngine engine) {
        super("antivpn", "antivpn.admin", "avpn", "axiomvpn");
        this.engine = engine;
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        Messages messages = engine.getMessages();

        if (!sender.hasPermission("antivpn.admin")) {
            sender.sendMessage(TextComponent.fromLegacy(messages.format(messages.getNoPermission())));
            return;
        }

        if (args.length == 0) {
            sender.sendMessage(TextComponent.fromLegacy(messages.format(messages.getUsageHelp())));
            return;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "check" -> handleCheck(sender, args, messages);
            case "whitelist" -> handleWhitelist(sender, args, messages);
            case "status" -> handleStatus(sender, messages);
            case "cache" -> handleCache(sender, args, messages);
            case "reload" -> handleReload(sender, messages);
            default -> sender.sendMessage(TextComponent.fromLegacy(messages.format(messages.getUsageHelp())));
        }
    }

    private void send(@NotNull CommandSender sender, @NotNull String message) {
        sender.sendMessage(TextComponent.fromLegacy(message));
    }

    private void handleCheck(@NotNull CommandSender sender, @NotNull String[] args, @NotNull Messages messages) {
        if (args.length < 2) {
            send(sender, messages.format(messages.getUsageHelp()));
            return;
        }
        String ip = args[1];
        if (!IpUtil.isValidIp(ip)) {
            send(sender, messages.format(messages.getInvalidIp()));
            return;
        }

        send(sender, messages.format("{prefix}&7Checking IP &f" + ip + "&7..."));
        engine.checkIp(ip).thenAccept(response -> {
            send(sender, messages.format(messages.getCheckResult(), response));
        }).exceptionally(ex -> {
            send(sender, messages.format("{prefix}&cFailed to check IP: " + ex.getMessage()));
            return null;
        });
    }

    private void handleWhitelist(@NotNull CommandSender sender, @NotNull String[] args, @NotNull Messages messages) {
        if (args.length < 3) {
            send(sender, messages.format(messages.getUsageHelp()));
            return;
        }

        String action = args[1].toLowerCase(Locale.ROOT);
        String target = args[2];

        switch (action) {
            case "add" -> {
                if (IpUtil.isValidIp(target)) {
                    if (engine.getWhitelist().addIp(target)) {
                        send(sender, messages.formatWith(messages.getWhitelistAdd(), "{target}", target));
                    } else {
                        send(sender, messages.formatWith(messages.getWhitelistAlready(), "{target}", target));
                    }
                } else {
                    ProxiedPlayer player = ProxyServer.getInstance().getPlayer(target);
                    if (player != null) {
                        if (engine.getWhitelist().addPlayer(player.getUniqueId())) {
                            send(sender, messages.formatWith(messages.getWhitelistAdd(), "{target}", target));
                        } else {
                            send(sender, messages.formatWith(messages.getWhitelistAlready(), "{target}", target));
                        }
                    } else {
                        send(sender, messages.format(messages.getPlayerNotFound()));
                    }
                }
            }
            case "remove" -> {
                if (IpUtil.isValidIp(target)) {
                    if (engine.getWhitelist().removeIp(target)) {
                        send(sender, messages.formatWith(messages.getWhitelistRemove(), "{target}", target));
                    } else {
                        send(sender, messages.formatWith(messages.getWhitelistNotFound(), "{target}", target));
                    }
                } else {
                    ProxiedPlayer player = ProxyServer.getInstance().getPlayer(target);
                    if (player != null) {
                        if (engine.getWhitelist().removePlayer(player.getUniqueId())) {
                            send(sender, messages.formatWith(messages.getWhitelistRemove(), "{target}", target));
                        } else {
                            send(sender, messages.formatWith(messages.getWhitelistNotFound(), "{target}", target));
                        }
                    } else {
                        send(sender, messages.format(messages.getPlayerNotFound()));
                    }
                }
            }
            default -> send(sender, messages.format(messages.getUsageHelp()));
        }
    }

    private void handleStatus(@NotNull CommandSender sender, @NotNull Messages messages) {
        engine.getStatus().thenAccept(status -> {
            if (status.isOperational()) {
                double latency = 0;
                var apiService = status.services().get("api");
                if (apiService != null) {
                    latency = apiService.responseTimeMs();
                }
                send(sender, messages.format(messages.getStatusOnline())
                        .replace("{latency}", String.format("%.1f", latency)));
            } else {
                send(sender, messages.format(messages.getStatusOffline()));
            }
        }).exceptionally(ex -> {
            send(sender, messages.format(messages.getStatusOffline()));
            return null;
        });
    }

    private void handleCache(@NotNull CommandSender sender, @NotNull String[] args, @NotNull Messages messages) {
        if (args.length < 2 || !"clear".equalsIgnoreCase(args[1])) {
            send(sender, messages.format(messages.getUsageHelp()));
            return;
        }
        long size = engine.getCache().size();
        engine.clearCache();
        send(sender, messages.formatWith(messages.getCacheCleared(), "{size}", String.valueOf(size)));
    }

    private void handleReload(@NotNull CommandSender sender, @NotNull Messages messages) {
        var plugin = ((BungeePlatform) engine.getPlatform()).getPlugin();
        BungeeConfig mainConfig = new BungeeConfig(new File(plugin.getDataFolder(), "config.yml"), plugin.getLogger());
        BungeeConfig messagesConfig = new BungeeConfig(new File(plugin.getDataFolder(), "messages.yml"), plugin.getLogger());
        engine.reload(mainConfig, messagesConfig);
        send(sender, messages.format(messages.getReloadSuccess()));
    }

    @Override
    public @NotNull Iterable<String> onTabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!sender.hasPermission("antivpn.admin")) return List.of();

        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
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
            for (ProxiedPlayer p : ProxyServer.getInstance().getPlayers()) {
                completions.add(p.getName());
            }
        }

        String lastArg = args[args.length - 1].toLowerCase(Locale.ROOT);
        completions.removeIf(s -> !s.toLowerCase(Locale.ROOT).startsWith(lastArg));
        return completions;
    }
}

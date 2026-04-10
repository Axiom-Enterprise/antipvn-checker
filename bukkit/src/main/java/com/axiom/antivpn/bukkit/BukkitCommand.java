package com.axiom.antivpn.bukkit;

import com.axiom.antivpn.common.AntiVpnEngine;
import com.axiom.antivpn.common.config.Messages;
import com.axiom.antivpn.common.util.IpUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class BukkitCommand implements CommandExecutor, TabCompleter {

    private final @NotNull AntiVpnEngine engine;

    public BukkitCommand(@NotNull AntiVpnEngine engine) {
        this.engine = engine;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Messages messages = engine.getMessages();

        if (!sender.hasPermission("antivpn.admin")) {
            sender.sendMessage(messages.format(messages.getNoPermission()));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(messages.format(messages.getUsageHelp()));
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "check" -> handleCheck(sender, args, messages);
            case "whitelist" -> handleWhitelist(sender, args, messages);
            case "status" -> handleStatus(sender, messages);
            case "cache" -> handleCache(sender, args, messages);
            case "reload" -> handleReload(sender, messages);
            default -> sender.sendMessage(messages.format(messages.getUsageHelp()));
        }
        return true;
    }

    private void handleCheck(@NotNull CommandSender sender, @NotNull String[] args, @NotNull Messages messages) {
        if (args.length < 2) {
            sender.sendMessage(messages.format(messages.getUsageHelp()));
            return;
        }

        String ip = args[1];
        if (!IpUtil.isValidIp(ip)) {
            sender.sendMessage(messages.format(messages.getInvalidIp()));
            return;
        }

        sender.sendMessage(messages.format(messages.format("{prefix}&7Checking IP &f" + ip + "&7...")));

        engine.checkIp(ip).thenAccept(response -> sender.sendMessage(messages.format(messages.getCheckResult(), response))
        ).exceptionally(ex -> {
            sender.sendMessage(messages.format("{prefix}&cFailed to check IP: " + ex.getMessage()));
            return null;
        });
    }

    private void handleWhitelist(@NotNull CommandSender sender, @NotNull String[] args, @NotNull Messages messages) {
        if (args.length < 3) {
            sender.sendMessage(messages.format(messages.getUsageHelp()));
            return;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        String target = args[2];

        switch (action) {
            case "add" -> {
                if (IpUtil.isValidIp(target)) {
                    if (engine.getWhitelist().addIp(target)) {
                        sender.sendMessage(messages.formatWith(messages.getWhitelistAdd(), "{target}", target));
                    } else {
                        sender.sendMessage(messages.formatWith(messages.getWhitelistAlready(), "{target}", target));
                    }
                } else {
                    Player player = Bukkit.getPlayerExact(target);
                    if (player != null) {
                        if (engine.getWhitelist().addPlayer(player.getUniqueId())) {
                            sender.sendMessage(messages.formatWith(messages.getWhitelistAdd(), "{target}", target));
                        } else {
                            sender.sendMessage(messages.formatWith(messages.getWhitelistAlready(), "{target}", target));
                        }
                    } else {
                        sender.sendMessage(messages.format(messages.getPlayerNotFound()));
                    }
                }
            }
            case "remove" -> {
                if (IpUtil.isValidIp(target)) {
                    if (engine.getWhitelist().removeIp(target)) {
                        sender.sendMessage(messages.formatWith(messages.getWhitelistRemove(), "{target}", target));
                    } else {
                        sender.sendMessage(messages.formatWith(messages.getWhitelistNotFound(), "{target}", target));
                    }
                } else {
                    Player player = Bukkit.getPlayerExact(target);
                    if (player != null) {
                        if (engine.getWhitelist().removePlayer(player.getUniqueId())) {
                            sender.sendMessage(messages.formatWith(messages.getWhitelistRemove(), "{target}", target));
                        } else {
                            sender.sendMessage(messages.formatWith(messages.getWhitelistNotFound(), "{target}", target));
                        }
                    } else {
                        sender.sendMessage(messages.format(messages.getPlayerNotFound()));
                    }
                }
            }
            default -> sender.sendMessage(messages.format(messages.getUsageHelp()));
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
                sender.sendMessage(messages.format(messages.getStatusOnline()).replace("{latency}", String.format("%.1f", latency)));
            } else {
                sender.sendMessage(messages.format(messages.getStatusOffline()));
            }
        }).exceptionally(ex -> {
            sender.sendMessage(messages.format(messages.getStatusOffline()));
            return null;
        });
    }

    private void handleCache(@NotNull CommandSender sender, @NotNull String[] args, @NotNull Messages messages) {
        if (args.length < 2 || !"clear".equalsIgnoreCase(args[1])) {
            sender.sendMessage(messages.format(messages.getUsageHelp()));
            return;
        }
        long size = engine.getCache().size();
        engine.clearCache();
        sender.sendMessage(messages.formatWith(messages.getCacheCleared(), "{size}", String.valueOf(size)));
    }

    private void handleReload(@NotNull CommandSender sender, @NotNull Messages messages) {
        JavaPlugin plugin = ((BukkitPlatform) engine.getPlatform()).getPlugin();
        BukkitConfig mainConfig = new BukkitConfig(new File(plugin.getDataFolder(), "config.yml"), plugin.getLogger());
        BukkitConfig messagesConfig = new BukkitConfig(new File(plugin.getDataFolder(), "messages.yml"), plugin.getLogger());
        engine.reload(mainConfig, messagesConfig);
        sender.sendMessage(messages.format(messages.getReloadSuccess()));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
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
            for (Player p : Bukkit.getOnlinePlayers()) {
                completions.add(p.getName());
            }
        }

        String lastArg = args[args.length - 1].toLowerCase(Locale.ROOT);
        completions.removeIf(s -> !s.toLowerCase(Locale.ROOT).startsWith(lastArg));
        return completions;
    }
}

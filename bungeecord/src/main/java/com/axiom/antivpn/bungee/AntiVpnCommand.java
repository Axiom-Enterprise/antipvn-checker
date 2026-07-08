package com.axiom.antivpn.bungee;

import com.axiom.antivpn.common.AntiVpnEngine;
import com.axiom.antivpn.common.command.OnlinePlayerNames;
import com.axiom.antivpn.common.config.Messages;
import com.axiom.antivpn.common.util.IpUtil;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bungee.actor.BungeeCommandActor;
import revxrsal.commands.bungee.annotation.CommandPermission;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Command("vpn")
@CommandPermission("antivpn.admin")
public final class AntiVpnCommand {

    private final @NotNull AntiVpnEngine engine;

    public AntiVpnCommand(@NotNull AntiVpnEngine engine) {
        this.engine = engine;
    }

    @Command("vpn")
    public void usage(@NotNull BungeeCommandActor actor) {
        Messages messages = engine.getMessages();
        actor.reply(messages.format(messages.getUsageHelp()));
    }

    @Subcommand("check")
    public void check(@NotNull BungeeCommandActor actor, @NotNull String ip) {
        Messages messages = engine.getMessages();
        if (!IpUtil.isValidIp(ip)) {
            actor.reply(messages.format(messages.getInvalidIp()));
            return;
        }

        actor.reply(messages.format("{prefix}&7Checking IP &f" + ip + "&7..."));
        engine.checkIp(ip).thenAccept(response ->
                actor.reply(messages.format(messages.getCheckResult(), response))
        ).exceptionally(ex -> {
            actor.reply(messages.format("{prefix}&cFailed to check IP: " + ex.getMessage()));
            return null;
        });
    }

    @Subcommand("status")
    public void status(@NotNull BungeeCommandActor actor) {
        Messages messages = engine.getMessages();
        engine.getStatus().thenAccept(status -> {
            if (status.isOperational()) {
                double latency = 0;
                var apiService = status.services().get("api");
                if (apiService != null) {
                    latency = apiService.responseTimeMs();
                }
                actor.reply(messages.format(messages.getStatusOnline()).replace("{latency}", String.format("%.1f", latency)));
            } else {
                actor.reply(messages.format(messages.getStatusOffline()));
            }
        }).exceptionally(ex -> {
            actor.reply(messages.format(messages.getStatusOffline()));
            return null;
        });
    }

    @Subcommand("cache clear")
    public void cacheClear(@NotNull BungeeCommandActor actor) {
        Messages messages = engine.getMessages();
        long size = engine.getCache().size();
        engine.clearCache();
        actor.reply(messages.formatWith(messages.getCacheCleared(), "{size}", String.valueOf(size)));
    }

    @Subcommand("reload")
    public void reload(@NotNull BungeeCommandActor actor) {
        Messages messages = engine.getMessages();
        Plugin plugin = ((BungeePlatform) engine.getPlatform()).getPlugin();
        BungeeConfig mainConfig = new BungeeConfig(new File(plugin.getDataFolder(), "config.yml"), plugin.getLogger());
        BungeeConfig messagesConfig = new BungeeConfig(new File(plugin.getDataFolder(), "messages.yml"), plugin.getLogger());
        engine.reload(mainConfig, messagesConfig);
        actor.reply(messages.format(messages.getReloadSuccess()));
    }

    @Subcommand("whitelist add")
    public void whitelistAdd(@NotNull BungeeCommandActor actor, @OnlinePlayerNames @NotNull String target) {
        whitelistApply(actor, "add", target);
    }

    @Subcommand("whitelist remove")
    public void whitelistRemove(@NotNull BungeeCommandActor actor, @OnlinePlayerNames @NotNull String target) {
        whitelistApply(actor, "remove", target);
    }

    @Subcommand("whitelist list")
    public void whitelistList(@NotNull BungeeCommandActor actor) {
        Messages messages = engine.getMessages();
        Set<String> ips = engine.getWhitelist().getWhitelistedIps();
        Map<UUID, String> players = engine.getWhitelist().getWhitelistedPlayers();

        if (ips.isEmpty() && players.isEmpty()) {
            actor.reply(messages.format(messages.getWhitelistListEmpty()));
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(messages.getWhitelistListIpsHeader().replace("{count}", String.valueOf(ips.size())));
        for (String ip : ips) {
            sb.append('\n').append(messages.getWhitelistListIpEntry().replace("{ip}", ip));
        }
        sb.append('\n').append(messages.getWhitelistListPlayersHeader().replace("{count}", String.valueOf(players.size())));
        for (Map.Entry<UUID, String> entry : players.entrySet()) {
            String name = entry.getValue().isEmpty() ? "?" : entry.getValue();
            sb.append('\n').append(messages.getWhitelistListPlayerEntry()
                    .replace("{name}", name)
                    .replace("{uuid}", entry.getKey().toString()));
        }
        actor.reply(messages.format(sb.toString()));
    }

    private void whitelistApply(@NotNull BungeeCommandActor actor, @NotNull String action, @NotNull String target) {
        Messages messages = engine.getMessages();

        if (IpUtil.isValidIp(target)) {
            boolean changed = action.equals("add")
                    ? engine.getWhitelist().addIp(target)
                    : engine.getWhitelist().removeIp(target);
            whitelistReply(actor, messages, action, target, changed);
            return;
        }

        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(target);
        if (player == null) {
            actor.reply(messages.format(messages.getPlayerNotFound()));
            return;
        }

        boolean changed = action.equals("add")
                ? engine.getWhitelist().addPlayer(player.getUniqueId(), target)
                : engine.getWhitelist().removePlayer(player.getUniqueId(), target);
        whitelistReply(actor, messages, action, target, changed);
    }

    private void whitelistReply(@NotNull BungeeCommandActor actor, @NotNull Messages messages, @NotNull String action, @NotNull String target, boolean changed) {
        if (action.equals("add")) {
            actor.reply(messages.formatWith(changed ? messages.getWhitelistAdd() : messages.getWhitelistAlready(), "{target}", target));
        } else {
            actor.reply(messages.formatWith(changed ? messages.getWhitelistRemove() : messages.getWhitelistNotFound(), "{target}", target));
        }
    }
}

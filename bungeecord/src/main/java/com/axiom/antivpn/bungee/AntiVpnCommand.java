package com.axiom.antivpn.bungee;

import com.axiom.antivpn.common.AntiVpnEngine;
import com.axiom.antivpn.common.config.Messages;
import com.axiom.antivpn.common.util.IpUtil;
import net.md_5.bungee.api.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bungee.actor.BungeeCommandActor;
import revxrsal.commands.bungee.annotation.CommandPermission;

import java.io.File;

@Command({"antivpn", "avpn", "axiomvpn"})
@CommandPermission("antivpn.admin")
public final class AntiVpnCommand {

    private final @NotNull AntiVpnEngine engine;

    public AntiVpnCommand(@NotNull AntiVpnEngine engine) {
        this.engine = engine;
    }

    @Command({"antivpn", "avpn", "axiomvpn"})
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
}

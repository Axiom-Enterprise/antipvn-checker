package com.axiom.antivpn.velocity;

import com.axiom.antivpn.common.AntiVpnEngine;
import com.axiom.antivpn.common.config.Messages;
import com.axiom.antivpn.common.util.IpUtil;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.velocity.actor.VelocityCommandActor;
import revxrsal.commands.velocity.annotation.CommandPermission;

@Command({"antivpn", "avpn", "axiomvpn"})
@CommandPermission("antivpn.admin")
public final class AntiVpnCommand {

    private final @NotNull AntiVpnEngine engine;

    public AntiVpnCommand(@NotNull AntiVpnEngine engine) {
        this.engine = engine;
    }

    private void reply(@NotNull VelocityCommandActor actor, @NotNull String message) {
        actor.reply(VelocitySerializer.INSTANCE.deserialize(message));
    }

    @Command({"antivpn", "avpn", "axiomvpn"})
    public void usage(@NotNull VelocityCommandActor actor) {
        Messages messages = engine.getMessages();
        reply(actor, messages.format(messages.getUsageHelp()));
    }

    @Subcommand("check")
    public void check(@NotNull VelocityCommandActor actor, @NotNull String ip) {
        Messages messages = engine.getMessages();
        if (!IpUtil.isValidIp(ip)) {
            reply(actor, messages.format(messages.getInvalidIp()));
            return;
        }

        reply(actor, messages.format("{prefix}&7Checking IP &f" + ip + "&7..."));
        engine.checkIp(ip).thenAccept(response ->
                reply(actor, messages.format(messages.getCheckResult(), response))
        ).exceptionally(ex -> {
            reply(actor, messages.format("{prefix}&cFailed to check IP: " + ex.getMessage()));
            return null;
        });
    }

    @Subcommand("status")
    public void status(@NotNull VelocityCommandActor actor) {
        Messages messages = engine.getMessages();
        long start = System.currentTimeMillis();
        engine.getStatus().thenAccept(status -> {
            long latency = System.currentTimeMillis() - start;
            if (status.isOperational()) {
                reply(actor, messages.format(messages.getStatusOnline())
                        .replace("{latency}", String.valueOf(latency))
                        .replace("{status}", status.overallStatus().toUpperCase()));
            } else {
                reply(actor, messages.format(messages.getStatusOffline()));
            }
        }).exceptionally(ex -> {
            reply(actor, messages.format(messages.getStatusOffline()));
            return null;
        });
    }

    @Subcommand("cache clear")
    public void cacheClear(@NotNull VelocityCommandActor actor) {
        Messages messages = engine.getMessages();
        long size = engine.getCache().size();
        engine.clearCache();
        reply(actor, messages.formatWith(messages.getCacheCleared(), "{size}", String.valueOf(size)));
    }

    @Subcommand("reload")
    public void reload(@NotNull VelocityCommandActor actor) {
        Messages messages = engine.getMessages();
        VelocityPlatform platform = (VelocityPlatform) engine.getPlatform();
        VelocityConfig mainConfig = new VelocityConfig(platform.getDataFolder().resolve("config.yml"), platform.getPluginLogger());
        VelocityConfig messagesConfig = new VelocityConfig(platform.getDataFolder().resolve("messages.yml"), platform.getPluginLogger());
        engine.reload(mainConfig, messagesConfig);
        reply(actor, messages.format(messages.getReloadSuccess()));
    }
}

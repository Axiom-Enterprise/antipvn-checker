package com.axiom.antivpn.bungee;

import com.axiom.antivpn.common.AntiVpnEngine;
import com.axiom.antivpn.common.command.OnlinePlayerNames;
import com.axiom.antivpn.common.config.Messages;
import com.axiom.antivpn.common.util.IpUtil;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bungee.actor.BungeeCommandActor;
import revxrsal.commands.bungee.annotation.CommandPermission;

@Command("vpn-whitelist")
@CommandPermission("antivpn.admin")
public final class VpnWhitelistCommand {

    private final @NotNull AntiVpnEngine engine;

    public VpnWhitelistCommand(@NotNull AntiVpnEngine engine) {
        this.engine = engine;
    }

    @Subcommand("add")
    public void add(@NotNull BungeeCommandActor actor, @OnlinePlayerNames @NotNull String target) {
        apply(actor, "add", target);
    }

    @Subcommand("remove")
    public void remove(@NotNull BungeeCommandActor actor, @OnlinePlayerNames @NotNull String target) {
        apply(actor, "remove", target);
    }

    private void apply(@NotNull BungeeCommandActor actor, @NotNull String action, @NotNull String target) {
        Messages messages = engine.getMessages();

        if (IpUtil.isValidIp(target)) {
            boolean changed = action.equals("add")
                    ? engine.getWhitelist().addIp(target)
                    : engine.getWhitelist().removeIp(target);
            reply(actor, messages, action, target, changed);
            return;
        }

        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(target);
        if (player == null) {
            actor.reply(messages.format(messages.getPlayerNotFound()));
            return;
        }

        boolean changed = action.equals("add")
                ? engine.getWhitelist().addPlayer(player.getUniqueId())
                : engine.getWhitelist().removePlayer(player.getUniqueId());
        reply(actor, messages, action, target, changed);
    }

    private void reply(@NotNull BungeeCommandActor actor, @NotNull Messages messages, @NotNull String action, @NotNull String target, boolean changed) {
        if (action.equals("add")) {
            actor.reply(messages.formatWith(changed ? messages.getWhitelistAdd() : messages.getWhitelistAlready(), "{target}", target));
        } else {
            actor.reply(messages.formatWith(changed ? messages.getWhitelistRemove() : messages.getWhitelistNotFound(), "{target}", target));
        }
    }
}

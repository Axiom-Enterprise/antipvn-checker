package com.axiom.antivpn.bukkit;

import com.axiom.antivpn.common.AntiVpnEngine;
import com.axiom.antivpn.common.command.OnlinePlayerNames;
import com.axiom.antivpn.common.config.Messages;
import com.axiom.antivpn.common.util.IpUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.bukkit.annotation.CommandPermission;

@Command("vpn-whitelist")
@CommandPermission("antivpn.admin")
public final class VpnWhitelistCommand {

    private final @NotNull AntiVpnEngine engine;

    public VpnWhitelistCommand(@NotNull AntiVpnEngine engine) {
        this.engine = engine;
    }

    @Subcommand("add")
    public void add(@NotNull BukkitCommandActor actor, @OnlinePlayerNames @NotNull String target) {
        apply(actor, "add", target);
    }

    @Subcommand("remove")
    public void remove(@NotNull BukkitCommandActor actor, @OnlinePlayerNames @NotNull String target) {
        apply(actor, "remove", target);
    }

    private void apply(@NotNull BukkitCommandActor actor, @NotNull String action, @NotNull String target) {
        Messages messages = engine.getMessages();

        if (IpUtil.isValidIp(target)) {
            boolean changed = action.equals("add")
                    ? engine.getWhitelist().addIp(target)
                    : engine.getWhitelist().removeIp(target);
            reply(actor, messages, action, target, changed);
            return;
        }

        Player player = Bukkit.getPlayerExact(target);
        if (player == null) {
            actor.reply(messages.format(messages.getPlayerNotFound()));
            return;
        }

        boolean changed = action.equals("add")
                ? engine.getWhitelist().addPlayer(player.getUniqueId())
                : engine.getWhitelist().removePlayer(player.getUniqueId());
        reply(actor, messages, action, target, changed);
    }

    private void reply(@NotNull BukkitCommandActor actor, @NotNull Messages messages, @NotNull String action, @NotNull String target, boolean changed) {
        if (action.equals("add")) {
            actor.reply(messages.formatWith(changed ? messages.getWhitelistAdd() : messages.getWhitelistAlready(), "{target}", target));
        } else {
            actor.reply(messages.formatWith(changed ? messages.getWhitelistRemove() : messages.getWhitelistNotFound(), "{target}", target));
        }
    }
}

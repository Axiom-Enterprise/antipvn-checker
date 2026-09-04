package com.axiom.antivpn.bungee;

import com.axiom.antivpn.common.command.OnlinePlayerNames;
import com.axiom.antivpn.common.command.VpnCommands;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Default;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bungee.actor.BungeeCommandActor;
import revxrsal.commands.bungee.annotation.CommandPermission;

@Command({"vpn", "antivpn"})
@CommandPermission(VpnCommands.ADMIN_PERMISSION)
public final class AntiVpnCommand {

    private final @NotNull VpnCommands commands;

    public AntiVpnCommand(@NotNull VpnCommands commands) {
        this.commands = commands;
    }

    @Command({"vpn", "antivpn"})
    public void usage(@NotNull BungeeCommandActor actor) {
        commands.usage(actor::reply);
    }

    @Subcommand("check")
    public void check(@NotNull BungeeCommandActor actor, @NotNull String ip) {
        commands.check(actor::reply, ip);
    }

    @Subcommand("status")
    public void status(@NotNull BungeeCommandActor actor) {
        commands.status(actor::reply);
    }

    @Subcommand("cache clear")
    public void cacheClear(@NotNull BungeeCommandActor actor) {
        commands.cacheClear(actor::reply);
    }

    @Subcommand("stats")
    public void stats(@NotNull BungeeCommandActor actor) {
        commands.stats(actor::reply);
    }

    @Subcommand("history")
    public void history(@NotNull BungeeCommandActor actor, @NotNull String player, @Default("10") int limit) {
        commands.history(actor::reply, player, limit);
    }

    @Subcommand("reload")
    public void reload(@NotNull BungeeCommandActor actor) {
        commands.reload(actor::reply);
    }

    @Subcommand("whitelist add")
    public void whitelistAdd(@NotNull BungeeCommandActor actor, @OnlinePlayerNames @NotNull String target) {
        commands.whitelistAdd(actor::reply, target);
    }

    @Subcommand("whitelist remove")
    public void whitelistRemove(@NotNull BungeeCommandActor actor, @OnlinePlayerNames @NotNull String target) {
        commands.whitelistRemove(actor::reply, target);
    }

    @Subcommand("whitelist list")
    public void whitelistList(@NotNull BungeeCommandActor actor) {
        commands.whitelistList(actor::reply);
    }
}

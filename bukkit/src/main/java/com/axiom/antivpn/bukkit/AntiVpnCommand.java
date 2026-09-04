package com.axiom.antivpn.bukkit;

import com.axiom.antivpn.common.command.OnlinePlayerNames;
import com.axiom.antivpn.common.command.VpnCommands;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Default;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.bukkit.annotation.CommandPermission;

@Command("vpn")
@CommandPermission(VpnCommands.ADMIN_PERMISSION)
public final class AntiVpnCommand {

    private final @NotNull VpnCommands commands;

    public AntiVpnCommand(@NotNull VpnCommands commands) {
        this.commands = commands;
    }

    @Command("vpn")
    public void usage(@NotNull BukkitCommandActor actor) {
        commands.usage(actor::reply);
    }

    @Subcommand("check")
    public void check(@NotNull BukkitCommandActor actor, @NotNull String ip) {
        commands.check(actor::reply, ip);
    }

    @Subcommand("status")
    public void status(@NotNull BukkitCommandActor actor) {
        commands.status(actor::reply);
    }

    @Subcommand("cache clear")
    public void cacheClear(@NotNull BukkitCommandActor actor) {
        commands.cacheClear(actor::reply);
    }

    @Subcommand("stats")
    public void stats(@NotNull BukkitCommandActor actor) {
        commands.stats(actor::reply);
    }

    @Subcommand("history")
    public void history(@NotNull BukkitCommandActor actor, @NotNull String player, @Default("10") int limit) {
        commands.history(actor::reply, player, limit);
    }

    @Subcommand("reload")
    public void reload(@NotNull BukkitCommandActor actor) {
        commands.reload(actor::reply);
    }

    @Subcommand("whitelist add")
    public void whitelistAdd(@NotNull BukkitCommandActor actor, @OnlinePlayerNames @NotNull String target) {
        commands.whitelistAdd(actor::reply, target);
    }

    @Subcommand("whitelist remove")
    public void whitelistRemove(@NotNull BukkitCommandActor actor, @OnlinePlayerNames @NotNull String target) {
        commands.whitelistRemove(actor::reply, target);
    }

    @Subcommand("whitelist list")
    public void whitelistList(@NotNull BukkitCommandActor actor) {
        commands.whitelistList(actor::reply);
    }
}

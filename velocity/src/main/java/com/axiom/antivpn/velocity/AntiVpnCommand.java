package com.axiom.antivpn.velocity;

import com.axiom.antivpn.common.color.ColorParser;
import com.axiom.antivpn.common.command.OnlinePlayerNames;
import com.axiom.antivpn.common.command.VpnCommands;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Default;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.velocity.actor.VelocityCommandActor;
import revxrsal.commands.velocity.annotation.CommandPermission;

import java.util.function.Consumer;

@Command("vpn")
@CommandPermission(VpnCommands.ADMIN_PERMISSION)
public final class AntiVpnCommand {

    private final @NotNull VpnCommands commands;

    public AntiVpnCommand(@NotNull VpnCommands commands) {
        this.commands = commands;
    }

    static @NotNull Consumer<String> reply(@NotNull VelocityCommandActor actor) {
        return message -> actor.reply(ColorParser.toComponent(message));
    }

    @Command("vpn")
    public void usage(@NotNull VelocityCommandActor actor) {
        commands.usage(reply(actor));
    }

    @Subcommand("check")
    public void check(@NotNull VelocityCommandActor actor, @NotNull String ip) {
        commands.check(reply(actor), ip);
    }

    @Subcommand("status")
    public void status(@NotNull VelocityCommandActor actor) {
        commands.status(reply(actor));
    }

    @Subcommand("cache clear")
    public void cacheClear(@NotNull VelocityCommandActor actor) {
        commands.cacheClear(reply(actor));
    }

    @Subcommand("stats")
    public void stats(@NotNull VelocityCommandActor actor) {
        commands.stats(reply(actor));
    }

    @Subcommand("history")
    public void history(@NotNull VelocityCommandActor actor, @NotNull String player, @Default("10") int limit) {
        commands.history(reply(actor), player, limit);
    }

    @Subcommand("reload")
    public void reload(@NotNull VelocityCommandActor actor) {
        commands.reload(reply(actor));
    }

    @Subcommand("whitelist add")
    public void whitelistAdd(@NotNull VelocityCommandActor actor, @OnlinePlayerNames @NotNull String target) {
        commands.whitelistAdd(reply(actor), target);
    }

    @Subcommand("whitelist remove")
    public void whitelistRemove(@NotNull VelocityCommandActor actor, @OnlinePlayerNames @NotNull String target) {
        commands.whitelistRemove(reply(actor), target);
    }

    @Subcommand("whitelist list")
    public void whitelistList(@NotNull VelocityCommandActor actor) {
        commands.whitelistList(reply(actor));
    }
}

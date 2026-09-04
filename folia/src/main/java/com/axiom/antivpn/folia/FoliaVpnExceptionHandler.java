package com.axiom.antivpn.folia;

import com.axiom.antivpn.common.command.VpnCommands;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.bukkit.exception.BukkitExceptionHandler;
import revxrsal.commands.exception.MissingArgumentException;
import revxrsal.commands.exception.NoPermissionException;
import revxrsal.commands.exception.UnknownCommandException;
import revxrsal.commands.node.ParameterNode;

public final class FoliaVpnExceptionHandler extends BukkitExceptionHandler {

    private final @NotNull VpnCommands commands;

    public FoliaVpnExceptionHandler(@NotNull VpnCommands commands) {
        this.commands = commands;
    }

    @Override
    public void onNoPermission(@NotNull NoPermissionException e, @NotNull BukkitCommandActor actor) {
        commands.noPermission(actor::reply);
    }

    @Override
    public void onUnknownCommand(@NotNull UnknownCommandException e, @NotNull BukkitCommandActor actor) {
        commands.usage(actor::reply);
    }

    @Override
    public void onMissingArgument(@NotNull MissingArgumentException e, @NotNull BukkitCommandActor actor, @NotNull ParameterNode<BukkitCommandActor, ?> parameter) {
        commands.usage(actor::reply);
    }
}

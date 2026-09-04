package com.axiom.antivpn.bungee;

import com.axiom.antivpn.common.command.VpnCommands;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.bungee.actor.BungeeCommandActor;
import revxrsal.commands.bungee.exception.BungeeExceptionHandler;
import revxrsal.commands.exception.MissingArgumentException;
import revxrsal.commands.exception.NoPermissionException;
import revxrsal.commands.exception.UnknownCommandException;
import revxrsal.commands.node.ParameterNode;

public final class BungeeVpnExceptionHandler extends BungeeExceptionHandler {

    private final @NotNull VpnCommands commands;

    public BungeeVpnExceptionHandler(@NotNull VpnCommands commands) {
        this.commands = commands;
    }

    @Override
    public void onNoPermission(@NotNull NoPermissionException e, @NotNull BungeeCommandActor actor) {
        commands.noPermission(actor::reply);
    }

    @Override
    public void onUnknownCommand(@NotNull UnknownCommandException e, @NotNull BungeeCommandActor actor) {
        commands.usage(actor::reply);
    }

    @Override
    public void onMissingArgument(@NotNull MissingArgumentException e, @NotNull BungeeCommandActor actor, @NotNull ParameterNode<BungeeCommandActor, ?> parameter) {
        commands.usage(actor::reply);
    }
}

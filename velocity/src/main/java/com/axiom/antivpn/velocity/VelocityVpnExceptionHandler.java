package com.axiom.antivpn.velocity;

import com.axiom.antivpn.common.command.VpnCommands;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.exception.MissingArgumentException;
import revxrsal.commands.exception.NoPermissionException;
import revxrsal.commands.exception.UnknownCommandException;
import revxrsal.commands.node.ParameterNode;
import revxrsal.commands.velocity.actor.VelocityCommandActor;
import revxrsal.commands.velocity.exception.VelocityExceptionHandler;

public final class VelocityVpnExceptionHandler extends VelocityExceptionHandler {

    private final @NotNull VpnCommands commands;

    public VelocityVpnExceptionHandler(@NotNull VpnCommands commands) {
        this.commands = commands;
    }

    @Override
    public void onNoPermission(@NotNull NoPermissionException e, @NotNull VelocityCommandActor actor) {
        commands.noPermission(AntiVpnCommand.reply(actor));
    }

    @Override
    public void onUnknownCommand(@NotNull UnknownCommandException e, @NotNull VelocityCommandActor actor) {
        commands.usage(AntiVpnCommand.reply(actor));
    }

    @Override
    public void onMissingArgument(@NotNull MissingArgumentException e, @NotNull VelocityCommandActor actor, @NotNull ParameterNode<VelocityCommandActor, ?> parameter) {
        commands.usage(AntiVpnCommand.reply(actor));
    }
}

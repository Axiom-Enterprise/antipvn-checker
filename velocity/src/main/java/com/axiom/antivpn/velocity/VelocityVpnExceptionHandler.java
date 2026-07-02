package com.axiom.antivpn.velocity;

import com.axiom.antivpn.common.AntiVpnEngine;
import com.axiom.antivpn.common.config.Messages;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.exception.MissingArgumentException;
import revxrsal.commands.exception.NoPermissionException;
import revxrsal.commands.exception.UnknownCommandException;
import revxrsal.commands.node.ParameterNode;
import revxrsal.commands.velocity.actor.VelocityCommandActor;
import revxrsal.commands.velocity.exception.VelocityExceptionHandler;

public final class VelocityVpnExceptionHandler extends VelocityExceptionHandler {

    private final @NotNull AntiVpnEngine engine;

    public VelocityVpnExceptionHandler(@NotNull AntiVpnEngine engine) {
        this.engine = engine;
    }

    @Override
    public void onNoPermission(@NotNull NoPermissionException e, @NotNull VelocityCommandActor actor) {
        Messages messages = engine.getMessages();
        actor.error(VelocitySerializer.INSTANCE.deserialize(messages.format(messages.getNoPermission())));
    }

    @Override
    public void onUnknownCommand(@NotNull UnknownCommandException e, @NotNull VelocityCommandActor actor) {
        Messages messages = engine.getMessages();
        actor.reply(VelocitySerializer.INSTANCE.deserialize(messages.format(messages.getUsageHelp())));
    }

    @Override
    public void onMissingArgument(@NotNull MissingArgumentException e, @NotNull VelocityCommandActor actor, @NotNull ParameterNode<VelocityCommandActor, ?> parameter) {
        Messages messages = engine.getMessages();
        actor.reply(VelocitySerializer.INSTANCE.deserialize(messages.format(messages.getUsageHelp())));
    }
}

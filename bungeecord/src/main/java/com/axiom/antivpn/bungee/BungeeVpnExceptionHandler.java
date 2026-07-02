package com.axiom.antivpn.bungee;

import com.axiom.antivpn.common.AntiVpnEngine;
import com.axiom.antivpn.common.config.Messages;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.bungee.actor.BungeeCommandActor;
import revxrsal.commands.bungee.exception.BungeeExceptionHandler;
import revxrsal.commands.exception.MissingArgumentException;
import revxrsal.commands.exception.NoPermissionException;
import revxrsal.commands.exception.UnknownCommandException;
import revxrsal.commands.node.ParameterNode;

public final class BungeeVpnExceptionHandler extends BungeeExceptionHandler {

    private final @NotNull AntiVpnEngine engine;

    public BungeeVpnExceptionHandler(@NotNull AntiVpnEngine engine) {
        this.engine = engine;
    }

    @Override
    public void onNoPermission(@NotNull NoPermissionException e, @NotNull BungeeCommandActor actor) {
        Messages messages = engine.getMessages();
        actor.reply(messages.format(messages.getNoPermission()));
    }

    @Override
    public void onUnknownCommand(@NotNull UnknownCommandException e, @NotNull BungeeCommandActor actor) {
        Messages messages = engine.getMessages();
        actor.reply(messages.format(messages.getUsageHelp()));
    }

    @Override
    public void onMissingArgument(@NotNull MissingArgumentException e, @NotNull BungeeCommandActor actor, @NotNull ParameterNode<BungeeCommandActor, ?> parameter) {
        Messages messages = engine.getMessages();
        actor.reply(messages.format(messages.getUsageHelp()));
    }
}

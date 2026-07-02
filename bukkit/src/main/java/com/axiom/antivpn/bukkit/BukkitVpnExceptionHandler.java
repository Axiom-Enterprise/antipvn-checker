package com.axiom.antivpn.bukkit;

import com.axiom.antivpn.common.AntiVpnEngine;
import com.axiom.antivpn.common.config.Messages;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.bukkit.exception.BukkitExceptionHandler;
import revxrsal.commands.exception.MissingArgumentException;
import revxrsal.commands.exception.NoPermissionException;
import revxrsal.commands.exception.UnknownCommandException;
import revxrsal.commands.node.ParameterNode;

public final class BukkitVpnExceptionHandler extends BukkitExceptionHandler {

    private final @NotNull AntiVpnEngine engine;

    public BukkitVpnExceptionHandler(@NotNull AntiVpnEngine engine) {
        this.engine = engine;
    }

    @Override
    public void onNoPermission(@NotNull NoPermissionException e, @NotNull BukkitCommandActor actor) {
        Messages messages = engine.getMessages();
        actor.reply(messages.format(messages.getNoPermission()));
    }

    @Override
    public void onUnknownCommand(@NotNull UnknownCommandException e, @NotNull BukkitCommandActor actor) {
        Messages messages = engine.getMessages();
        actor.reply(messages.format(messages.getUsageHelp()));
    }

    @Override
    public void onMissingArgument(@NotNull MissingArgumentException e, @NotNull BukkitCommandActor actor, @NotNull ParameterNode<BukkitCommandActor, ?> parameter) {
        Messages messages = engine.getMessages();
        actor.reply(messages.format(messages.getUsageHelp()));
    }
}

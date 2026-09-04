package com.axiom.antivpn.minestom;

import com.axiom.antivpn.common.color.ColorParser;
import com.axiom.antivpn.common.command.VpnCommands;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentLiteral;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.arguments.ArgumentWord;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

final class MinestomVpnCommand extends Command {

    MinestomVpnCommand(@NotNull VpnCommands commands, @NotNull PermissionChecker permissions) {
        super("vpn", "antivpn");
        setCondition((sender, input) -> permissions.hasPermission(sender, VpnCommands.ADMIN_PERMISSION));
        setDefaultExecutor((sender, context) -> commands.usage(reply(sender)));

        ArgumentWord ip = ArgumentType.Word("ip");
        ArgumentWord player = ArgumentType.Word("player");
        ArgumentWord target = ArgumentType.Word("target");
        target.setSuggestionCallback((sender, context, suggestion) -> {
            for (Player online : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
                suggestion.addEntry(new SuggestionEntry(online.getUsername()));
            }
        });
        Argument<Integer> limit = ArgumentType.Integer("limit").setDefaultValue(VpnCommands.DEFAULT_HISTORY_LIMIT);
        ArgumentLiteral whitelist = ArgumentType.Literal("whitelist");
        ArgumentLiteral cache = ArgumentType.Literal("cache");

        addSyntax((sender, context) -> commands.check(reply(sender), context.get(ip)), ArgumentType.Literal("check"), ip);
        addSyntax((sender, context) -> commands.status(reply(sender)), ArgumentType.Literal("status"));
        addSyntax((sender, context) -> commands.stats(reply(sender)), ArgumentType.Literal("stats"));
        addSyntax((sender, context) -> commands.reload(reply(sender)), ArgumentType.Literal("reload"));
        addSyntax((sender, context) -> commands.history(reply(sender), context.get(player), context.get(limit)),
                ArgumentType.Literal("history"), player, limit);
        addSyntax((sender, context) -> commands.cacheClear(reply(sender)), cache, ArgumentType.Literal("clear"));
        addSyntax((sender, context) -> commands.whitelistList(reply(sender)), whitelist, ArgumentType.Literal("list"));
        addSyntax((sender, context) -> commands.whitelistAdd(reply(sender), context.get(target)),
                whitelist, ArgumentType.Literal("add"), target);
        addSyntax((sender, context) -> commands.whitelistRemove(reply(sender), context.get(target)),
                whitelist, ArgumentType.Literal("remove"), target);
    }

    private static @NotNull Consumer<String> reply(@NotNull CommandSender sender) {
        return message -> sender.sendMessage(ColorParser.toComponent(message));
    }
}

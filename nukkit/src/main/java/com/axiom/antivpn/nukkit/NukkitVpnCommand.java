package com.axiom.antivpn.nukkit;

import cn.nukkit.command.Command;
import cn.nukkit.command.CommandExecutor;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.data.CommandParamType;
import cn.nukkit.command.data.CommandParameter;
import com.axiom.antivpn.common.command.VpnCommands;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/** Hand-rolled dispatcher: Nukkit has no Lamp module. Replies are marshalled back to the main thread. */
final class NukkitVpnCommand implements CommandExecutor {

    private final @NotNull VpnCommands commands;
    private final @NotNull NukkitPlatform platform;

    NukkitVpnCommand(@NotNull VpnCommands commands, @NotNull NukkitPlatform platform) {
        this.commands = commands;
        this.platform = platform;
    }

    /** Bedrock command UI overloads, one per subcommand. */
    static @NotNull Map<String, CommandParameter[]> parameters() {
        Map<String, CommandParameter[]> overloads = new LinkedHashMap<>();
        overloads.put("check", new CommandParameter[]{
                CommandParameter.newEnum("action", new String[]{"check"}),
                CommandParameter.newType("ip", CommandParamType.STRING)});
        overloads.put("history", new CommandParameter[]{
                CommandParameter.newEnum("action", new String[]{"history"}),
                CommandParameter.newType("player", CommandParamType.TARGET),
                CommandParameter.newType("limit", true, CommandParamType.INT)});
        overloads.put("whitelist", new CommandParameter[]{
                CommandParameter.newEnum("action", new String[]{"whitelist"}),
                CommandParameter.newEnum("operation", new String[]{"add", "remove", "list"}),
                CommandParameter.newType("target", true, CommandParamType.STRING)});
        overloads.put("cache", new CommandParameter[]{
                CommandParameter.newEnum("action", new String[]{"cache"}),
                CommandParameter.newEnum("operation", new String[]{"clear"})});
        overloads.put("simple", new CommandParameter[]{
                CommandParameter.newEnum("action", new String[]{"stats", "status", "reload"})});
        return overloads;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Consumer<String> reply = message -> {
            String rendered = BedrockText.render(message);
            platform.runSync(() -> sender.sendMessage(rendered));
        };
        if (!sender.hasPermission(VpnCommands.ADMIN_PERMISSION)) {
            commands.noPermission(reply);
            return true;
        }

        String action = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        switch (action) {
            case "check" -> {
                if (args.length < 2) commands.usage(reply);
                else commands.check(reply, args[1]);
            }
            case "status" -> commands.status(reply);
            case "stats" -> commands.stats(reply);
            case "reload" -> commands.reload(reply);
            case "history" -> {
                if (args.length < 2) commands.usage(reply);
                else commands.history(reply, args[1], args.length > 2 ? parseLimit(args[2]) : VpnCommands.DEFAULT_HISTORY_LIMIT);
            }
            case "cache" -> {
                if (args.length > 1 && args[1].equalsIgnoreCase("clear")) commands.cacheClear(reply);
                else commands.usage(reply);
            }
            case "whitelist" -> whitelist(reply, args);
            default -> commands.usage(reply);
        }
        return true;
    }

    private void whitelist(@NotNull Consumer<String> reply, @NotNull String[] args) {
        String operation = args.length < 2 ? "" : args[1].toLowerCase(Locale.ROOT);
        switch (operation) {
            case "list" -> commands.whitelistList(reply);
            case "add" -> {
                if (args.length < 3) commands.usage(reply);
                else commands.whitelistAdd(reply, args[2]);
            }
            case "remove" -> {
                if (args.length < 3) commands.usage(reply);
                else commands.whitelistRemove(reply, args[2]);
            }
            default -> commands.usage(reply);
        }
    }

    private static int parseLimit(@NotNull String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return VpnCommands.DEFAULT_HISTORY_LIMIT;
        }
    }
}

package com.axiom.antivpn.minestom;

import net.minestom.server.command.CommandSender;
import net.minestom.server.command.ConsoleSender;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface PermissionChecker {

    PermissionChecker CONSOLE_ONLY = (sender, permission) -> sender instanceof ConsoleSender;

    boolean hasPermission(@NotNull CommandSender sender, @NotNull String permission);
}

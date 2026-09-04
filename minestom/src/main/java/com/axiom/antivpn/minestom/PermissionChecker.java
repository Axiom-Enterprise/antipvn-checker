package com.axiom.antivpn.minestom;

import net.minestom.server.command.CommandSender;
import net.minestom.server.command.ConsoleSender;
import org.jetbrains.annotations.NotNull;

/**
 * Minestom ships no permission system, so the host server decides who may run {@code /vpn},
 * receive {@code antivpn.alerts} or hold {@code antivpn.bypass}.
 */
@FunctionalInterface
public interface PermissionChecker {

    /** Console only: safe default for servers that never wired a permission provider. */
    PermissionChecker CONSOLE_ONLY = (sender, permission) -> sender instanceof ConsoleSender;

    boolean hasPermission(@NotNull CommandSender sender, @NotNull String permission);
}

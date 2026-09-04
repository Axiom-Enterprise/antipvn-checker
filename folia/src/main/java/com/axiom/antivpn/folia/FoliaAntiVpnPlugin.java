package com.axiom.antivpn.folia;

import com.axiom.antivpn.bukkit.BukkitAntiVpnPlugin;

/**
 * Folia entrypoint. All behaviour lives in the Bukkit module: {@code BukkitPlatform} detects Folia at
 * runtime and routes work through the region/entity schedulers. This jar only adds {@code folia-supported}.
 */
public final class FoliaAntiVpnPlugin extends BukkitAntiVpnPlugin {
}

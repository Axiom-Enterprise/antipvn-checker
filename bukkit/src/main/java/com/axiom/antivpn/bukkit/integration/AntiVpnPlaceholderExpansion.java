package com.axiom.antivpn.bukkit.integration;

import com.axiom.antivpn.api.model.VpnResponse;
import com.axiom.antivpn.common.AntiVpnEngine;
import com.axiom.antivpn.common.integration.AntiVpnPlaceholders;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public final class AntiVpnPlaceholderExpansion extends PlaceholderExpansion {

    private final @NotNull String version;
    private final @NotNull AntiVpnEngine engine;
    private final @NotNull AntiVpnPlaceholders placeholders = new AntiVpnPlaceholders();

    @SuppressWarnings("deprecation")
    public AntiVpnPlaceholderExpansion(@NotNull Plugin plugin, @NotNull AntiVpnEngine engine) {
        this.version = plugin.getDescription().getVersion();
        this.engine = engine;
    }

    @Override public @NotNull String getIdentifier() { return "axiomantivpn"; }
    @Override public @NotNull String getAuthor() { return "Axiom"; }
    @Override public @NotNull String getVersion() { return version; }
    @Override public boolean persist() { return true; }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";
        VpnResponse response = engine.getLastResult(player.getUniqueId());
        if (response == null) return "";
        return placeholders.resolve(params, response, engine.evaluate(response));
    }
}

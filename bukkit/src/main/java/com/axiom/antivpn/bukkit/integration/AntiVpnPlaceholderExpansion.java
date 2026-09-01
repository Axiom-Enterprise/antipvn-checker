package com.axiom.antivpn.bukkit.integration;

import com.axiom.antivpn.api.model.VpnResponse;
import com.axiom.antivpn.common.AntiVpnEngine;
import com.axiom.antivpn.common.integration.AntiVpnPlaceholders;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public final class AntiVpnPlaceholderExpansion extends PlaceholderExpansion {
    private final AntiVpnEngine engine;
    private final AntiVpnPlaceholders placeholders = new AntiVpnPlaceholders();

    public AntiVpnPlaceholderExpansion(AntiVpnEngine engine) { this.engine = engine; }
    @Override public @NotNull String getIdentifier() { return "axiomantivpn"; }
    @Override public @NotNull String getAuthor() { return "Axiom"; }
    @Override public @NotNull String getVersion() { return "1.0.2"; }
    @Override public boolean persist() { return true; }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";
        VpnResponse response = engine.getLastResult(player.getUniqueId());
        if (response == null) return "";
        return placeholders.resolve(params, response, engine.evaluate(response));
    }
}

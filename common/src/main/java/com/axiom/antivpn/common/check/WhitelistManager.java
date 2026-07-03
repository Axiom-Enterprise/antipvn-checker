package com.axiom.antivpn.common.check;

import com.axiom.antivpn.common.config.PluginConfig;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class WhitelistManager {

    private final Set<String> ips = ConcurrentHashMap.newKeySet();
    private final Set<UUID> players = ConcurrentHashMap.newKeySet();
    private final Set<String> names = ConcurrentHashMap.newKeySet();
    private final @NotNull PluginConfig config;

    public WhitelistManager(@NotNull PluginConfig config) {
        this.config = config;
        load();
    }

    public void load() {
        ips.clear();
        players.clear();
        names.clear();
        for (String ip : config.getStringList("whitelist.ips")) {
            ips.add(ip.toLowerCase());
        }
        for (String uuid : config.getStringList("whitelist.players")) {
            try {
                players.add(UUID.fromString(uuid));
            } catch (IllegalArgumentException ignored) {
                ips.add(uuid.toLowerCase());
            }
        }
        for (String name : config.getStringList("whitelist.player-names")) {
            names.add(name.toLowerCase());
        }
    }

    public boolean isIpWhitelisted(@NotNull String ip) {
        return ips.contains(ip.toLowerCase());
    }

    public boolean isPlayerWhitelisted(@NotNull UUID uuid) {
        return players.contains(uuid);
    }

    public boolean isPlayerNameWhitelisted(@NotNull String name) {
        return names.contains(name.toLowerCase());
    }

    public boolean addIp(@NotNull String ip) {
        if (!ips.add(ip.toLowerCase())) {
            return false;
        }
        save();
        return true;
    }

    public boolean addPlayer(@NotNull UUID uuid) {
        if (!players.add(uuid)) {
            return false;
        }
        save();
        return true;
    }

    public boolean addPlayer(@NotNull UUID uuid, @NotNull String name) {
        boolean changed = players.add(uuid);
        changed |= names.add(name.toLowerCase());
        if (!changed) {
            return false;
        }
        save();
        return true;
    }

    public boolean removeIp(@NotNull String ip) {
        if (!ips.remove(ip.toLowerCase())) {
            return false;
        }
        save();
        return true;
    }

    public boolean removePlayer(@NotNull UUID uuid) {
        if (!players.remove(uuid)) {
            return false;
        }
        save();
        return true;
    }

    public boolean removePlayer(@NotNull UUID uuid, @NotNull String name) {
        boolean changed = players.remove(uuid);
        changed |= names.remove(name.toLowerCase());
        if (!changed) {
            return false;
        }
        save();
        return true;
    }

    private void save() {
        config.set("whitelist.ips", ips.toArray(new String[0]));
        String[] uuids = new String[players.size()];
        int i = 0;
        for (UUID uuid : players) {
            uuids[i++] = uuid.toString();
        }
        config.set("whitelist.players", uuids);
        config.set("whitelist.player-names", names.toArray(new String[0]));
        config.save();
    }

    public @NotNull Set<String> getWhitelistedIps() {
        return Set.copyOf(ips);
    }

    public @NotNull Set<UUID> getWhitelistedPlayers() {
        return Set.copyOf(players);
    }
}

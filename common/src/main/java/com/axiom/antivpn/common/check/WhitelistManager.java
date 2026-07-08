package com.axiom.antivpn.common.check;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class WhitelistManager {

    private final Set<String> ips = ConcurrentHashMap.newKeySet();
    private final Map<UUID, String> players = new ConcurrentHashMap<>();
    private final @NotNull WhitelistStorage storage;

    public WhitelistManager(@NotNull WhitelistStorage storage) {
        this.storage = storage;
        load();
    }

    public void load() {
        ips.clear();
        players.clear();
        for (String ip : storage.loadIps()) {
            ips.add(ip.toLowerCase());
        }
        for (Map.Entry<UUID, String> entry : storage.loadPlayers().entrySet()) {
            players.put(entry.getKey(), entry.getValue() != null ? entry.getValue() : "");
        }
    }

    public boolean isIpWhitelisted(@NotNull String ip) {
        return ips.contains(ip.toLowerCase());
    }

    public boolean isPlayerWhitelisted(@NotNull UUID uuid) {
        return players.containsKey(uuid);
    }

    public boolean isPlayerNameWhitelisted(@NotNull String name) {
        for (String whitelistedName : players.values()) {
            if (whitelistedName.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    public boolean addIp(@NotNull String ip) {
        String lower = ip.toLowerCase();
        if (!ips.add(lower)) {
            return false;
        }
        storage.addIp(lower);
        return true;
    }

    public boolean addPlayer(@NotNull UUID uuid) {
        if (players.putIfAbsent(uuid, "") != null) {
            return false;
        }
        storage.addPlayer(uuid, null);
        return true;
    }

    public boolean addPlayer(@NotNull UUID uuid, @NotNull String name) {
        String lowerName = name.toLowerCase();
        String previous = players.put(uuid, lowerName);
        if (lowerName.equals(previous)) {
            return false;
        }
        storage.addPlayer(uuid, lowerName);
        return true;
    }

    public boolean removeIp(@NotNull String ip) {
        String lower = ip.toLowerCase();
        if (!ips.remove(lower)) {
            return false;
        }
        storage.removeIp(lower);
        return true;
    }

    public boolean removePlayer(@NotNull UUID uuid) {
        if (players.remove(uuid) == null) {
            return false;
        }
        storage.removePlayer(uuid);
        return true;
    }

    public boolean removePlayer(@NotNull UUID uuid, @NotNull String name) {
        return removePlayer(uuid);
    }

    public @NotNull Set<String> getWhitelistedIps() {
        return Set.copyOf(ips);
    }

    public @NotNull Map<UUID, String> getWhitelistedPlayers() {
        return Map.copyOf(players);
    }
}

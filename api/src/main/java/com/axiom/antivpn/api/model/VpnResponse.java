package com.axiom.antivpn.api.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record VpnResponse(
        @NotNull String ip,
        boolean vpn,
        boolean proxy,
        boolean tor,
        boolean datacenter,
        boolean residential,
        boolean mobile,
        int riskScore,
        @Nullable String isp,
        @Nullable String country,
        @Nullable String countryCode,
        @Nullable String city,
        @Nullable String region,
        double latitude,
        double longitude,
        @Nullable String timezone,
        @Nullable String asn,
        @Nullable String org,
        boolean cached,
        long timestamp
) {

    public boolean isBlocked() {
        return vpn || proxy || tor || datacenter;
    }

    public boolean isSafe() {
        return !isBlocked() && riskScore < 50;
    }
}

package com.axiom.antivpn.common.cache;

import com.axiom.antivpn.api.model.VpnResponse;
import com.axiom.antivpn.common.config.Settings;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;

public final class VpnCache {

    private Cache<String, VpnResponse> cache;

    public VpnCache(@NotNull Settings settings) {
        rebuild(settings);
    }

    public void rebuild(@NotNull Settings settings) {
        Cache<String, VpnResponse> old = this.cache;
        this.cache = Caffeine.newBuilder()
                .maximumSize(settings.getCacheMaxSize())
                .expireAfterWrite(Duration.ofSeconds(settings.getCacheTtlSeconds()))
                .build();

        if (old != null) {
            old.invalidateAll();
        }
    }

    public @Nullable VpnResponse get(@NotNull String ip) {
        return cache.getIfPresent(ip);
    }

    public void put(@NotNull String ip, @NotNull VpnResponse response) {
        VpnResponse cached = new VpnResponse(
                response.ip(), response.vpn(), response.proxy(), response.tor(),
                response.datacenter(), response.residential(), response.mobile(),
                response.riskScore(), response.isp(), response.country(), response.countryCode(),
                response.city(), response.region(), response.latitude(), response.longitude(),
                response.timezone(), response.asn(), response.org(),
                true, response.timestamp()
        );
        cache.put(ip, cached);
    }

    public void invalidate(@NotNull String ip) {
        cache.invalidate(ip);
    }

    public long size() {
        cache.cleanUp();
        return cache.estimatedSize();
    }

    public void clear() {
        cache.invalidateAll();
    }
}

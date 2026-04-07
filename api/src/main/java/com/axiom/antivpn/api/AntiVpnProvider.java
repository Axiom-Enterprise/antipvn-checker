package com.axiom.antivpn.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class AntiVpnProvider {

    private static volatile @Nullable AntiVpnAPI instance;

    private AntiVpnProvider() {
    }

    public static @NotNull AntiVpnAPI getApi() {
        AntiVpnAPI api = instance;
        if (api == null) {
            throw new IllegalStateException("AxiomAntiVPN is not loaded yet");
        }
        return api;
    }

    public static boolean isAvailable() {
        return instance != null;
    }

    public static void register(@NotNull AntiVpnAPI api) {
        instance = api;
    }

    public static void unregister() {
        instance = null;
    }
}

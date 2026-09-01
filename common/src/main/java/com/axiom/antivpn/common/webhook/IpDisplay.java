package com.axiom.antivpn.common.webhook;

import org.jetbrains.annotations.NotNull;

public enum IpDisplay {
    FULL,
    MASKED,
    HIDDEN;

    public @NotNull String render(@NotNull String ip) {
        return switch (this) {
            case FULL -> ip;
            case HIDDEN -> "hidden";
            case MASKED -> mask(ip);
        };
    }

    private static @NotNull String mask(@NotNull String ip) {
        int dot = ip.lastIndexOf('.');
        if (dot > 0) return ip.substring(0, dot + 1) + "xxx";
        int colon = ip.lastIndexOf(':');
        if (colon > 0) return ip.substring(0, colon + 1) + "xxxx";
        return "hidden";
    }
}

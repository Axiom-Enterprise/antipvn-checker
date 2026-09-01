package com.axiom.antivpn.common.policy;

import org.jetbrains.annotations.NotNull;

public final class CommandTemplate {
    private static final int MAX_COMMAND_LENGTH = 512;

    private CommandTemplate() {
    }

    public static @NotNull String render(@NotNull String template, @NotNull String player,
                                         @NotNull String ip, int riskScore, @NotNull String detection) {
        String rendered = template
                .replace("{player}", data(player))
                .replace("{ip}", data(ip))
                .replace("{risk_score}", Integer.toString(riskScore))
                .replace("{detection}", data(detection));
        if (rendered.length() > MAX_COMMAND_LENGTH) {
            throw new IllegalArgumentException("Rendered command exceeds 512 characters");
        }
        return rendered;
    }

    private static @NotNull String data(@NotNull String value) {
        return value.replace('\r', ' ').replace('\n', ' ').strip();
    }
}

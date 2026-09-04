package com.axiom.antivpn.common.check;

import com.axiom.antivpn.api.model.VpnResponse;
import com.axiom.antivpn.common.policy.PolicyDecision;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Outcome of a login check. {@code kickMessage} is already rendered (legacy section string) when the
 * connection must be refused; {@code response} and {@code decision} are null when no API check ran.
 */
public record LoginVerdict(@Nullable VpnResponse response, @Nullable PolicyDecision decision, @Nullable String kickMessage) {

    public static final LoginVerdict ALLOW = new LoginVerdict(null, null, null);

    public static @NotNull LoginVerdict deny(@NotNull String kickMessage) {
        return new LoginVerdict(null, null, kickMessage);
    }

    public boolean denied() {
        return kickMessage != null;
    }

    public boolean checked() {
        return response != null && decision != null;
    }
}

package com.axiom.antivpn.velocity;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class VelocitySerializer {

    public static final LegacyComponentSerializer INSTANCE = LegacyComponentSerializer.builder()
            .character('§')
            .hexColors()
            .build();

    private VelocitySerializer() {
    }
}

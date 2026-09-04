package com.axiom.antivpn.nukkit;

import com.axiom.antivpn.common.color.ColorParser;
import org.jetbrains.annotations.NotNull;

final class BedrockText {

    private BedrockText() {
    }

    static @NotNull String render(@NotNull String sectionText) {
        return ColorParser.downsampleHex(sectionText);
    }
}

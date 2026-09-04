package com.axiom.antivpn.nukkit;

import com.axiom.antivpn.common.color.ColorParser;
import org.jetbrains.annotations.NotNull;

/** Bedrock clients render legacy section codes but not RGB, so hex and gradients collapse to the 16-colour palette. */
final class BedrockText {

    private BedrockText() {
    }

    static @NotNull String render(@NotNull String sectionText) {
        return ColorParser.downsampleHex(sectionText);
    }
}

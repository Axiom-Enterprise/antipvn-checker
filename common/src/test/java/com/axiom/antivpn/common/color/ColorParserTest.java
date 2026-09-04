package com.axiom.antivpn.common.color;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColorParserTest {

    @Test
    void legacyHexRgbAndGradient() {
        assertEquals("§a§lHi", ColorParser.parse("&a&lHi"));
        assertEquals("§x§7§c§3§a§e§dX", ColorParser.parse("&#7C3AEDX"));
        assertEquals("§x§f§f§0§0§0§0X", ColorParser.parse("{rgb:255,0,0}X"));

        String gradient = ColorParser.parse("{gradient:#000000:#ffffff}ab{/gradient}");
        assertEquals("§x§0§0§0§0§0§0a§x§f§f§f§f§f§fb", gradient);
        assertEquals("ab", ColorParser.strip(gradient));

        String bold = ColorParser.parse("{gradient:#000000:#ffffff}&lab{/gradient}");
        assertEquals("§x§0§0§0§0§0§0§la§x§f§f§f§f§f§f§lb", bold);
    }

    @Test
    void miniMessageKeepsHexColours() {
        String out = ColorParser.parse("mm:<#7c3aed><bold>Axiom</bold>");
        assertTrue(out.startsWith("§x§7§c§3§a§e§d"), out);
        assertTrue(out.contains("§l"), out);
        assertEquals("Axiom", ColorParser.strip(out));
    }

    @Test
    void miniMessageRoundTripOfLegacyPrefix() {
        String mini = ColorParser.toMiniMessage("&#7C3AED&lAxiom &8» &7");
        assertFalse(mini.contains("&"), mini);
        assertEquals(ColorParser.parse("&#7C3AED&lAxiom &8» &7T"), ColorParser.parse("mm:" + mini + "T"));
        assertEquals("§8» §x§7§c§3§a§e§d§lT", ColorParser.parse("mm:" + ColorParser.toMiniMessage("&8» &#7C3AED&l") + "T"));
    }

    @Test
    void bedrockDownsampling() {
        assertEquals("§9X §cY", ColorParser.downsampleHex("§x§7§c§3§a§e§dX §x§f§f§5§5§5§5Y"));
        assertEquals("§5Z", ColorParser.downsampleHex("§x§a§a§0§0§a§aZ"));
        assertEquals("§aplain", ColorParser.downsampleHex("§aplain"));
    }
}

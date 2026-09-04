package com.axiom.antivpn.common.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YamlConfigTest {

    @Test
    void readsNestedValuesListsAndNumericKeys(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("config.yml");
        Files.writeString(file, """
                api:
                  key: "axm_123"
                  timeout-ms: 5000
                detection:
                  blocked-types: ["VPN", "PROXY"]
                actions:
                  by-risk-score:
                    75: "ALERT"
                    90: "KICK"
                kick-message:
                  - "&cDon't use a VPN"
                  - ""
                alerts:
                  enabled: false
                """);
        YamlConfig config = new YamlConfig(file, Logger.getAnonymousLogger());

        assertEquals("axm_123", config.getString("api.key", ""));
        assertEquals(5000, config.getInt("api.timeout-ms", 0));
        assertEquals(List.of("VPN", "PROXY"), config.getStringList("detection.blocked-types"));
        assertEquals(Map.of("75", "ALERT", "90", "KICK"), config.getSection("actions.by-risk-score"));
        assertEquals(List.of("&cDon't use a VPN", ""), config.getStringList("kick-message"));
        assertFalse(config.getBoolean("alerts.enabled", true));
        assertEquals("fallback", config.getString("kick-message", "fallback"));
        assertTrue(config.contains("api.key"));
        assertFalse(config.contains("api.missing"));
    }

    @Test
    void setSaveAndReloadRoundTrip(@TempDir Path dir) {
        Path file = dir.resolve("config.yml");
        YamlConfig config = new YamlConfig(file, Logger.getAnonymousLogger());
        config.set("whitelist.ips", List.of("1.1.1.1"));
        config.set("network.mode", "PROXY");
        config.save();
        config.set("network.mode", null);

        config.reload();
        assertEquals("PROXY", config.getString("network.mode", ""));
        assertEquals(List.of("1.1.1.1"), config.getStringList("whitelist.ips"));
    }
}

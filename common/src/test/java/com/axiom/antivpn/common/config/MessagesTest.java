package com.axiom.antivpn.common.config;

import com.axiom.antivpn.api.model.VpnResponse;
import com.axiom.antivpn.common.policy.EnforcementAction;
import com.axiom.antivpn.common.policy.PolicyDecision;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessagesTest {

    private static final VpnResponse RESPONSE = new VpnResponse("1.2.3.4", true, false, false, false, false, false, 88,
            "Evil ISP", "Italy", "IT", "Rome", "Lazio", 0, 0, "Europe/Rome", "AS1", "Org", false, 0L);

    @Test
    void kickAndAlertAcceptListsOrScalars() {
        MemoryConfig config = new MemoryConfig();
        config.values.put("kick-message", List.of("&cLine1", "", "mm:<red>Score {risk_score}"));
        config.values.put("alert-message", "{prefix}{player} {action} {reason} {ip}");
        config.values.put("prefix", "&8[&dA&8] ");
        Messages messages = new Messages(config);

        assertEquals("§cLine1\n\n§cScore 88", messages.formatKick(RESPONSE));
        String alert = messages.formatAlert("Steve", RESPONSE, new PolicyDecision(EnforcementAction.KICK, true, "VPN", List.of()));
        assertEquals("§8[§dA§8] Steve KICK VPN 1.2.3.4", alert);
        assertEquals("§cLine1\n\n§cScore N/A", messages.formatKick());
    }

    @Test
    void miniMessageTemplatesGetMiniMessagePrefixAndEscapedValues() {
        MemoryConfig config = new MemoryConfig();
        config.values.put("prefix", "&#7C3AED&lAxiom &7");
        config.values.put("check-result", "mm:{prefix}<white>{isp} {vpn_status}");
        Messages messages = new Messages(config);

        VpnResponse tagged = new VpnResponse("1.2.3.4", true, false, false, false, false, false, 1,
                "<red>Injected", null, null, null, null, 0, 0, null, null, null, false, 0L);
        String out = messages.format(messages.getCheckResult(), tagged);
        assertTrue(out.startsWith("§x§7§c§3§a§e§d§lAxiom "), out);
        assertTrue(out.contains("<red>Injected"), out);
        assertTrue(out.endsWith("§c§lBLOCKED"), out);
    }

    @Test
    void pairsReplacePlaceholders() {
        Messages messages = new Messages(new MemoryConfig());
        assertTrue(messages.format("{prefix}&a{size} removed", "{size}", "42").endsWith("§a42 removed"));
    }

    private static final class MemoryConfig implements PluginConfig {
        final Map<String, Object> values = new HashMap<>();

        @Override public String getString(String path, String def) {
            Object v = values.get(path);
            return v instanceof String s ? s : def;
        }
        @Override public int getInt(String path, int def) { return def; }
        @Override public long getLong(String path, long def) { return def; }
        @Override public boolean getBoolean(String path, boolean def) { return def; }
        @Override public double getDouble(String path, double def) { return def; }
        @Override public List<String> getStringList(String path) {
            Object v = values.get(path);
            if (!(v instanceof List<?> list)) return List.of();
            List<String> out = new java.util.ArrayList<>(list.size());
            for (Object item : list) out.add(String.valueOf(item));
            return out;
        }
        @Override public Set<String> getKeys(String path) { return Set.of(); }
        @Override public Map<String, Object> getSection(String path) { return Map.of(); }
        @Override public boolean contains(String path) { return values.containsKey(path); }
        @Override public void set(String path, Object value) { values.put(path, value); }
        @Override public void save() { }
        @Override public void reload() { }
    }
}

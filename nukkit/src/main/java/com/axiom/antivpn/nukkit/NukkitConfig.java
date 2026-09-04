package com.axiom.antivpn.nukkit;

import cn.nukkit.utils.Config;
import com.axiom.antivpn.common.config.PluginConfig;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class NukkitConfig implements PluginConfig {

    private final @NotNull Config config;

    NukkitConfig(@NotNull File file) {
        this.config = new Config(file, Config.YAML);
    }

    @Override
    public @NotNull String getString(@NotNull String path, @NotNull String def) {
        Object value = config.get(path);
        if (value == null || value instanceof Map<?, ?> || value instanceof List<?>) return def;
        return String.valueOf(value);
    }

    @Override
    public int getInt(@NotNull String path, int def) {
        return config.getInt(path, def);
    }

    @Override
    public long getLong(@NotNull String path, long def) {
        return config.getLong(path, def);
    }

    @Override
    public boolean getBoolean(@NotNull String path, boolean def) {
        return config.getBoolean(path, def);
    }

    @Override
    public double getDouble(@NotNull String path, double def) {
        return config.getDouble(path, def);
    }

    @Override
    public @NotNull List<String> getStringList(@NotNull String path) {
        return config.getStringList(path);
    }

    @Override
    public @NotNull Set<String> getKeys(@NotNull String path) {
        if (!(config.get(path) instanceof Map<?, ?> map)) return Set.of();
        Set<String> keys = new LinkedHashSet<>(map.size());
        for (Object key : map.keySet()) {
            keys.add(String.valueOf(key));
        }
        return keys;
    }

    @Override
    public @NotNull Map<String, Object> getSection(@NotNull String path) {
        if (!(config.get(path) instanceof Map<?, ?> map)) return Map.of();
        Map<String, Object> section = new LinkedHashMap<>(map.size());
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            section.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return section;
    }

    @Override
    public boolean contains(@NotNull String path) {
        return config.get(path) != null;
    }

    @Override
    public void set(@NotNull String path, Object value) {
        if (value == null) {
            config.remove(path);
        } else {
            config.set(path, value);
        }
    }

    @Override
    public void save() {
        config.save();
    }

    @Override
    public void reload() {
        config.reload();
    }
}

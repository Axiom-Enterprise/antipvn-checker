package com.axiom.antivpn.common.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.representer.Representer;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class YamlConfig implements PluginConfig {

    private final @NotNull Path file;
    private final @NotNull Logger logger;
    private final @NotNull Yaml yaml;
    private volatile Map<Object, Object> root = new LinkedHashMap<>();

    public YamlConfig(@NotNull Path file, @NotNull Logger logger) {
        this.file = file;
        this.logger = logger;
        LoaderOptions loader = new LoaderOptions();
        DumperOptions dumper = new DumperOptions();
        dumper.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumper.setPrettyFlow(true);
        dumper.setIndent(2);
        this.yaml = new Yaml(new SafeConstructor(loader), new Representer(dumper), dumper, loader);
        reload();
    }

    @Override
    public @NotNull String getString(@NotNull String path, @NotNull String def) {
        Object value = resolve(path);
        if (value == null || value instanceof Map<?, ?> || value instanceof List<?>) return def;
        return String.valueOf(value);
    }

    @Override
    public int getInt(@NotNull String path, int def) {
        Object value = resolve(path);
        if (value instanceof Number n) return n.intValue();
        if (value instanceof String s) {
            try { return Integer.parseInt(s.trim()); } catch (NumberFormatException ignored) { }
        }
        return def;
    }

    @Override
    public long getLong(@NotNull String path, long def) {
        Object value = resolve(path);
        if (value instanceof Number n) return n.longValue();
        if (value instanceof String s) {
            try { return Long.parseLong(s.trim()); } catch (NumberFormatException ignored) { }
        }
        return def;
    }

    @Override
    public boolean getBoolean(@NotNull String path, boolean def) {
        Object value = resolve(path);
        if (value instanceof Boolean b) return b;
        if (value instanceof String s) return Boolean.parseBoolean(s.trim());
        return def;
    }

    @Override
    public double getDouble(@NotNull String path, double def) {
        Object value = resolve(path);
        if (value instanceof Number n) return n.doubleValue();
        if (value instanceof String s) {
            try { return Double.parseDouble(s.trim()); } catch (NumberFormatException ignored) { }
        }
        return def;
    }

    @Override
    public @NotNull List<String> getStringList(@NotNull String path) {
        if (!(resolve(path) instanceof List<?> list)) return List.of();
        List<String> result = new ArrayList<>(list.size());
        for (Object item : list) {
            result.add(item == null ? "" : String.valueOf(item));
        }
        return result;
    }

    @Override
    public @NotNull Set<String> getKeys(@NotNull String path) {
        if (!(resolve(path) instanceof Map<?, ?> map)) return Set.of();
        Set<String> keys = new LinkedHashSet<>(map.size());
        for (Object key : map.keySet()) {
            keys.add(String.valueOf(key));
        }
        return keys;
    }

    @Override
    public @NotNull Map<String, Object> getSection(@NotNull String path) {
        if (!(resolve(path) instanceof Map<?, ?> map)) return Map.of();
        Map<String, Object> result = new LinkedHashMap<>(map.size());
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            result.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return result;
    }

    @Override
    public boolean contains(@NotNull String path) {
        return resolve(path) != null;
    }

    @Override
    public void set(@NotNull String path, @Nullable Object value) {
        String[] parts = path.split("\\.");
        Map<Object, Object> current = root;
        for (int i = 0; i < parts.length - 1; i++) {
            Object next = current.get(parts[i]);
            if (next instanceof Map<?, ?> map) {
                @SuppressWarnings("unchecked")
                Map<Object, Object> typed = (Map<Object, Object>) map;
                current = typed;
            } else {
                Map<Object, Object> created = new LinkedHashMap<>();
                current.put(parts[i], created);
                current = created;
            }
        }
        String leaf = parts[parts.length - 1];
        if (value == null) {
            current.remove(leaf);
        } else {
            current.put(leaf, value);
        }
    }

    @Override
    public void save() {
        try {
            Files.writeString(file, yaml.dump(root), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to save config: " + file.getFileName(), e);
        }
    }

    @Override
    public void reload() {
        if (!Files.exists(file)) {
            root = new LinkedHashMap<>();
            return;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Object loaded = yaml.load(reader);
            Map<Object, Object> parsed = new LinkedHashMap<>();
            if (loaded instanceof Map<?, ?> map) {
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    parsed.put(entry.getKey(), entry.getValue());
                }
            }
            root = parsed;
        } catch (IOException | RuntimeException e) {
            logger.log(Level.SEVERE, "Failed to load config: " + file.getFileName(), e);
        }
    }

    private @Nullable Object resolve(@NotNull String path) {
        Object current = root;
        int start = 0;
        while (true) {
            if (!(current instanceof Map<?, ?> map)) return null;
            int dot = path.indexOf('.', start);
            String key = dot < 0 ? path.substring(start) : path.substring(start, dot);
            current = map.get(key);
            if (dot < 0) return current;
            start = dot + 1;
        }
    }
}

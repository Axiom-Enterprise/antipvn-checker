package com.axiom.antivpn.velocity;

import com.axiom.antivpn.common.config.PluginConfig;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class VelocityConfig implements PluginConfig {

    private final @NotNull Path filePath;
    private final @NotNull Logger logger;
    private final Map<String, Object> data = new LinkedHashMap<>();

    public VelocityConfig(@NotNull Path filePath, @NotNull Logger logger) {
        this.filePath = filePath;
        this.logger = logger;
        reload();
    }

    @Override
    public @NotNull String getString(@NotNull String path, @NotNull String def) {
        Object val = resolve(path);
        return val instanceof String s ? s : def;
    }

    @Override
    public int getInt(@NotNull String path, int def) {
        Object val = resolve(path);
        if (val instanceof Number n) return n.intValue();
        if (val instanceof String s) {
            try { return Integer.parseInt(s.trim()); } catch (NumberFormatException ignored) {}
        }
        return def;
    }

    @Override
    public long getLong(@NotNull String path, long def) {
        Object val = resolve(path);
        if (val instanceof Number n) return n.longValue();
        if (val instanceof String s) {
            try { return Long.parseLong(s.trim()); } catch (NumberFormatException ignored) {}
        }
        return def;
    }

    @Override
    public boolean getBoolean(@NotNull String path, boolean def) {
        Object val = resolve(path);
        if (val instanceof Boolean b) return b;
        if (val instanceof String s) return Boolean.parseBoolean(s.trim());
        return def;
    }

    @Override
    public double getDouble(@NotNull String path, double def) {
        Object val = resolve(path);
        if (val instanceof Number n) return n.doubleValue();
        if (val instanceof String s) {
            try { return Double.parseDouble(s.trim()); } catch (NumberFormatException ignored) {}
        }
        return def;
    }

    @Override
    public @NotNull List<String> getStringList(@NotNull String path) {
        Object val = resolve(path);
        if (val instanceof List<?> list) {
            List<String> result = new ArrayList<>(list.size());
            for (Object item : list) {
                result.add(String.valueOf(item));
            }
            return result;
        }
        return List.of();
    }

    @Override
    public @NotNull Set<String> getKeys(@NotNull String path) {
        Object val = resolve(path);
        if (val instanceof Map<?, ?> map) {
            Set<String> keys = new LinkedHashSet<>();
            for (Object key : map.keySet()) {
                keys.add(String.valueOf(key));
            }
            return keys;
        }
        return Set.of();
    }

    @Override
    public @NotNull Map<String, Object> getSection(@NotNull String path) {
        Object val = resolve(path);
        if (val instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                result.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return result;
        }
        return Map.of();
    }

    @Override
    public boolean contains(@NotNull String path) {
        return resolve(path) != null;
    }

    @Override
    public void set(@NotNull String path, Object value) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = data;
        for (int i = 0; i < parts.length - 1; i++) {
            Object next = current.get(parts[i]);
            if (next instanceof Map<?, ?> map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> typedMap = (Map<String, Object>) map;
                current = typedMap;
            } else {
                Map<String, Object> newMap = new LinkedHashMap<>();
                current.put(parts[i], newMap);
                current = newMap;
            }
        }
        if (value == null) {
            current.remove(parts[parts.length - 1]);
        } else {
            current.put(parts[parts.length - 1], value);
        }
    }

    @Override
    public void save() {
        List<String> lines = new ArrayList<>();
        writeMap(lines, data, 0);
        try {
            Files.write(filePath, lines);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to save config: " + filePath.getFileName(), e);
        }
    }

    @Override
    public void reload() {
        data.clear();
        if (!Files.exists(filePath)) return;
        try {
            List<String> rawLines = Files.readAllLines(filePath);
            parseYaml(rawLines, data, 0, 0);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to load config: " + filePath.getFileName(), e);
        }
    }

    private Object resolve(@NotNull String path) {
        String[] parts = path.split("\\.");
        Object current = data;
        for (String part : parts) {
            if (current instanceof Map<?, ?> map) {
                current = map.get(part);
            } else {
                return null;
            }
        }
        return current;
    }

    private int parseYaml(List<String> lines, Map<String, Object> target, int startLine, int baseIndent) {
        int i = startLine;
        while (i < lines.size()) {
            String line = lines.get(i);
            String trimmed = line.trim();

            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                i++;
                continue;
            }

            int indent = countIndent(line);
            if (indent < baseIndent) return i;

            if (trimmed.startsWith("- ")) {
                return i;
            }

            int colonIdx = trimmed.indexOf(':');
            if (colonIdx == -1) {
                i++;
                continue;
            }

            String key = trimmed.substring(0, colonIdx).trim();
            String valueStr = trimmed.substring(colonIdx + 1).trim();

            if (valueStr.isEmpty()) {
                if (i + 1 < lines.size()) {
                    String nextTrimmed = lines.get(i + 1).trim();
                    int nextIndent = countIndent(lines.get(i + 1));
                    if (nextTrimmed.startsWith("- ")) {
                        List<String> list = new ArrayList<>();
                        i++;
                        while (i < lines.size()) {
                            String listLine = lines.get(i).trim();
                            int listIndent = countIndent(lines.get(i));
                            if (listLine.startsWith("- ") && listIndent >= nextIndent) {
                                list.add(listLine.substring(2).trim().replace("\"", "").replace("'", ""));
                                i++;
                            } else {
                                break;
                            }
                        }
                        target.put(key, list);
                    } else if (nextIndent > indent) {
                        Map<String, Object> nested = new LinkedHashMap<>();
                        i = parseYaml(lines, nested, i + 1, nextIndent);
                        target.put(key, nested);
                    } else {
                        target.put(key, "");
                        i++;
                    }
                } else {
                    target.put(key, "");
                    i++;
                }
            } else {
                target.put(key, parseValue(valueStr));
                i++;
            }
        }
        return i;
    }

    private Object parseValue(String value) {
        if (value.startsWith("\"") && value.endsWith("\"")) {
            return unescapeDoubleQuoted(value.substring(1, value.length() - 1));
        }
        if (value.startsWith("'") && value.endsWith("'")) {
            return value.substring(1, value.length() - 1);
        }
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        try { return Integer.parseInt(value); } catch (NumberFormatException ignored) {}
        try { return Long.parseLong(value); } catch (NumberFormatException ignored) {}
        try { return Double.parseDouble(value); } catch (NumberFormatException ignored) {}
        return value;
    }

    private @NotNull String unescapeDoubleQuoted(@NotNull String value) {
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\' && i + 1 < value.length()) {
                char next = value.charAt(i + 1);
                switch (next) {
                    case 'n' -> { sb.append('\n'); i++; }
                    case 't' -> { sb.append('\t'); i++; }
                    case '\\' -> { sb.append('\\'); i++; }
                    case '"' -> { sb.append('"'); i++; }
                    default -> sb.append(c);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private int countIndent(String line) {
        int count = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ') count++;
            else break;
        }
        return count;
    }

    private void writeMap(List<String> lines, Map<String, Object> map, int indent) {
        String prefix = " ".repeat(indent);
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            switch (value) {
                case Map<?, ?> nested -> {
                    lines.add(prefix + entry.getKey() + ":");
                    @SuppressWarnings("unchecked")
                    Map<String, Object> typedNested = (Map<String, Object>) nested;
                    writeMap(lines, typedNested, indent + 2);
                }
                case List<?> list -> {
                    lines.add(prefix + entry.getKey() + ":");
                    for (Object item : list) {
                        lines.add(prefix + "  - \"" + item + "\"");
                    }
                }
                case String s -> lines.add(prefix + entry.getKey() + ": \"" + s + "\"");
                case null, default -> lines.add(prefix + entry.getKey() + ": " + value);
            }
        }
    }
}

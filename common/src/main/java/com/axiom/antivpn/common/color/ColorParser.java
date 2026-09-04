package com.axiom.antivpn.common.color;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renders every supported text syntax into a section-sign legacy string.
 * <p>
 * Supported inputs: legacy {@code &a}, hex {@code &#RRGGBB}, {@code {rgb:R,G,B}},
 * {@code {gradient:#RRGGBB:#RRGGBB}text{/gradient}} and MiniMessage when the value starts with {@code mm:}.
 * Hex colours are emitted as {@code §x§R§R§G§G§B§B}, the form every Java platform understands.
 */
public final class ColorParser {

    public static final String MINI_MESSAGE_PREFIX = "mm:";
    public static final char SECTION = '§';

    private static final String LEGACY_CODES = "0123456789abcdefklmnor";
    private static final String HEX_DIGITS = "0123456789abcdef";
    private static final Pattern HEX = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern RGB = Pattern.compile("\\{rgb:(\\d{1,3}),(\\d{1,3}),(\\d{1,3})}");
    private static final Pattern GRADIENT = Pattern.compile("\\{gradient:#([A-Fa-f0-9]{6}):#([A-Fa-f0-9]{6})}(.*?)\\{/gradient}", Pattern.DOTALL);
    private static final Pattern SECTION_HEX = Pattern.compile(SECTION + "x(?:" + SECTION + "[A-Fa-f0-9]){6}");
    private static final Pattern ANY_CODE = Pattern.compile("[&" + SECTION + "][0-9A-Fa-fK-Ok-oRrXx]");

    private static final int[] PALETTE = {
            0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
            0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF
    };

    private static final LegacyComponentSerializer SECTION_SERIALIZER = LegacyComponentSerializer.builder()
            .character(SECTION)
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private ColorParser() {
    }

    public static boolean isMiniMessage(@NotNull String input) {
        return input.startsWith(MINI_MESSAGE_PREFIX);
    }

    public static @NotNull String parse(@NotNull String input) {
        if (isMiniMessage(input)) {
            return SECTION_SERIALIZER.serialize(MINI_MESSAGE.deserialize(input.substring(MINI_MESSAGE_PREFIX.length())));
        }
        return parseLegacy(parseHex(parseRgb(parseGradients(input))));
    }

    public static @NotNull Component toComponent(@NotNull String input) {
        return SECTION_SERIALIZER.deserialize(parse(input));
    }

    /**
     * Component round-trips drop styles that carry no text, but a prefix such as {@code "&8» &7"} relies on
     * its trailing colour to style what follows; those trailing codes are re-emitted as open tags.
     */
    public static @NotNull String toMiniMessage(@NotNull String input) {
        String legacy = parse(input);
        return MINI_MESSAGE.serialize(SECTION_SERIALIZER.deserialize(legacy)) + trailingStyleTags(legacy);
    }

    public static @NotNull String escapeMiniMessage(@NotNull String raw) {
        return MINI_MESSAGE.escapeTags(raw);
    }

    /** Bedrock has no RGB text colours: every {@code §x} sequence becomes the closest legacy code. */
    public static @NotNull String downsampleHex(@NotNull String sectionText) {
        Matcher matcher = SECTION_HEX.matcher(sectionText);
        if (!matcher.find()) return sectionText;
        StringBuilder out = new StringBuilder(sectionText.length());
        do {
            String seq = matcher.group();
            int rgb = 0;
            for (int i = 3; i < seq.length(); i += 2) {
                rgb = (rgb << 4) | Character.digit(seq.charAt(i), 16);
            }
            matcher.appendReplacement(out, Matcher.quoteReplacement("" + SECTION + nearestLegacy(rgb)));
        } while (matcher.find());
        matcher.appendTail(out);
        return out.toString();
    }

    public static @NotNull String strip(@NotNull String input) {
        String result = GRADIENT.matcher(input).replaceAll("$3");
        result = HEX.matcher(result).replaceAll("");
        result = RGB.matcher(result).replaceAll("");
        result = SECTION_HEX.matcher(result).replaceAll("");
        return ANY_CODE.matcher(result).replaceAll("");
    }

    private static @NotNull String parseGradients(@NotNull String input) {
        Matcher matcher = GRADIENT.matcher(input);
        if (!matcher.find()) return input;
        StringBuilder out = new StringBuilder(input.length() * 4);
        do {
            int start = Integer.parseInt(matcher.group(1), 16);
            int end = Integer.parseInt(matcher.group(2), 16);
            matcher.appendReplacement(out, Matcher.quoteReplacement(applyGradient(matcher.group(3), start, end)));
        } while (matcher.find());
        matcher.appendTail(out);
        return out.toString();
    }

    private static @NotNull String applyGradient(@NotNull String text, int start, int end) {
        int visible = strip(text).length();
        if (visible <= 1) {
            return sectionHex(start) + text;
        }
        StringBuilder out = new StringBuilder(text.length() * 16);
        StringBuilder formats = new StringBuilder(8);
        int index = 0;
        int last = visible - 1;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '&' && i + 1 < text.length()) {
                char code = Character.toLowerCase(text.charAt(i + 1));
                if (LEGACY_CODES.indexOf(code) >= 0) {
                    if (code >= 'k' && code <= 'o') {
                        formats.append(SECTION).append(code);
                    } else {
                        formats.setLength(0);
                    }
                    i++;
                    continue;
                }
            }
            out.append(sectionHex(interpolate(start, end, index, last))).append(formats).append(c);
            index++;
        }
        return out.toString();
    }

    private static int interpolate(int start, int end, int index, int last) {
        double ratio = (double) index / last;
        int r = channel(start >> 16, end >> 16, ratio);
        int g = channel(start >> 8, end >> 8, ratio);
        int b = channel(start, end, ratio);
        return (r << 16) | (g << 8) | b;
    }

    private static int channel(int start, int end, double ratio) {
        int s = start & 0xFF;
        int e = end & 0xFF;
        return (int) Math.round(s + (e - s) * ratio);
    }

    private static @NotNull String parseRgb(@NotNull String input) {
        Matcher matcher = RGB.matcher(input);
        if (!matcher.find()) return input;
        StringBuilder out = new StringBuilder(input.length() * 2);
        do {
            int r = clamp(Integer.parseInt(matcher.group(1)));
            int g = clamp(Integer.parseInt(matcher.group(2)));
            int b = clamp(Integer.parseInt(matcher.group(3)));
            matcher.appendReplacement(out, Matcher.quoteReplacement(sectionHex((r << 16) | (g << 8) | b)));
        } while (matcher.find());
        matcher.appendTail(out);
        return out.toString();
    }

    private static @NotNull String parseHex(@NotNull String input) {
        Matcher matcher = HEX.matcher(input);
        if (!matcher.find()) return input;
        StringBuilder out = new StringBuilder(input.length() * 2);
        do {
            matcher.appendReplacement(out, Matcher.quoteReplacement(sectionHex(Integer.parseInt(matcher.group(1), 16))));
        } while (matcher.find());
        matcher.appendTail(out);
        return out.toString();
    }

    private static @NotNull String parseLegacy(@NotNull String input) {
        if (input.indexOf('&') < 0) return input;
        char[] chars = input.toCharArray();
        for (int i = 0; i < chars.length - 1; i++) {
            if (chars[i] != '&') continue;
            char code = Character.toLowerCase(chars[i + 1]);
            if (LEGACY_CODES.indexOf(code) >= 0) {
                chars[i] = SECTION;
                chars[i + 1] = code;
                i++;
            }
        }
        return new String(chars);
    }

    private static @NotNull String trailingStyleTags(@NotNull String legacy) {
        int end = legacy.length();
        int start = end;
        while (start >= 2 && legacy.charAt(start - 2) == SECTION) {
            if (start >= 14 && legacy.charAt(start - 14) == SECTION && legacy.charAt(start - 13) == 'x'
                    && SECTION_HEX.matcher(legacy.substring(start - 14, start)).matches()) {
                start -= 14;
            } else if (LEGACY_CODES.indexOf(Character.toLowerCase(legacy.charAt(start - 1))) >= 0) {
                start -= 2;
            } else {
                break;
            }
        }
        if (start == end) return "";
        StringBuilder tags = new StringBuilder(32);
        int i = start;
        while (i < end) {
            if (legacy.charAt(i + 1) == 'x') {
                tags.append("<#");
                for (int j = i + 3; j < i + 14; j += 2) {
                    tags.append(legacy.charAt(j));
                }
                tags.append('>');
                i += 14;
            } else {
                tags.append('<').append(tagName(Character.toLowerCase(legacy.charAt(i + 1)))).append('>');
                i += 2;
            }
        }
        return tags.toString();
    }

    private static @NotNull String tagName(char code) {
        return switch (code) {
            case '0' -> "black";
            case '1' -> "dark_blue";
            case '2' -> "dark_green";
            case '3' -> "dark_aqua";
            case '4' -> "dark_red";
            case '5' -> "dark_purple";
            case '6' -> "gold";
            case '7' -> "gray";
            case '8' -> "dark_gray";
            case '9' -> "blue";
            case 'a' -> "green";
            case 'b' -> "aqua";
            case 'c' -> "red";
            case 'd' -> "light_purple";
            case 'e' -> "yellow";
            case 'f' -> "white";
            case 'k' -> "obfuscated";
            case 'l' -> "bold";
            case 'm' -> "strikethrough";
            case 'n' -> "underlined";
            case 'o' -> "italic";
            default -> "reset";
        };
    }

    private static @NotNull String sectionHex(int rgb) {
        StringBuilder sb = new StringBuilder(14);
        sb.append(SECTION).append('x');
        for (int shift = 20; shift >= 0; shift -= 4) {
            sb.append(SECTION).append(HEX_DIGITS.charAt((rgb >> shift) & 0xF));
        }
        return sb.toString();
    }

    private static char nearestLegacy(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        int best = 0;
        int bestDistance = Integer.MAX_VALUE;
        for (int i = 0; i < PALETTE.length; i++) {
            int pr = (PALETTE[i] >> 16) & 0xFF;
            int pg = (PALETTE[i] >> 8) & 0xFF;
            int pb = PALETTE[i] & 0xFF;
            int distance = (r - pr) * (r - pr) + (g - pg) * (g - pg) + (b - pb) * (b - pb);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = i;
            }
        }
        return HEX_DIGITS.charAt(best);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}

package com.axiom.antivpn.common.color;

import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ColorParser {

    private static final char SECTION_CHAR = '\u00A7';
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern RGB_PATTERN = Pattern.compile("\\{rgb:(\\d{1,3}),(\\d{1,3}),(\\d{1,3})}");
    private static final Pattern GRADIENT_PATTERN = Pattern.compile("\\{gradient:#([A-Fa-f0-9]{6}):#([A-Fa-f0-9]{6})}(.*?)\\{/gradient}", Pattern.DOTALL);

    private ColorParser() {
    }

    public static @NotNull String parse(@NotNull String input) {
        String result = input;
        result = parseGradients(result);
        result = parseRgb(result);
        result = parseHex(result);
        result = parseLegacy(result);
        return result;
    }

    public static @NotNull String strip(@NotNull String input) {
        String result = GRADIENT_PATTERN.matcher(input).replaceAll("$3");
        result = HEX_PATTERN.matcher(result).replaceAll("");
        result = RGB_PATTERN.matcher(result).replaceAll("");
        result = result.replaceAll("[&" + SECTION_CHAR + "][0-9a-fk-or]", "");
        return result;
    }

    private static @NotNull String parseGradients(@NotNull String input) {
        Matcher matcher = GRADIENT_PATTERN.matcher(input);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String startHex = matcher.group(1);
            String endHex = matcher.group(2);
            String text = matcher.group(3);
            String gradientText = applyGradient(text, startHex, endHex);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(gradientText));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static @NotNull String applyGradient(@NotNull String text, @NotNull String startHex, @NotNull String endHex) {
        int sr = Integer.parseInt(startHex.substring(0, 2), 16);
        int sg = Integer.parseInt(startHex.substring(2, 4), 16);
        int sb = Integer.parseInt(startHex.substring(4, 6), 16);
        int er = Integer.parseInt(endHex.substring(0, 2), 16);
        int eg = Integer.parseInt(endHex.substring(2, 4), 16);
        int eb = Integer.parseInt(endHex.substring(4, 6), 16);

        String stripped = strip(text);
        int length = stripped.length();
        if (length <= 1) {
            return hexToMinecraft(startHex) + text;
        }

        StringBuilder result = new StringBuilder(text.length() * 14);
        int charIndex = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '&' && i + 1 < text.length()) {
                char next = text.charAt(i + 1);
                if ("0123456789abcdefklmnor".indexOf(next) != -1) {
                    i++;
                    continue;
                }
            }

            float ratio = (float) charIndex / (length - 1);
            int r = Math.round(sr + (er - sr) * ratio);
            int g = Math.round(sg + (eg - sg) * ratio);
            int b2 = Math.round(sb + (eb - sb) * ratio);
            result.append(hexToMinecraft(String.format("%02x%02x%02x", r, g, b2)));
            result.append(c);
            charIndex++;
        }
        return result.toString();
    }

    private static @NotNull String parseRgb(@NotNull String input) {
        Matcher matcher = RGB_PATTERN.matcher(input);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            int r = clamp(Integer.parseInt(matcher.group(1)), 0, 255);
            int g = clamp(Integer.parseInt(matcher.group(2)), 0, 255);
            int b = clamp(Integer.parseInt(matcher.group(3)), 0, 255);
            String hex = String.format("%02x%02x%02x", r, g, b);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(hexToMinecraft(hex)));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static @NotNull String parseHex(@NotNull String input) {
        Matcher matcher = HEX_PATTERN.matcher(input);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(sb, Matcher.quoteReplacement(hexToMinecraft(matcher.group(1))));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static @NotNull String parseLegacy(@NotNull String input) {
        char[] chars = input.toCharArray();
        for (int i = 0; i < chars.length - 1; i++) {
            if (chars[i] == '&' && "0123456789abcdefklmnorABCDEFKLMNOR".indexOf(chars[i + 1]) != -1) {
                chars[i] = SECTION_CHAR;
                chars[i + 1] = Character.toLowerCase(chars[i + 1]);
            }
        }
        return new String(chars);
    }

    private static @NotNull String hexToMinecraft(@NotNull String hex) {
        StringBuilder sb = new StringBuilder(14);
        sb.append(SECTION_CHAR).append('x');
        for (char c : hex.toCharArray()) {
            sb.append(SECTION_CHAR).append(c);
        }
        return sb.toString();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}

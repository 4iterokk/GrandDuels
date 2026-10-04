package org.chiterok.grandDuels.utils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.bukkit.ChatColor;

/**
 * Utility for translating legacy & color codes and hex color codes.
 * Supports both &#RRGGBB and <#RRGGBB> formats.
 */
public class ColorUtil {

    private static final Pattern HEX_PATTERN     = Pattern.compile("&#([a-fA-F0-9]{6})");
    private static final Pattern NEW_HEX_PATTERN = Pattern.compile("<#([a-fA-F0-9]{6})>");

    private ColorUtil() {}

    public static String colorize(String message) {
        if (message == null) return null;
        message = translateNewHexColors(message);
        message = translateHexColors(message);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static List<String> colorize(List<String> messages) {
        return messages.stream()
                .map(ColorUtil::colorize)
                .collect(Collectors.toList());
    }

    private static String translateHexColors(String message) {
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String hexCode = matcher.group(1).toLowerCase();
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hexCode.toCharArray()) replacement.append('§').append(c);
            matcher.appendReplacement(buffer, replacement.toString());
        }
        return matcher.appendTail(buffer).toString();
    }

    private static String translateNewHexColors(String message) {
        Matcher matcher = NEW_HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String hexCode = matcher.group(1);
            String replacement = "§x" + hexCode.chars()
                    .<CharSequence>mapToObj(c -> "§" + (char) c)
                    .collect(Collectors.joining());
            matcher.appendReplacement(buffer, replacement);
        }
        return matcher.appendTail(buffer).toString();
    }
}
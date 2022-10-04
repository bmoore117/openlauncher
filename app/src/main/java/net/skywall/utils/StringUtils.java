package net.skywall.utils;

import java.util.Arrays;
import java.util.stream.Collectors;

public class StringUtils {

    public static String capitalizeFirst(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        return Arrays
                .stream(text.split("\\s+"))
                .map(word -> word.isEmpty()
                        ? word
                        : Character.toTitleCase(word.charAt(0)) + word
                        .substring(1))
                .collect(Collectors.joining(" "));
    }
}

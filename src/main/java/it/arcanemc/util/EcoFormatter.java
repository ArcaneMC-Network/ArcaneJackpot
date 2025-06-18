package it.arcanemc.util;

import lombok.Getter;

import java.util.Map;

@Getter
public class EcoFormatter {
    public static String minimal(double value, Map<?, ?> map, int decimal){
        String format = "%." + decimal + "f%s";
        if (value >= 1_000_000_000) {
            return String.format(format, value / 1_000_000_000, map.get("b"));
        } else if (value >= 1_000_000) {
            return String.format(format, value / 1_000_000, map.get("m"));
        } else if (value >= 1_000) {
            return String.format(format, value / 1_000, map.get("s"));
        } else {
            return String.format(format.substring(0, format.length()-2), value);
        }
    }
}

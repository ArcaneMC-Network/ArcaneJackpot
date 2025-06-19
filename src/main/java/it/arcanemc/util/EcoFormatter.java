package it.arcanemc.util;

import lombok.Getter;

import java.util.Map;

@Getter
public class EcoFormatter {
    public static String minimal(double value, Map<?, ?> map, int decimals) {
        double formattedValue;
        String suffix = "";

        if (value >= 1_000_000_000) {
            formattedValue = value / 1_000_000_000;
            suffix = (String) map.get("b");
        } else if (value >= 1_000_000) {
            formattedValue = value / 1_000_000;
            suffix = (String) map.get("m");
        } else if (value >= 1_000) {
            formattedValue = value / 1_000;
            suffix = (String) map.get("k");
        } else {
            return (value == (long) value) ? String.valueOf((long) value) : String.format("%." + decimals + "f", value);
        }

        if (formattedValue == (long) formattedValue) {
            return String.format("%d%s", (long) formattedValue, suffix);
        } else {
            return String.format("%." + decimals + "f%s", formattedValue, suffix);
        }
    }

}

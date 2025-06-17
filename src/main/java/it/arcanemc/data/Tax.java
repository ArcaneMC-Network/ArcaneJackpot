package it.arcanemc.data;

import lombok.Getter;

@Getter
public class Tax {
    private final String name;
    private final double percentage;
    private final boolean isEnabled;

    public Tax(String name, double percentage, boolean isEnabled) {
        this.name = name;
        this.percentage = percentage;
        this.isEnabled = isEnabled;
    }

    public double calculate(double amount) {
        if (!isEnabled) {
            return 0.0;
        }
        return amount * (percentage / 100);
    }
}

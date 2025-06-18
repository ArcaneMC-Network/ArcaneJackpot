package it.arcanemc.task;

import it.arcanemc.ArcanePlugin;
import it.arcanemc.util.Colors;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import com.connorlinfoot.titleapi.TitleAPI;

import java.text.DecimalFormat;

public class JackpotWinnerTitleTask extends BukkitRunnable {

    private Player winner;
    private double jackpotAmount;

    private int steps;
    private int interval;
    private int fadeIn;
    private int fadeOut;
    private ArcanePlugin pl;
    private int currentStep = 0;

    private final DecimalFormat df = new DecimalFormat("#.#");


    public JackpotWinnerTitleTask(ArcanePlugin pl, Player winner, double jackpotAmount) {
        ConfigurationSection config = pl.getConfigurationManager().get("config")
                .getConfigurationSection("winner.title");
        this.pl = pl;
        this.winner = winner;
        this.jackpotAmount = jackpotAmount;
        this.steps = config.getInt("steps");
        this.interval = config.getInt("interval");
        this.fadeIn = config.getInt("fade-in");
        this.fadeOut = config.getInt("fade-out");
    }

    public void start(Player player, double jackpotAmount) {
        this.winner = player;
        this.jackpotAmount = jackpotAmount;
        this.currentStep = 0;

        this.runTaskTimer(pl, 0L, interval);
    }

    public void set(ArcanePlugin pl){
        ConfigurationSection config = pl.getConfigurationManager().get("config")
                .getConfigurationSection("winner.title");
        this.pl = pl;
        this.steps = config.getInt("steps");
        this.interval = config.getInt("interval");
        this.fadeIn = config.getInt("fade-in");
        this.fadeOut = config.getInt("fade-out");
    }

    @Override
    public void run() {
        ConfigurationSection config = pl.getConfigurationManager().get("config")
                .getConfigurationSection("winner.title");

        if (currentStep > steps) {
            String finalTitle = Colors.translate(config.getString("title"));
            String finalSubtitle = Colors.translate(config.getString("title")
                    .replace("{money}", df.format(jackpotAmount)));
            TitleAPI.clearTitle(winner);
            TitleAPI.sendTitle(winner, 0, 0, fadeOut, finalTitle, finalSubtitle);
            cancel();
            return;
        }

        double displayAmount = jackpotAmount * currentStep / steps;

        String title = Colors.translate(config.getString("title"));
        String subtitle = Colors.translate(config.getString("subtitle")
                .replace("{money}", df.format(displayAmount)));

        int actualFadeIn = (currentStep < fadeIn) ? fadeIn : 0;

        TitleAPI.clearTitle(winner);
        TitleAPI.sendTitle(winner, actualFadeIn, interval, 0, title, subtitle);

        currentStep++;
    }
}


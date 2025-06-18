package it.arcanemc.manager;

import it.arcanemc.ArcanePlugin;
import it.arcanemc.data.JackpotTime;
import it.arcanemc.data.Tax;
import it.arcanemc.task.JackpotWinnerTitleTask;
import it.arcanemc.util.EcoFormatter;
import it.arcanemc.util.Msg;
import it.arcanemc.util.Timer;
import it.arcanemc.util.exception.EconomyNotFoundException;
import lombok.Getter;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.DecimalFormat;
import java.util.*;

@Getter
public class JackpotManager {
    private final ArcanePlugin pl;
    private final Economy economy;
    private JackpotWinnerTitleTask winnerTitle;

    private double ticketCost;
    private Tax tax;
    private JackpotTime jackpotTime;
    private Map<UUID, Integer> tickets;
    private UUID winner;

    public JackpotManager(ArcanePlugin pl) {
        this.pl = pl;
        RegisteredServiceProvider<Economy> rsp = this.pl.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null){
            throw new EconomyNotFoundException();
        }
        this.economy = rsp.getProvider();
        this.winnerTitle = new JackpotWinnerTitleTask(pl, null, 0.0);
        initialize();
    }

    public void initialize() {
        this.ticketCost = getTicketCost();

        FileConfiguration config = pl.getConfigurationManager().get("config");
        ConfigurationSection timerSection = config.getConfigurationSection("timer");

        this.winner = null;
        this.jackpotTime = new JackpotTime(
                Timer.convertVerbose(timerSection.getString("reminder")),
                Timer.convertVerbose(timerSection.getString("phases.purchasing")),
                Timer.convertVerbose(timerSection.getString("phases.winning")),
                Timer.convertVerbose(timerSection.getString("phases.sleeping"))
        );

        this.tax = new Tax(
                config.getString("tax.permission"),
                config.getDouble("tax.percentage"),
                config.getBoolean("tax.enabled")
        );
        this.tickets = new HashMap<>();
    }

    public void reload() {
        this.reset();
        this.pl.getConfigurationManager().reload();
        this.winnerTitle.set(pl);
        initialize();
    }

    public void sendStart(){
        Msg.all(
                pl.getConfigurationManager().get("message").getString("jackpot.start")
                        .replace(
                                "{money}",
                                this.getFormatted(this.ticketCost)
                        )
        );
    }

    public void sendReminder() {
        if (jackpotTime.getTimer() % jackpotTime.getReminder() == 0) {
            Msg.all(
                    pl.getConfigurationManager().get("message").getString("jackpot.purchasing")
                            .replace(
                                    "{time}",
                                    Timer.getVerbose(
                                            jackpotTime.remainingToWinning()
                                    )
                            )
            );
        }
    }

    public void sendWinner(OfflinePlayer offlinePlayer, double payout) {
        String message;

        if (!offlinePlayer.getPlayer().hasPermission(tax.getName())){
            message = pl.getConfigurationManager().get("message").getString("jackpot.winner-no-vip")
                    .replace("{name}", offlinePlayer.getName())
                    .replace("{money}", this.getFormatted(payout))
                    .replace("{tax}", String.valueOf(tax.getPercentage()));

        } else {
            message = pl.getConfigurationManager().get("message").getString("jackpot.winner-vip")
                    .replace("{name}", offlinePlayer.getName())
                    .replace("{money}", this.getFormatted(payout));
        }

        if (offlinePlayer.isOnline()){
            if (pl.getConfigurationManager().get("config").getBoolean("winner.title.enabled")) {
                new JackpotWinnerTitleTask(pl, offlinePlayer.getPlayer(), payout).start();
            }
        }
        Msg.all(message);
    }

    public void sendNotEnoughPlayers() {
        Msg.all(
                pl.getConfigurationManager().get("message").getString("error.not-enough-players")
        );
    }

    public OfflinePlayer pickWinner() {
        if (getTotalTickets() == 0) {
            pl.getLogger().severe("No tickets available to pick a winner.");
            return null;
        }
        int random = new Random().nextInt(getTotalTickets());

        for (Map.Entry<UUID, Integer> entry : tickets.entrySet()) {
            random -= entry.getValue();
            if (random < 0) {
                return pl.getServer().getOfflinePlayer(entry.getKey());
            }
        }

        return null; // This should never happen if the tickets are managed correctly
    }

    public void payWinner(){
        if (winner == null) {
            pl.getLogger().severe("No winner selected for the jackpot. Cannot pay out.");
            return;
        }
        OfflinePlayer p = pl.getServer().getOfflinePlayer(winner);
        double payout = this.getInitialPayout();
        boolean isTaxed = !p.getPlayer().hasPermission(tax.getName());
        if (isTaxed) {
            payout = tax.calculate(payout);
        }
        if (economy.depositPlayer(p, payout).transactionSuccess()) {
            sendWinner(p, payout);
        } else {
            pl.getLogger().severe("Failed to pay jackpot winner: " + p.getName() + ". Please check your economy plugin.");
        }
    }

    public void update() {
        if (jackpotTime.getState() == JackpotState.SLEEPING) {
            if (jackpotTime.remainingToPurchasing() == 0L) {
                jackpotTime.setState(JackpotState.PURCHASING);
                sendStart();
            }
        } else if (jackpotTime.getState() == JackpotState.PURCHASING) {
            if (jackpotTime.remainingToWinning() == 0L){
                jackpotTime.setState(JackpotState.WINNING);
                if (getTotalPlayers() < getMinimumPlayer()) {
                    refund();
                } else {
                    winner = pickWinner().getUniqueId();
                    payWinner();
                }
            } else {
                sendReminder();
            }
        } else if (jackpotTime.getState() == JackpotState.WINNING) {
            if (jackpotTime.remainingToSleeping() == 0L){
                reset();
            }
        }
    }

    public void reset() {
        jackpotTime.setTimer(0L);
        jackpotTime.setState(JackpotState.SLEEPING);
        tickets.clear();
        winner = null;
    }

    public void refund(){
        sendNotEnoughPlayers();
        if (tickets.isEmpty()) {
            return;
        }

        for (Map.Entry<UUID, Integer> entry : tickets.entrySet()) {
            OfflinePlayer player = pl.getServer().getOfflinePlayer(entry.getKey());
            int ticketCount = entry.getValue();
            double refundAmount = ticketCount * ticketCost;
            if (economy.depositPlayer(player, refundAmount).transactionSuccess()) {
                Msg.player(player.getPlayer(),
                        pl.getConfigurationManager().get("message").getString("ticket.refund")
                                .replace("{money}", this.getFormatted(refundAmount))
                );
            } else {
                pl.getLogger().severe("Failed to refund player: " + player.getName());
            }
        }
        reset();
    }

    public int getMinimumPlayer() {
        return pl.getConfigurationManager().get("config").getInt("min-players");
    }

    public float getTicketCost() {
        return pl.getConfigurationManager().get("config").getInt("ticket-cost");
    }

    public int getTickets(OfflinePlayer p) {
        return tickets.getOrDefault(p.getUniqueId(), 0);
    }

    public double getTicketPercentage(OfflinePlayer p) {
        int totalTickets = getTotalTickets();
        if (totalTickets == 0) {
            return 0.0;
        }
        return (double) getTickets(p) / totalTickets * 100;
    }

    public int getTotalTickets() {
        return tickets.values().stream().mapToInt(Integer::intValue).sum();
    }

    public int getTotalPlayers() {
        return tickets.size();
    }

    public double getInitialPayout() {
        return getTotalTickets() * ticketCost;
    }

    public boolean isEcoFormat(){
        return pl.getConfigurationManager().get("config").getBoolean("eco-format.enabled");
    }

    public void addTicket(OfflinePlayer p, int amount) {
        if (this.jackpotTime.getState() != JackpotState.PURCHASING) {
            Msg.player(p.getPlayer(),
                    pl.getConfigurationManager().get("message").getString("ticket.no-purchasing-state")
            );
            return;
        }
        double price = ticketCost * amount;
        if (economy.withdrawPlayer(p, price).transactionSuccess()){
            int currentTickets = tickets.getOrDefault(p.getUniqueId(), 0);
            tickets.put(p.getUniqueId(), currentTickets + amount);
            if (currentTickets == 0) {
                Msg.player(p.getPlayer(),
                        pl.getConfigurationManager().get("message").getString("ticket.bought")
                                .replace("{amount}", this.getFormatted(amount))
                                .replace("{money}", this.getFormatted(price))
                );
            } else {
                Msg.player(p.getPlayer(),
                        pl.getConfigurationManager().get("message").getString("ticket.bought-again")
                                .replace("{amount}", this.getFormatted(amount))
                                .replace("{money}", this.getFormatted(price))
                                .replace("{total}", this.getFormatted(currentTickets + amount))
                );
            }

        } else {
            Msg.player(p.getPlayer(),
                    pl.getConfigurationManager().get("message").getString("ticket.no-money")
            );
        }

    }

    public void sendJackpotInfo(OfflinePlayer p) {
        if (this.jackpotTime.getState() == JackpotState.PURCHASING) {
            boolean isTaxed = !p.getPlayer().hasPermission(tax.getName());
            List<String> rawMessages = pl.getConfigurationManager().get("message").getStringList("stats.purchasing");
            String[] messages;
            DecimalFormat df = new DecimalFormat("#.#");
            if (isTaxed) {
                double money = tax.calculate(getInitialPayout());
                double taxPercentage = tax.getPercentage();
                messages = rawMessages.stream()
                        .map(msg -> msg
                                .replace("{money}", this.getFormatted(money))
                                .replace("{tax}", df.format(taxPercentage))
                                .replace("{tickets}", String.valueOf(getTotalTickets()))
                                .replace("{your-tickets}", String.valueOf(getTickets(p)))
                                .replace("{your-tickets-percentage}", df.format(getTicketPercentage(p)))
                                .replace("{time}", Timer.getVerbose(jackpotTime.remainingToWinning()))
                        ).toArray(String[]::new);
            } else {
                double money = getInitialPayout();
                double taxPercentage = 0.0;
                messages = rawMessages.stream()
                        .map(msg -> msg
                                .replace("{money}", this.getFormatted(money))
                                .replace("{tax}", df.format(taxPercentage))
                                .replace("{tickets}", String.valueOf(getTotalTickets()))
                                .replace("{your-tickets}", String.valueOf(getTickets(p)))
                                .replace("{your-tickets-percentage}", df.format(getTicketPercentage(p)))
                                .replace("{time}", Timer.getVerbose(jackpotTime.remainingToWinning()))
                        ).toArray(String[]::new);
            }
            Msg.player(p.getPlayer(), messages);
        } else if (this.jackpotTime.getState() == JackpotState.WINNING) {
            Msg.player(p.getPlayer(),
                    pl.getConfigurationManager().get("message").getString("stats.winning")
                            .replace("{money}", this.getFormatted(getInitialPayout()))
                            .replace("{name}", pl.getServer().getOfflinePlayer(winner).getName())
            );
        } else {
            Msg.player(p.getPlayer(),
                    pl.getConfigurationManager().get("message").getString("stats.sleeping")
                            .replace("{time}", Timer.getVerbose(jackpotTime.remainingToPurchasing()))
            );
        }

    }

    public String getFormatted(double value) {
        if (!isEcoFormat()) {
            return String.valueOf(value);
        }
        ConfigurationSection section = pl.getConfigurationManager().get("config").getConfigurationSection("eco-format");
        int decimal = section.getInt("decimal");
        Map<String, Object> map = section.getConfigurationSection("values").getValues(false);
        return EcoFormatter.minimal(value, map, decimal);
    }

    public void run(){
        new BukkitRunnable() {
            @Override
            public void run() {
                jackpotTime.addTime(1000L);
                update();
            }
        }.runTaskTimerAsynchronously(pl, 0L, 20L);
    }
}

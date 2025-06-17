package it.arcanemc.command;

import dev.rollczi.litecommands.annotations.argument.Arg;
import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.execute.Execute;
import dev.rollczi.litecommands.annotations.permission.Permission;
import it.arcanemc.manager.JackpotManager;
import it.arcanemc.util.Colors;
import it.arcanemc.util.Msg;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Command(name = "jackpot")
public class JackpotCommand {
    private final JackpotManager jackpotManager;

    public JackpotCommand(JackpotManager jackpotManager) {
        this.jackpotManager = jackpotManager;
    }

    @Execute
    void executeJackpot(@Context Player player) {
        jackpotManager.sendJackpotInfo(player);
    }

    @Execute(name = "reload")
    @Permission("arcanejackpot.admin.reload")
    void executeJackpotReload(@Context CommandSender commandSender) {
        try {
            jackpotManager.reload();
            Msg.sender(commandSender, jackpotManager.getPl().getConfigurationManager().get("message").getString("admin.reload.success"));
        } catch (Exception e){
            Msg.sender(commandSender, jackpotManager.getPl().getConfigurationManager().get("message").getString("admin.reload.deny"));
            e.printStackTrace();
        }
    }

    @Execute(name="buy")
    void executeFPassShow(
            @Context Player player,
            @Arg Integer amount
    ) {
        jackpotManager.addTicket(player, amount);
    }

}

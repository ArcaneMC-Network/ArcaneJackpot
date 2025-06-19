package it.arcanemc;

import dev.rollczi.litecommands.message.LiteMessages;
import dev.rollczi.litecommands.message.MessageKey;
import it.arcanemc.command.JackpotCommand;
import it.arcanemc.manager.JackpotManager;
import it.arcanemc.util.Colors;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
public final class ArcaneJackpot extends ArcanePlugin {
    @Setter
    private JackpotManager jackpotManager;

    @Override
    public void performInitialization() {
        this.prefix = "ArcaneJackpot";
        this.jackpotManager = new JackpotManager(this);
        this.jackpotManager.run();
    }

    public List<String> getDependencyNames(){
        return new ArrayList<>(
                List.of(
                    "Vault"
                )
        );
    }

    public List<String> getConfigPaths(){
        return new ArrayList<>(
                List.of(
                        "config.yml",
                        "message.yml",
                        "sound.yml"
                )
        );
    }

    public List<Listener> getListeners(){
        return new ArrayList<>(
                List.of(

                )
        );
    }

    public Object[] getCommands() {
        return new Object[]{
                new JackpotCommand(this.jackpotManager)
        };
    }

    @Override
    public Map<MessageKey<?>, String> getCommandMessages() {
        return Map.of(
                LiteMessages.INVALID_USAGE,
                Colors.translate(this.configurationManager.get("message").getString("error.invalid-command"))
        );
    }
}

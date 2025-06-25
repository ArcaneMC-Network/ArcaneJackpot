package it.arcanemc.data;

import it.arcanemc.manager.JackpotState;
import lombok.Getter;
import lombok.Setter;

@Getter
public class JackpotTime {
    private final long reminder, sleepingTime, purchasingTime, winningTime;

    @Setter
    private long timer;

    @Setter
    private JackpotState state;

    public JackpotTime(Long reminder, Long sleepingTime, Long purchasingTime, Long winningTime) {
        this.reminder = reminder;
        this.sleepingTime = sleepingTime;
        this.purchasingTime = purchasingTime + sleepingTime;
        this.winningTime = winningTime + purchasingTime;
        timer = 0L;
        state = JackpotState.SLEEPING;
    }

    public void addTime(Long time) {
        this.timer += time;
    }

    public long remainingToWinning(){
        if (state != JackpotState.PURCHASING){
            return -1L;
        }
        return purchasingTime - timer;
    }

    public long remainingToPurchasing(){
        if (state != JackpotState.SLEEPING){
            return -1L;
        }
        return sleepingTime - timer;
    }

    public long remainingToSleeping(){
        if (state != JackpotState.WINNING){
            return -1L;
        }
        return winningTime - timer;
    }
}

package me.toomuchzelda.teamarenapaper.teamarena.damage;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class KillAssistTracker {

    private final Player player;
    private final HashMap<Player, Double> playerDamageAmounts = new HashMap<>();

    public KillAssistTracker(Player player) {
        this.player = player;
    }

    public void addDamage(Player cause, double damage) {
        playerDamageAmounts.put(cause, damage);
    }

    //reduce all damage amounts done by other players by uniform amount when healing
    public void heal(double amount) {
        //find percentage of new health out of max health
        double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        double oldHealth = player.getHealth();
        double oldRemainderHealth = maxHealth - oldHealth;
        double newHealth = Math.min(oldHealth + amount, maxHealth);
        double newRemainderHealth = maxHealth - newHealth;

        double percent = newRemainderHealth / oldRemainderHealth;

        //multiply all the damage credits they've received by this percent: reduce them uniformly
        var iter = playerDamageAmounts.entrySet().iterator();
        while(iter.hasNext()) {
            Map.Entry<Player, Double> entry = iter.next();

            double newValue = entry.getValue() * percent;
            if(newValue < 0.001) { // this will lead to some discrepancy/gaps but shouldn't be a huge problem
                iter.remove();
            }
            else {
                entry.setValue(newValue);
            }
        }
    }

    public double getAssistAmount(Player player) {
        return playerDamageAmounts.getOrDefault(player, 0d);
    }

    public void clear() {
        playerDamageAmounts.clear();
    }
}

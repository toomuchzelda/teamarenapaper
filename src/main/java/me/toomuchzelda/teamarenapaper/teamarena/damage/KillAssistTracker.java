package me.toomuchzelda.teamarenapaper.teamarena.damage;

import me.toomuchzelda.teamarenapaper.Main;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class KillAssistTracker {

    private final Player player;
    //double is the raw amount of damage done
    // gets divided by 10 in TeamArena
    private final HashMap<Player, Double> playerDamageAmounts = new HashMap<>();

    public KillAssistTracker(Player player) {
        this.player = player;
    }

    public void addDamage(Player cause, double damage) {
        //add this to the previous value if it exists, else just make it the value
        playerDamageAmounts.merge(cause, damage, Double::sum);
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

        //multiply all the damage credits they've received by this percent; reduce them uniformly
        var iter = playerDamageAmounts.entrySet().iterator();
        while(iter.hasNext()) {
            Map.Entry<Player, Double> entry = iter.next();

            double newValue = entry.getValue() * percent;
            if(newValue < 0.05) { // this will lead to some discrepancy/gaps but shouldn't be a huge problem
                iter.remove();
            }
            else {
                entry.setValue(newValue);
            }
        }
        
        if(player.getName().equalsIgnoreCase("EnemyCircle901")) {
            Main.logger().info(playerDamageAmounts.toString());
        }
    }

    public double getAssistAmount(Player player) {
        return playerDamageAmounts.getOrDefault(player, 0d);
    }
    
    public Player getPlayer() {
        return this.player;
    }
    
    public void removeAssist(Player player) {
        playerDamageAmounts.remove(player);
    }
    
    public Iterator<Map.Entry<Player, Double>> getIterator() {
        return playerDamageAmounts.entrySet().iterator();
    }
    
    public void clear() {
        playerDamageAmounts.clear();
    }
}

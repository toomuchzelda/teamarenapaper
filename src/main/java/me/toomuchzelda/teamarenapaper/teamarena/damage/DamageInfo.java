package me.toomuchzelda.teamarenapaper.teamarena.damage;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.core.EntityUtils;
import me.toomuchzelda.teamarenapaper.core.MathUtils;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.List;

public class DamageInfo {
    public DamageType damageType;
    public double damage;
    public @Nullable
    Component damager;
    public int time;

    public DamageInfo(DamageType type, double damage, @Nullable Entity damager, int time) {
        this.damageType = type;
        this.damage = damage;
        if(damager != null)
            this.damager = EntityUtils.getName(damager);
        else
            this.damager = null;
        this.time = time;
    }

    public static void sendDamageLog(Player player, boolean clear) {

        Component text = Component.text("Here's how you died (L):").color(NamedTextColor.DARK_PURPLE).append(Component.newline());

        PlayerInfo pinfo = Main.getPlayerInfo(player);
        List<DamageInfo> list = pinfo.getDamageReceivedLog();
        var iter = list.iterator();
        while(iter.hasNext()) {
            DamageInfo dinfo = iter.next();
            text = text.append(Component.text(MathUtils.round((dinfo.damage / 2), 2) + " ♥").color(TextColor.color(217, 45, 2)));
            text = text.append(Component.text(", Cause: " + dinfo.damageType.getName()).color(TextColor.color(255, 247, 0)));
            if(dinfo.damager != null) {
                text = text.append(Component.text(", Damager: ").color(TextColor.color(6, 122, 0)).append(dinfo.damager));
            }
            float timeAgo = TeamArena.getGameTick() - dinfo.time;
            timeAgo /= 20;
            text = text.append(Component.text(", " + timeAgo + "s ago").color(NamedTextColor.WHITE));

            if(iter.hasNext())
                text = text.append(Component.newline());

            if(clear)
                iter.remove();
        }

        Bukkit.broadcastMessage("size of list: " + list.size());

        player.sendMessage(text);

        /*if(clear) {
            pinfo.clearDamageInfos();
        }*/
    }
}

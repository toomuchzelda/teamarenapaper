package me.toomuchzelda.teamarenapaper.teamarena.damage;

import me.toomuchzelda.teamarenapaper.core.MathUtils;
import me.toomuchzelda.teamarenapaper.core.PacketHologram;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Collection;

public class DamageIndicatorHologram extends PacketHologram {

    public int age;
    private final Vector horizontalDirection;
    private final Location startLoc;

    public DamageIndicatorHologram(Location spawnLoc, Collection<Player> viewers, Component text) {
        super(spawnLoc, text);
        this.setViewers(viewers);
        this.age = 0;
        startLoc = location.clone();

        horizontalDirection = new Vector(MathUtils.randomRange(-0.6d, 0.6d), 0, MathUtils.randomRange(-0.6d, 0.6d)).normalize();
    }

    public void tick() {

        double yPos = Math.sin((double) age / 3);
        double horiPercent = ((double) age * 0.03);
        double x = horizontalDirection.getX() * horiPercent;
        double z = horizontalDirection.getZ() * horiPercent;

        this.move(startLoc.clone().add(x, yPos, z));

        age++;
    }

}

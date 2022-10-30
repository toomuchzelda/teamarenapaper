package me.toomuchzelda.teamarenapaper.teamarena.damage;

import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketHologram;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.LinkedHashSet;

public class DamageIndicatorHologram extends PacketHologram {

    public int age;
    private final Vector horizontalDirection;
    private final Location startLoc;

    public DamageIndicatorHologram(Location spawnLoc, LinkedHashSet<Player> viewers, Component text) {
        super(spawnLoc, viewers, /*player -> (TeamArena.getGameTick() % 10) / 5 == 1*/ null, text);

        this.age = 0;
        startLoc = location.clone();

        horizontalDirection = new Vector(MathUtils.randomRange(-0.6d, 0.6d), 0, MathUtils.randomRange(-0.6d, 0.6d)).normalize();
    }

	@Override
    public void tick() {
        double yPos = Math.sin((double) age / 4);
        double horiPercent = ((double) age * 0.03);
        double x = horizontalDirection.getX() * horiPercent;
        double z = horizontalDirection.getZ() * horiPercent;

        this.move(startLoc.clone().add(x, yPos, z));

        if(age++ >= 15) {
			this.remove();
		}
    }
}

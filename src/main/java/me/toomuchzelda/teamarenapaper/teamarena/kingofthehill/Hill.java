package me.toomuchzelda.teamarenapaper.teamarena.kingofthehill;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import me.toomuchzelda.teamarenapaper.utils.PacketSender;
import me.toomuchzelda.teamarenapaper.utils.ParticleUtils;
import me.toomuchzelda.teamarenapaper.utils.RealHologram;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

public class Hill {

    public static final int VIEW_DISTANCE = 35;

    private final String name;
    private final BoundingBox border;
    //time in seconds
    private final int time;

    private RealHologram hologram;
	private final World world;

    public Hill(String name, BoundingBox border, int time, World world) {
        this.name = name;
        this.border = border;
        this.time = time;
		this.world = world;

        hologram = new RealHologram(border.getCenter().toLocation(world), RealHologram.Alignment.TOP, Component.text("Hill: " + name));
    }

    public void playParticles(Color... colors) {
        //ParticleBuilder doesn't support coloured SPELL_MOB Particles

        MathUtils.shuffleArray(colors);

        //draw x lines
        Location location = border.getMin().toLocation(world);
		Location otherSide = location.clone();
        double xLength = border.getWidthX();
        double zLength = border.getWidthZ();

        int red;
        int green;
        int blue;

		PacketSender sender = PacketSender.getDefault((int) (xLength + zLength + 1) * 2);

        for(int x = 0; x <= xLength; x++)
        {
            //get RGB as 0-255 and convert to 0-1
            int index = x % colors.length;
            red = colors[index].getRed();
            green = colors[index].getGreen();
            blue = colors[index].getBlue();

			Color bukkitColor = Color.fromRGB(red, green, blue);
            location.setX(border.getMinX() + x);
			otherSide.set(location.getX(), location.getY(), location.getZ() + zLength);

			for(Player p : Bukkit.getOnlinePlayers()) {
				int num = Main.getPlayerInfo(p).getPreference(Preferences.KOTH_HILL_PARTICLES);
				if (num == 0)
					continue;

				if(TeamArena.getGameTick() % (11 - num) == 0) {
					// Also does distance check
					ParticleUtils.batchParticles(p, sender, Particle.ENTITY_EFFECT, bukkitColor, location,
						VIEW_DISTANCE, 0, 0, 0, 0, 1f, true);

					ParticleUtils.batchParticles(p, sender, Particle.ENTITY_EFFECT, bukkitColor, otherSide,
						VIEW_DISTANCE, 0, 0, 0, 0, 1f, true);
				}
			}
        }

        //draw z lines
        // reset location vector to start point X
        location.setX(border.getMinX());
        for(int z = 0; z <= zLength; z += 1)
        {
            int index = z % colors.length;
            red = colors[index].getRed();
            green = colors[index].getGreen();
            blue = colors[index].getBlue();

			Color bukkitColor = Color.fromRGB(red, green, blue);
            location.setZ(border.getMinZ() + z);
			otherSide.set(location.getX() + xLength, location.getY(), location.getZ());

            for(Player p : Bukkit.getOnlinePlayers()) {
				int freq = Main.getPlayerInfo(p).getPreference(Preferences.KOTH_HILL_PARTICLES);
				if (freq == 0)
					continue;

				if(TeamArena.getGameTick() % (11 - freq) == 0) {
					ParticleUtils.batchParticles(p, sender, Particle.ENTITY_EFFECT, bukkitColor, location,
						VIEW_DISTANCE, 0, 0, 0, 0, 1f, true);

					ParticleUtils.batchParticles(p, sender, Particle.ENTITY_EFFECT, bukkitColor, otherSide,
						VIEW_DISTANCE, 0, 0, 0, 0, 1f, true);
				}
			}
		}

		sender.flush();
    }

    public void setHologram(Component... text) {
        hologram.setText(text);
    }

    public RealHologram getHologram() {
        return hologram;
    }

    public String getName() {
        return name;
    }

    public BoundingBox getBorder() {
        return border;
    }

    public int getHillTime() {
        return time;
    }

    public int getTime() {
        return time;
    }
}

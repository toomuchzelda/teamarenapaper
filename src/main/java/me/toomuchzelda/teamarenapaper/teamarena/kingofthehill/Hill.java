package me.toomuchzelda.teamarenapaper.teamarena.kingofthehill;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import me.toomuchzelda.teamarenapaper.utils.RealHologram;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class Hill {

    public static final int VIEW_DISTANCE = 35;

    private final String name;
    private final BoundingBox border;
    //time in seconds
    private final int time;

    private RealHologram hologram;

    public Hill(String name, BoundingBox border, int time, World world) {
        this.name = name;
        this.border = border;
        this.time = time;

        hologram = new RealHologram(border.getCenter().toLocation(world), RealHologram.Alignment.TOP, Component.text("Hill: " + name));
    }

    public void playParticles(Color... colors) {
        //ParticleBuilder doesn't support coloured SPELL_MOB Particles

        //https://www.spigotmc.org/wiki/colored-particles/

        MathUtils.shuffleArray(colors);

        //draw x lines
        Vector location = border.getMin();
        double xLength = border.getWidthX();
        double zLength = border.getWidthZ();

        double red;
        double green;
        double blue;

        for(int x = 0; x <= xLength; x++)
        {
            //get RGB as 0-255 and convert to 0-1
            int index = x % colors.length;
            red = colors[index].getRed();
            green = colors[index].getGreen();
            blue = colors[index].getBlue();
            red /= 255;
            green /= 255;
            blue /= 255;
            //apparently must not be 0 for colours to work
            if(red == 0)
                red = 0.0001;

            location.setX(border.getMinX() + x);

            for(Player p : Bukkit.getOnlinePlayers()) {
                //if player is within 35 blocks
                if(p.getLocation().toVector().distanceSquared(border.getCenter()) < VIEW_DISTANCE * VIEW_DISTANCE) {

                    int num = Main.getPlayerInfo(p).getPreference(Preferences.KOTH_HILL_PARTICLES);
					if (num == 0)
						continue;

                    if(TeamArena.getGameTick() % (11 - num) == 0) {
                        p.spawnParticle(Particle.SPELL_MOB, location.getX(), location.getY(),
                                location.getZ(), 0, red, green, blue, 1);

                        p.spawnParticle(Particle.SPELL_MOB, location.getX(), location.getY(),
                                location.getZ() + zLength, 0, red, green, blue, 1);
                    }
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
            red /= 255;
            green /= 255;
            blue /= 255;
            //apparently must not be 0 for colours to work
            if(red == 0)
                red = 0.0001;

            location.setZ(border.getMinZ() + z);

            for(Player p : Bukkit.getOnlinePlayers()) {
				int freq = Main.getPlayerInfo(p).getPreference(Preferences.KOTH_HILL_PARTICLES);
				if (freq == 0)
					continue;
                //if player is within 35 blocks
                if(p.getLocation().toVector().distanceSquared(border.getCenter()) < VIEW_DISTANCE * VIEW_DISTANCE) {


                    if(TeamArena.getGameTick() % (11 - freq) == 0) {
                        p.spawnParticle(Particle.SPELL_MOB, location.getX(), location.getY(),
                                location.getZ(), 0, red, green, blue, 1);

                        p.spawnParticle(Particle.SPELL_MOB, location.getX() + xLength, location.getY(),
                                location.getZ(), 0, red, green, blue, 1);
                    }
                }
            }
        }
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

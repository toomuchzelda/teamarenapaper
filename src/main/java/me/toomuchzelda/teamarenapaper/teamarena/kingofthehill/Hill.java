package me.toomuchzelda.teamarenapaper.teamarena.kingofthehill;

import com.comphenix.protocol.events.PacketContainer;
import com.destroystokyo.paper.ClientOption;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import me.toomuchzelda.teamarenapaper.utils.PacketSender;
import me.toomuchzelda.teamarenapaper.utils.ParticleUtils;
import me.toomuchzelda.teamarenapaper.utils.RealHologram;
import net.kyori.adventure.text.Component;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
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
		if (TeamArena.getGameTick() % 5 != 0) // minimum particle interval
			return;

        //ParticleBuilder doesn't support coloured SPELL_MOB Particles

        MathUtils.shuffleArray(colors);

        //draw x lines
        Location location = border.getMin().toLocation(world);
		Location otherSide = location.clone();
		int minX = (int) border.getMinX();
		int minY = (int) border.getMinY();
		int minZ = (int) border.getMinZ();
        int xLength = (int) border.getWidthX();
        int zLength = (int) border.getWidthZ();

        int red;
        int green;
        int blue;

		int listSize = (xLength + 1 + zLength + 1);
		PacketSender sender = PacketSender.getDefault(listSize * 2);

		// array of packets
		PacketContainer[] particles = new PacketContainer[listSize * 2];
		int i = 0;

		// draw x lines
        for(int x = 0; x <= xLength; x++)
        {
            int index = x % colors.length;

			particles[i * 2] = ParticleUtils.batchParticles(Particle.ENTITY_EFFECT, colors[index],
				minX + x, minY, minZ,
				0, 0, 0, 0, 1, true);
			particles[i * 2 + 1] = ParticleUtils.batchParticles(Particle.ENTITY_EFFECT, colors[index],
				minX + x, minY, minZ + zLength,
				0, 0, 0, 0, 1, true);

			i++;
        }

        //draw z lines
        for(int z = 0; z <= zLength; z += 1)
        {
            int index = z % colors.length;

			particles[i * 2] = ParticleUtils.batchParticles(Particle.ENTITY_EFFECT, colors[index],
				minX, minY, minZ + z,
				0, 0, 0, 0, 1, true);
			particles[i * 2 + 1] = ParticleUtils.batchParticles(Particle.ENTITY_EFFECT, colors[index],
				minX + xLength, minY, minZ + z,
				0, 0, 0, 0, 1, true);
			i++;
		}

		boolean isSecondTick = TeamArena.getGameTick() % 10 == 0;
		for (Player player : Bukkit.getOnlinePlayers()) {
			ClientOption.ParticleVisibility particleVisibility = player.getClientOption(ClientOption.PARTICLE_VISIBILITY);
			if (particleVisibility == ClientOption.ParticleVisibility.MINIMAL)
				continue;
			if (particleVisibility == ClientOption.ParticleVisibility.DECREASED && !isSecondTick)
				continue;

			Location eyeLocation = player.getEyeLocation();
			Location particleLocation = eyeLocation.clone();

			for (PacketContainer packet : particles) {
				ClientboundLevelParticlesPacket nmsPacket = (ClientboundLevelParticlesPacket) packet.getHandle();
				if (particleLocation.set(nmsPacket.getX(), minY, nmsPacket.getZ()).distanceSquared(eyeLocation) <= VIEW_DISTANCE * VIEW_DISTANCE) {
					sender.enqueue(player, packet);
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

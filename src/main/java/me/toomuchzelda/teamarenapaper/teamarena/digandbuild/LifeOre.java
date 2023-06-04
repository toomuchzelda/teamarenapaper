package me.toomuchzelda.teamarenapaper.teamarena.digandbuild;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.utils.BlockCoords;
import me.toomuchzelda.teamarenapaper.utils.EntityUtils;
import me.toomuchzelda.teamarenapaper.utils.RealHologram;
import me.toomuchzelda.teamarenapaper.utils.TextColors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LifeOre
{
	/** Number of times ore must be broken to disqualify the team */
	private static final int HEALTH = 12;

	final TeamArenaTeam owningTeam;
	final BlockCoords coords;
	final Location coordsAsLoc;

	private int health;

	private final double protectionRadius;
	final double protectionRadiusSqr;

	private final Component[] healthComponents; // Pre generated components of the ore's health.

	private final PointMarker hologram;

	private final Set<Player> currentMiners;

	LifeOre(TeamArenaTeam owningTeam, Material mat, BlockCoords coords, double protectionRadius, World world) {
		this.owningTeam = owningTeam;
		this.coords = coords;
		this.coordsAsLoc = coords.toLocation(world);

		this.health = HEALTH;

		this.protectionRadius = protectionRadius;
		this.protectionRadiusSqr = protectionRadius * protectionRadius;

		// Pre generate the health hologram texts
		this.healthComponents = new Component[this.health + 1];
		for (int i = 0; i < this.healthComponents.length; i++) {
			this.healthComponents[i] = createHealthComponent(i);
		}

		Component displayText = getTextDisplayComponent();

		this.hologram = new PointMarker(this.coordsAsLoc.clone().add(0.5d, 1.5d, 0.5d), displayText,
			this.owningTeam.getColour(), Material.ALLAY_SPAWN_EGG);

		world.setBlockData(coords.x(), coords.y(), coords.z(), mat.createBlockData());

		this.currentMiners = new HashSet<>();
	}

	void addMiner(Player miner) {
		if (Main.getPlayerInfo(miner).team != this.owningTeam)
			this.currentMiners.add(miner);
	}

	void removeMiner(Player miner) {
		this.currentMiners.remove(miner);
	}

	void clearMiners() {
		this.currentMiners.clear();
	}

	Set<Player> getMiners() {
		return this.currentMiners;
	}

	public boolean isDead() {
		return this.health == 0;
	}

	Component getMinerComponent(boolean shortName) {
		if (this.currentMiners.size() == 1) {
			return EntityUtils.getComponent(currentMiners.iterator().next());
		}
		else if (this.currentMiners.size() == 0) {
			return Component.text("No one???");
		}
		else {
			// Players of many different teams may be mining at once
			// Count the # of each team and use the one that's highest

			Map<TeamArenaTeam, Integer> allMiners = new HashMap<>();
			for (Player miner : this.currentMiners) {
				allMiners.merge(Main.getPlayerInfo(miner).team, 1, Integer::sum);
			}

			TeamArenaTeam highestTeam = null;
			int numMiners = 0;
			for (var entry : allMiners.entrySet()) {
				if (entry.getValue() > numMiners) {
					numMiners = entry.getValue();
					highestTeam = entry.getKey();
				}
			}

			if (highestTeam == null) {
				Main.logger().warning("LifeOre.getMinerComponent highestTeam was null when should never be");
				return Component.text("Herobrine", NamedTextColor.YELLOW);
			}

			if (shortName)
				return highestTeam.getComponentSimpleName();
			else
				return highestTeam.getComponentName();
		}
	}

	private Component getTextDisplayComponent() {
		return Component.textOfChildren(
			owningTeam.getComponentName().append(Component.text("'s Life Ore", owningTeam.getRGBTextColor())),
			Component.newline(),
			this.healthComponents[this.health]
		);
	}

	OreBreakResult onBreak(Player breaker) {
		final PlayerInfo pinfo = Main.getPlayerInfo(breaker);
		if (pinfo.team != this.owningTeam) {
			this.health = Math.max(0, this.health - 1);
			// Update the hologram
			//this.hologramLines[1] = this.healthComponents[this.health];
			//this.hologram.setText(this.hologramLines);
			this.hologram.setText(this.getTextDisplayComponent());

			// Issue where after breaking, but the player continues mining, the BlockDamageEvent isn't re-called
			// So the player isn't in currentMiners anymore.
			// Band aid: add the breaker to currentMiners
			this.currentMiners.add(breaker);

			if (this.health == 0) {
				return OreBreakResult.KILLED;
			}
			else {
				return OreBreakResult.BROKEN_BY_ENEMY;
			}
		}
		else {
			this.currentMiners.remove(breaker);
			return OreBreakResult.BROKEN_BY_TEAMMATE;
		}
	}

	enum OreBreakResult {
		BROKEN_BY_ENEMY,
		BROKEN_BY_TEAMMATE,
		KILLED
	}

	private Component createHealthComponent(int health) {
		TextColor color;
		if (health == 0)
			return Component.text("RIP " + this.owningTeam.getName(), NamedTextColor.DARK_RED);
		else if (health == 1)
			color = TextColors.ERROR_RED;
		else if (health < 4)
			color = NamedTextColor.RED;
		else if (health < 8)
			color = NamedTextColor.YELLOW;
		else
			color = NamedTextColor.GREEN;

		return Component.text("Health: ").append(Component.text("" + health, color, TextDecoration.BOLD));
	}
}

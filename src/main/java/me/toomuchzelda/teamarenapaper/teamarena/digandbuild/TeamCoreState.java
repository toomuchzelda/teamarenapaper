package me.toomuchzelda.teamarenapaper.teamarena.digandbuild;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.commands.CommandDebug;
import me.toomuchzelda.teamarenapaper.teamarena.digandbuild.upgrades.HealUpgradeInfo;
import me.toomuchzelda.teamarenapaper.utils.EntityUtils;
import me.toomuchzelda.teamarenapaper.utils.TextColors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class TeamCoreState {
	/** Number of times ore must be broken to disqualify the team */
	public static final int STARTING_HEALTH = 25; // TODO make configurable
	public static final int MAX_HEALTH = 30;
	private static final double HEAL_PARTICLE_DIST = 1d;
	public static final int HEAL_PARTICLE_COUNT = 20;

	public record LifeOreProperties(@Nullable Component customName, @Nullable PointMarker hologram,
									double protectionDistanceSquared, double interactionDistanceSquared) {
		public boolean isProtected(Location ore, Location target) {
			return ore.distanceSquared(target) < protectionDistanceSquared;
		}

		public boolean isInteractable(Location ore, Location target) {
			return ore.equals(target) || ore.distanceSquared(target) < interactionDistanceSquared;
		}
	}
	private final Map<Block, LifeOreProperties> oreIntances;

	private final TeamArenaTeam team;
	private int health;

	private final Set<Player> currentMiners;

	TeamCoreState(World world, TeamArenaTeam team, BlockData defaultBlock, List<DigAndBuildInfo.LifeOreInfo> lifeOreInfos) {
		this.team = team;

		this.health = STARTING_HEALTH;

		Map<Block, LifeOreProperties> oreIntances = HashMap.newHashMap(lifeOreInfos.size());

		for (DigAndBuildInfo.LifeOreInfo lifeOreInfo : lifeOreInfos) {
			Block block = lifeOreInfo.location().toBlock(world);
			block.setBlockData(lifeOreInfo.block() != null ? lifeOreInfo.block() : defaultBlock);

			PointMarker hologram;

			if (!lifeOreInfo.hideHologram()) {
				hologram = new PointMarker(lifeOreInfo.location().toLocation(world).add(0.5d, 1.5d, 0.5d),
					getTextDisplayComponent(lifeOreInfo.customName()),
					this.team.getColour(), Material.ALLAY_SPAWN_EGG);
			} else {
				hologram = null;
			}
			oreIntances.put(block, new LifeOreProperties(
				lifeOreInfo.customName(), hologram,
				lifeOreInfo.protectionRadius() * lifeOreInfo.protectionRadius(),
				lifeOreInfo.interactionRadius() * lifeOreInfo.interactionRadius()
			));

		}
		this.oreIntances = Map.copyOf(oreIntances); // returns an optimized implementation for size = 1

		this.currentMiners = new HashSet<>();
	}

	void addMiner(Player miner) {
		if (Main.getPlayerInfo(miner).team != this.team)
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

	public TeamArenaTeam getTeam() {
		return team;
	}

	public Map<Block, LifeOreProperties> getLifeOres() {
		return oreIntances;
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
				Thread.dumpStack();
				return Component.text("Herobrine", NamedTextColor.YELLOW);
			}

			if (shortName)
				return highestTeam.getComponentSimpleName();
			else
				return highestTeam.getComponentName();
		}
	}

	private Component getTextDisplayComponent(@Nullable Component customName) {
		if (health == 0)
			return formatHealth();

		var builder = Component.text().append(team.colourWord("⭐ "));
		if (customName != null)
			builder.append(customName);
		else
			builder.append(team.getComponentSimpleName(), team.colourWord(" Core"));
		builder.appendNewline().append(formatHealth());
		return builder.build();
	}

	public boolean isLifeOre(Block block) {
		return oreIntances.containsKey(block);
	}

	public double getLifeOreProtectionRadius(Block block) {
		return Math.sqrt(oreIntances.get(block).protectionDistanceSquared);
	}

	@Nullable
	public Block findNearbyProtectedLifeOre(Location location) {
		Location temp = location.clone();
		for (Map.Entry<Block, LifeOreProperties> entry : oreIntances.entrySet()) {
			Block block = entry.getKey();
			LifeOreProperties properties = entry.getValue();
			if (properties.isProtected(block.getLocation(temp), location))
				return block;
		}
		return null;
	}

	@Nullable
	OreBreakResult onBreak(Player breaker, Block block) {
		if (!isLifeOre(block)) return null;

		if (this.health == 0) return OreBreakResult.ALREADY_DEAD;

		final PlayerInfo pinfo = Main.getPlayerInfo(breaker);
		if (CommandDebug.ignoreObjectiveTeamChecks || pinfo.team != this.team) {
			setHealth(this.health - 1);

			// Issue where after breaking, but the player continues mining, the BlockDamageEvent isn't re-called
			// So the player isn't in currentMiners anymore.
			// Band aid: add the breaker to currentMiners
			this.currentMiners.add(breaker);
			return OreBreakResult.DAMAGED_BY_ENEMY;
		} else {
			this.currentMiners.remove(breaker);
			return OreBreakResult.DAMAGED_BY_TEAMMATE;
		}
	}

	enum OreBreakResult {
		DAMAGED_BY_ENEMY,
		DAMAGED_BY_TEAMMATE,
		ALREADY_DEAD
	}

	public int getHealth() {
		return this.health;
	}

	public int getMaxHealth() {
		return STARTING_HEALTH;
	}

	public int getTotalMaxHealth(DigAndBuild game) {
		HealUpgradeInfo healUpgrade = game.getMapInfo().healUpgrade;
		return STARTING_HEALTH + (healUpgrade != null ? healUpgrade.maxShield() : 0);
	}

	public void setHealth(int newHealth) {
		if (this.health == 0) return; // Cannot change health after died.

		this.health = Math.min(Math.max(0, newHealth), MAX_HEALTH);
		for (var props : oreIntances.values()) {
			if (props.hologram != null) {
				props.hologram.setText(getTextDisplayComponent(props.customName));
			}
		}

		if (health == 0) {
			for (Block block : oreIntances.keySet()) {
				block.setType(Material.AIR);
			}
		}
	}

	public TextComponent formatHealth() {
		if (health == 0)
			return Component.text("🪦 " + team.getName(), NamedTextColor.DARK_RED);

		if (health < TeamCoreState.STARTING_HEALTH) {
			float percentage = (float) health / TeamCoreState.STARTING_HEALTH;
			return Component.text(health + "⛏",
				percentage < 0.5f ?
					TextColor.lerp(percentage * 2, NamedTextColor.DARK_RED, NamedTextColor.YELLOW) :
					TextColor.lerp((percentage - 0.5f) * 2, NamedTextColor.YELLOW, NamedTextColor.GREEN));
		} else {
			int extra = health - TeamCoreState.STARTING_HEALTH;
			if (extra != 0)
				return Component.textOfChildren(
					Component.text(TeamCoreState.STARTING_HEALTH, NamedTextColor.BLUE),
					Component.text(" + ", NamedTextColor.GRAY),
					Component.text(extra + "⛏", TextColors.ABSORPTION_HEART)
				);
			else
				return Component.text(TeamCoreState.STARTING_HEALTH + "⛏", NamedTextColor.BLUE);
		}
	}


	private static final Component CANT_BUILD_HERE = Component.text("You can't build here", TextColors.ERROR_RED);
	private static final Component CANT_BREAK_YOUR_ORE = Component.textOfChildren(
			Component.text("You can't break blocks near "), DigAndBuild.CORE, Component.text("s")
	).color(TextColors.ERROR_RED);
	public void playDenyBuildEffect(Player player, Block affected, Block protectionSource, boolean isBreaking) {
		Location loc = affected.getLocation().add(0.5d, 0.5d, 0.5d).subtract(player.getLocation().getDirection());
		if (isBreaking) {
			player.spawnParticle(Particle.BLOCK_MARKER, loc, 1, Material.BARRIER.createBlockData());
		} else {
			Particle.DustOptions data = new Particle.DustOptions(team.getColour(), 1);
			double radius = Math.sqrt(getLifeOreProtectionRadius(protectionSource));
			double sample = radius / 5;
			double centerX = protectionSource.getX() + 0.5;
			double centerY = protectionSource.getY() + 0.5;
			double centerZ = protectionSource.getZ() + 0.5;
			double minY = centerY - radius;
			double maxY = centerY + radius;
			// bottom and top
			player.spawnParticle(Particle.DUST, centerX, minY, centerZ, 0, data);
			player.spawnParticle(Particle.DUST, centerX, maxY, centerZ, 0, data);
			for (int j = 1; j <= 5; j++) {
				double y1 = minY + sample * j;
				double y2 = maxY - sample * j;
				double a = Math.acos((centerY - y1) / radius);
				double effectiveRadius = radius * Math.sin(a);
				for (int i = 0; i < 10; i++) {
					double b = 2 * Math.PI * i / 10;
					double x = centerX + effectiveRadius * Math.cos(b);
					double z = centerZ + effectiveRadius * Math.sin(b);
					player.spawnParticle(Particle.DUST, x, y1, z, 0, data);
					if (j != 5)
						player.spawnParticle(Particle.DUST, x, y2, z, 0, data);
				}
			}
		}
		player.playSound(loc, Sound.ENTITY_ITEM_BREAK, SoundCategory.BLOCKS, 0.5f, 2f);
		player.sendMessage(isBreaking ? CANT_BREAK_YOUR_ORE : CANT_BUILD_HERE);
	}

	public void tick() {

	}
}

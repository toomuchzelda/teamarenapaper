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
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class TeamLifeOres {
	/** Number of times ore must be broken to disqualify the team */
	public static final int STARTING_HEALTH = 25; // TODO make configurable
	public static final int MAX_HEALTH = 30;
	private static final double HEAL_PARTICLE_DIST = 1d;
	public static final int HEAL_PARTICLE_COUNT = 20;

	public record LifeOreProperties(double protectionDistanceSquared, double interactionDistanceSquared) {
		public boolean isProtected(Location ore, Location target) {
			return ore.distanceSquared(target) < protectionDistanceSquared;
		}

		public boolean isInteractable(Location ore, Location target) {
			return ore.distanceSquared(target) < interactionDistanceSquared;
		}
	}
	private final Map<Block, LifeOreProperties> oreIntances;

	private final TeamArenaTeam owningTeam;
	private int health;

	private final List<PointMarker> holograms;
	private final Set<Player> currentMiners;

	TeamLifeOres(World world, TeamArenaTeam owningTeam, BlockData defaultBlock, List<DigAndBuildInfo.LifeOreInfo> lifeOreInfos) {
		this.owningTeam = owningTeam;

		this.health = STARTING_HEALTH;

		holograms = new ArrayList<>();
		Map<Block, LifeOreProperties> oreIntances = HashMap.newHashMap(lifeOreInfos.size());

		Component displayText = getTextDisplayComponent();

		for (DigAndBuildInfo.LifeOreInfo lifeOreInfo : lifeOreInfos) {
			Block block = lifeOreInfo.location().toBlock(world);
			block.setBlockData(lifeOreInfo.block() != null ? lifeOreInfo.block() : defaultBlock);

			oreIntances.put(block, new LifeOreProperties(
				lifeOreInfo.protectionRadius() * lifeOreInfo.protectionRadius(),
				lifeOreInfo.interactionRadius() * lifeOreInfo.interactionRadius()
				));

			if (lifeOreInfo.showHologram()) {
				holograms.add(new PointMarker(lifeOreInfo.location().toLocation(world).add(0.5d, 1.5d, 0.5d), displayText,
					this.owningTeam.getColour(), Material.ALLAY_SPAWN_EGG));
			}
		}
		this.oreIntances = Map.copyOf(oreIntances); // returns an optimized implementation for size = 1

		this.currentMiners = new HashSet<>();
	}

	public void playCoolUpgradeEffect(Block block, ItemStack upgradeItem) {

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

	public TeamArenaTeam getTeam() {
		return owningTeam;
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

	private Component getTextDisplayComponent() {
		return Component.textOfChildren(
			owningTeam.getComponentName(),
			Component.text("'s Core", owningTeam.getRGBTextColor()),
			Component.newline(),
			formatHealth()
		);
	}

	public boolean isLifeOre(Block block) {
		return oreIntances.containsKey(block);
	}

	public double getLifeOreProtectionRadius(Block block) {
		return Math.sqrt(oreIntances.get(block).protectionDistanceSquared);
	}

	@Nullable
	OreBreakResult onBreak(Player breaker, Block block) {
		if (!isLifeOre(block)) return null;

		if (this.health == 0) return OreBreakResult.ALREADY_DEAD;

		final PlayerInfo pinfo = Main.getPlayerInfo(breaker);
		if (CommandDebug.ignoreObjectiveTeamChecks || pinfo.team != this.owningTeam) {
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
		HealUpgradeInfo healUpgrade = game.getTeamUpgrades(owningTeam).getHealUpgrade();
		return STARTING_HEALTH + (healUpgrade != null ? healUpgrade.maxShield() : 0);
	}

	public void setHealth(int newHealth) {
		if (this.health == 0) return; // Cannot change health after died.

		this.health = Math.min(Math.max(0, newHealth), MAX_HEALTH);
		Component display = this.getTextDisplayComponent();
		for (PointMarker hologram : holograms) {
			hologram.setText(display);
		}

		if (health == 0) {
			for (Block block : oreIntances.keySet()) {
				block.setType(Material.AIR);
			}
		}
	}

	public TextComponent formatHealth() {
		if (health == 0)
			return Component.text("🪦 " + owningTeam.getName(), NamedTextColor.DARK_RED);

		if (health < TeamLifeOres.STARTING_HEALTH) {
			float percentage = (float) health / TeamLifeOres.STARTING_HEALTH;
			return Component.text(health + "⛏",
				percentage < 0.5f ?
					TextColor.lerp(percentage * 2, NamedTextColor.DARK_RED, NamedTextColor.YELLOW) :
					TextColor.lerp((percentage - 0.5f) * 2, NamedTextColor.YELLOW, NamedTextColor.GREEN));
		} else {
			int extra = health - TeamLifeOres.STARTING_HEALTH;
			if (extra != 0)
				return Component.textOfChildren(
					Component.text(TeamLifeOres.STARTING_HEALTH, NamedTextColor.BLUE),
					Component.text(" + ", NamedTextColor.GRAY),
					Component.text(extra + "⛏", TextColors.ABSORPTION_HEART)
				);
			else
				return Component.text(TeamLifeOres.STARTING_HEALTH + "⛏", NamedTextColor.BLUE);
		}
	}
}

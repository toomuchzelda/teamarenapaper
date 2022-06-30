package me.toomuchzelda.teamarenapaper.teamarena.searchanddestroy;

import me.toomuchzelda.teamarenapaper.teamarena.ExplosionManager;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import me.toomuchzelda.teamarenapaper.utils.RealHologram;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class Bomb
{
	//number of ticks to keep a player 'arming' since their last interaction with the tnt block.
	//needed because: 1. holding right click only sends clicks 5 times/second
	// 2. lag can interrupt the receiving of right click packets which may interrupt arming which is undesirable.
	public static final int VALID_TICKS_SINCE_LAST_CLICK = 20;
	public static final int BOMB_DETONATION_TIME = 20 * 20;// * 20;
	public static final String PROGRESS_BAR_STRING = "█".repeat(10);
	public static final int BOMB_CHARCOAL_RADIUS = 10;
	public static final float BOMB_DESTROY_RADIUS = 10f;

	public static final int JUST_BEEN_DISARMED = -2;
	public static final int NULL_ARMED_TIME = -1;
	public static final int JUST_EXPLODED = -3;

	private final TeamArenaTeam owningTeam;
	private final Location spawnLoc;
	private RealHologram hologram;
	private final Component title;
	private final Map<TeamArenaTeam, TeamArmInfo> currentArmers;

	private TNTPrimed tnt;
	private boolean armed = false;
	private boolean detonated = false;
	private int armedTime;
	private TeamArenaTeam armingTeam;
	private ExplosionEffect explodeMode = ExplosionEffect.CARBONIZE_BLOCKS;

	public Bomb(TeamArenaTeam team, Location spawnLoc) {
		this.owningTeam = team;
		this.spawnLoc = spawnLoc;
		this.title = owningTeam.getComponentName().append(Component.text("'s Bomb")).decoration(TextDecoration.BOLD, true);

		currentArmers = new LinkedHashMap<>();
	}

	public void init() {
		spawnLoc.getBlock().setType(Material.TNT);
		//add half block XZ offsets to put it above centre of block
		this.hologram = new RealHologram(spawnLoc.clone().add(0.5d, 1d, 0.5d), RealHologram.Alignment.BOTTOM, this.title);
		//add offsets for tnt spawning/despawning
		spawnLoc.add(0.5d, 0d, 0.5d);
	}

	public void addClicker(TeamArenaTeam clickersTeam, Player clicker, int clickTime, float power) {
		TeamArmInfo teamClickers = getClickers(clickersTeam);
		//Bukkit.broadcastMessage("power: " + power);
		teamClickers.currentlyClicking.put(clicker, new PlayerArmInfo(clickTime, power));
	}

	public void arm(TeamArenaTeam armingTeam) {
		Bukkit.broadcastMessage("Armed");
		this.spawnLoc.getBlock().setType(Material.AIR);

		this.tnt = spawnLoc.getWorld().spawn(spawnLoc, TNTPrimed.class);
		tnt.setFuseTicks(BOMB_DETONATION_TIME);
		tnt.setVelocity(new Vector(0d, 0.4d, 0d));

		this.armingTeam = armingTeam;
		this.armedTime = TeamArena.getGameTick();
		this.armed = true;
	}

	public void disarm() {
		Bukkit.broadcastMessage("disarmed");
		this.spawnLoc.getBlock().setType(Material.TNT);

		this.tnt.remove();

		this.armingTeam = null;
		this.armedTime = JUST_BEEN_DISARMED;
		this.armed = false;
	}

	/**
	 * @param effect Type of effect for explosion
	 */
	public void detonate(ExplosionEffect effect) {
		if (effect == ExplosionEffect.DESTROY_BLOCKS) {
			ExplosionManager.EntityExplosionInfo exInfo = new ExplosionManager.EntityExplosionInfo(false, ExplosionManager.NO_FIRE,
					BOMB_DESTROY_RADIUS, ExplosionManager.DEFAULT_FLOAT_VALUE, true, null);

			ExplosionManager.setEntityInfo(this.tnt, exInfo);
		} else if (effect == ExplosionEffect.CARBONIZE_BLOCKS) {
			this.charExplosion();
		}

		this.armedTime = JUST_EXPLODED;
		this.armed = false;
		this.detonated = true;
	}

	public void tick() {
		if(!this.isArmed() && (this.armedTime == JUST_BEEN_DISARMED || this.armedTime == JUST_EXPLODED)) {
			this.armedTime = NULL_ARMED_TIME;
		}

		ArrayList<Component> hologramLines = new ArrayList<>(currentArmers.size() + 1);

		//tick current clickers and update the hologram
		hologramLines.add(0, this.title);
		int i = 1;
		final int currentTime = TeamArena.getGameTick();
		TeamArenaTeam armingTeam = null;
		for(Map.Entry<TeamArenaTeam, TeamArmInfo> entry : currentArmers.entrySet()) {
			TeamArmInfo teamInfo = entry.getValue();
			float progressToAdd = 0f;

			//add current clickers arming progress and remove any that haven't clicked for too long
			var iter = teamInfo.currentlyClicking.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<Player, PlayerArmInfo> clicker = iter.next();
				PlayerArmInfo pArmInfo = clicker.getValue();

				if (currentTime - pArmInfo.startTime() <= VALID_TICKS_SINCE_LAST_CLICK) {
					progressToAdd += pArmInfo.armingPower();
				}
				else {
					iter.remove();
				}
			}

			//if noone is left clicking on it then reset progress
			if(teamInfo.currentlyClicking.size() == 0) {
				teamInfo.armProgress = 0f;
				teamInfo.startTime = -1;
			}
			//else display the progress bars or arm the bomb if done
			else {
				teamInfo.armProgress += progressToAdd;
				float totalProgress = teamInfo.armProgress;
				//if there's any arming progress display the bar
				if(totalProgress > 0f) {
					TextColor teamColor = entry.getKey().getRGBTextColor();
					Component progressBar = TextUtils.getProgressText(PROGRESS_BAR_STRING, NamedTextColor.DARK_RED, NamedTextColor.DARK_RED,
							teamColor, totalProgress + -0.1f);
					hologramLines.add(i++,  progressBar);
				}

				//arm if done
				if(totalProgress >= 1f) {
					//break out of iterator first before arming
					armingTeam = entry.getKey();
					break;
				}
			}
		}

		if(armingTeam != null) {
			currentArmers.clear();
			if(this.isArmed())
				this.disarm();
			else
				this.arm(armingTeam);
		}

		//process bomb countdown
		if(this.isArmed()) {
			int timeDiff = currentTime - this.armedTime;
			if(timeDiff % 20 == 0) {
				this.tnt.getWorld().playSound(tnt.getLocation(), Sound.ENTITY_CREEPER_DEATH, SoundCategory.AMBIENT, 0.8f, 1f);
			}

			timeDiff = BOMB_DETONATION_TIME - timeDiff;
			int secondsLeft = timeDiff / 20;
			Component detonatingIn = Component.text().append(Component.text("Explodes in ", owningTeam.getRGBTextColor()))
					.append(Component.text(secondsLeft + "s", NamedTextColor.DARK_RED)).build();

			hologramLines.set(0, detonatingIn);

			if(timeDiff <= 0) {
				this.detonate(this.explodeMode);
			}
		}
		else if(this.isDetonated()){
			hologramLines.set(0, Component.text("R.I.P " + owningTeam.getName(), owningTeam.getRGBTextColor()));
		}

		Component[] finalLines = hologramLines.toArray(new Component[0]);
		this.hologram.setText(finalLines);
	}

	/**
	 * Replace nearby blocks with coal and similarly coloured blocks.
	 */
	public void charExplosion() {
		final int boxLength = BOMB_CHARCOAL_RADIUS * 2;
		final double maxRadiusSqr = BOMB_CHARCOAL_RADIUS * BOMB_CHARCOAL_RADIUS;
		final Location originalLoc = tnt.getLocation();

		Location currentLoc = originalLoc.clone();
		currentLoc.subtract(BOMB_CHARCOAL_RADIUS, BOMB_CHARCOAL_RADIUS, BOMB_CHARCOAL_RADIUS);
		Location startLoc = currentLoc.clone();
		//loop through a cube area, but only affect blocks in a sphere of sqrt(maxRadiusSqr) radius
		for(int x = 0; x < boxLength; x++) {
			for(int y = 0; y < boxLength; y++) {
				for (int z = 0; z < boxLength; z++) {
					Block block = currentLoc.getBlock();
					BlockData blockData = block.getBlockData();
					if (!block.getType().isAir() && block.isSolid()) {
						double distSqr = currentLoc.distanceSquared(originalLoc);
						if (distSqr <= maxRadiusSqr) {
							block.setBlockData(getCharredState(blockData));
						}
					}
					currentLoc.add(0d, 0d, 1d);
				}
				currentLoc.setZ(startLoc.getZ());
				currentLoc.add(0d, 1d, 0d);
			}
			currentLoc.setY(startLoc.getY());
			currentLoc.add(1d, 0d, 0d);
		}
	}

	/**
	 * Get a replacement BlockData to char a block
	 */
	public static BlockData getCharredState(BlockData blockData) {
		boolean rand = MathUtils.random.nextBoolean();
		Material material;

		if (blockData.getMaterial().isOccluding()) {
			material = rand ? Material.COAL_BLOCK : Material.BLACKSTONE;
			return material.createBlockData();
		}

		if (blockData instanceof Slab slabData) {
			material = rand ? Material.BLACKSTONE_SLAB : Material.COBBLED_DEEPSLATE_SLAB;
			return material.createBlockData(newBlockData -> {
				Slab newSlabData = (Slab) newBlockData;
				newSlabData.setType(slabData.getType());
				newSlabData.setWaterlogged(slabData.isWaterlogged());
			});
		} else if (blockData instanceof Stairs stairsData) {
			material = rand ? Material.BLACKSTONE_STAIRS : Material.COBBLED_DEEPSLATE_STAIRS;
			return material.createBlockData(newBlockData -> {
				Stairs newStairsData = (Stairs) newBlockData;
				newStairsData.setShape(stairsData.getShape());
				newStairsData.setHalf(stairsData.getHalf());
				newStairsData.setFacing(stairsData.getFacing());
				newStairsData.setWaterlogged(stairsData.isWaterlogged());
			});
		} else {
			//default: don't know, just return itself.
			return blockData;
		}
	}

	public boolean isArmed() {
		return this.armed;
	}

	public int getArmedTime() {
		return this.armedTime;
	}

	public boolean isDetonated() {
		return this.detonated;
	}

	private TeamArmInfo getClickers(TeamArenaTeam team) {
		return currentArmers.computeIfAbsent(team, teamArenaTeam -> new TeamArmInfo(TeamArena.getGameTick(), 0f));
	}

	public TeamArenaTeam getTeam() {
		return this.owningTeam;
	}

	public TeamArenaTeam getArmingTeam() {
		return this.armingTeam;
	}

	public TNTPrimed getTNT() {
		return this.tnt;
	}

	/**
	 * Get how much per tick an item should arm a Bomb. Used for special fuse enchantments
	 * @param item The fuse.
	 * @return Arm percent per tick in range 0.0 to 1.0
	 */
	public static float getArmProgressPerTick(ItemStack item) {
		return 1f / 10f / 20f;
	}

	private record PlayerArmInfo(int startTime, float armingPower) {}

	private static class TeamArmInfo {
		private int startTime;
		private float armProgress;
		private final Map<Player, PlayerArmInfo> currentlyClicking;

		private TeamArmInfo(int startTime, float armProgress) {
			this.startTime = startTime;
			this.armProgress = armProgress;

			this.currentlyClicking = new LinkedHashMap<>();
		}
	}

	public String toString() {
		return owningTeam.getName() + ' ' + spawnLoc.toString();
	}

	public enum ExplosionEffect {
		DO_NOTHING,
		DESTROY_BLOCKS,
		CARBONIZE_BLOCKS
	}
}

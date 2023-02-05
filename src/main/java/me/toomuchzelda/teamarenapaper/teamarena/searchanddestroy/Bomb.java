package me.toomuchzelda.teamarenapaper.teamarena.searchanddestroy;

import me.toomuchzelda.teamarenapaper.explosions.ExplosionManager;
import me.toomuchzelda.teamarenapaper.explosions.VanillaExplosionInfo;
import me.toomuchzelda.teamarenapaper.scoreboard.PlayerScoreboard;
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
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import java.util.*;

public class Bomb
{
	//number of ticks to keep a player 'arming' since their last interaction with the tnt block.
	//needed because: 1. holding right click only sends clicks 5 times/second
	// 2. lag can interrupt the receiving of right click packets which may interrupt arming which is undesirable.
	public static final int VALID_TICKS_SINCE_LAST_CLICK = 20;
	public static final int BOMB_DETONATION_TIME = 60 * 20;
	public static final String PROGRESS_BAR_STRING = "█".repeat(10);
	public static final int BOMB_CHARCOAL_RADIUS = 10;
	public static final float BOMB_DESTROY_RADIUS = 10f;

	public static final int JUST_BEEN_DISARMED = -2;
	public static final int NULL_ARMED_TIME = -1;
	public static final int JUST_EXPLODED = -3;

	private static final Vector TNT_HOLOGRAM_OFFSET = new Vector(0d, 1d, 0d);

	//space for indentation
	private static final Component BOMB_IS_SAFE = Component.text(" Bomb is safe");

	private final TeamArenaTeam owningTeam;
	private final Location spawnLoc;
	private RealHologram hologram;
	private final Component title;
	private final Map<TeamArenaTeam, TeamArmInfo> currentArmers;
	// The players that armed or disarmed the last arm/disarm event
	private Set<Player> responsibleArmers;

	private TNTPrimed tnt;
	private boolean armed = false;
	private boolean detonated = false;
	private int armedTime;
	private TeamArenaTeam armingTeam;
	private ExplosionEffect explodeMode = ExplosionEffect.CARBONIZE_BLOCKS;
	private int lastHissTime = 0;

	private Component sidebarStatus = BOMB_IS_SAFE;

	private ArmorStand glowingBlockStand; // An entity to make the solid block TNT appear as if it is glowing.
	private Team visualTeam; // Team for the glowing entities to be on to give them correct colour.

	public Bomb(TeamArenaTeam team, Location spawnLoc) {
		this.owningTeam = team;
		this.spawnLoc = spawnLoc;
		this.title = owningTeam.getComponentName().append(Component.text("'s Bomb")).decoration(TextDecoration.BOLD, true);

		currentArmers = new LinkedHashMap<>();
		responsibleArmers = new HashSet<>();
	}

	public void init(Team visualTeam) {
		this.visualTeam = visualTeam;
		spawnLoc.getBlock().setType(Material.TNT);
		//add half block XZ offsets to put it above centre of block
		this.hologram = new RealHologram(spawnLoc.clone().add(0.5d, 1d, 0.5d), RealHologram.Alignment.BOTTOM, this.title);
		//add offsets for tnt spawning/despawning
		spawnLoc.add(0.5d, 0d, 0.5d);
		this.glowingBlockStand = this.spawnGlowerStand(spawnLoc);
	}

	public void setGrave() {
		if(this.tnt != null) {
			this.removeTnt();
		}

		if (this.glowingBlockStand != null) {
			this.removeGlowerStand();
		}

		this.spawnLoc.getBlock().setType(Material.AIR);

		this.armed = false;
		this.detonated = true;
	}

	public void addClicker(TeamArenaTeam clickersTeam, Player clicker, int clickTime, float power) {
		TeamArmInfo teamClickers = getClickers(clickersTeam);
		//Bukkit.broadcastMessage("power: " + power);
		teamClickers.currentlyClicking.put(clicker, new PlayerArmInfo(clickTime, power));
	}

	public void arm(TeamArenaTeam armingTeam) {
		this.spawnLoc.getBlock().setType(Material.AIR);

		this.tnt = spawnLoc.getWorld().spawn(spawnLoc, TNTPrimed.class);
		tnt.setFuseTicks(BOMB_DETONATION_TIME + 1); //+1 so that the TNT explodes after the VanillaExplosionInfo is placed.
		tnt.setVelocity(new Vector(0d, 0.4d, 0d));
		tnt.setGlowing(true);

		this.visualTeam.addEntity(tnt); // Make tnt glow the team's colour
		PlayerScoreboard.addMembersAll(this.visualTeam, tnt);
		this.removeGlowerStand();

		this.armingTeam = armingTeam;
		this.armedTime = TeamArena.getGameTick();
		this.armed = true;
	}

	public void disarm() {
		this.spawnLoc.getBlock().setType(Material.TNT);
		this.hologram.moveTo(spawnLoc.clone().add(TNT_HOLOGRAM_OFFSET));

		this.removeTnt();

		this.glowingBlockStand = spawnGlowerStand(this.spawnLoc);

		this.armingTeam = null;
		this.armedTime = JUST_BEEN_DISARMED;
		this.armed = false;
	}

	/**
	 * @param effect Type of effect for explosion
	 */
	public void detonate(ExplosionEffect effect) {
		if (effect == ExplosionEffect.DESTROY_BLOCKS) {
			VanillaExplosionInfo exInfo = new VanillaExplosionInfo(false, VanillaExplosionInfo.FireMode.NO_FIRE,
					BOMB_DESTROY_RADIUS, VanillaExplosionInfo.DEFAULT_FLOAT_VALUE, true, null);

			ExplosionManager.setEntityInfo(this.tnt, exInfo);
		} else if (effect == ExplosionEffect.CARBONIZE_BLOCKS) {
			this.charExplosion();
		}

		this.armedTime = JUST_EXPLODED;
		this.armed = false;
		this.detonated = true;
		this.currentArmers.clear();
	}

	public void tick() {
		if(!this.isArmed() && (this.armedTime == JUST_BEEN_DISARMED || this.armedTime == JUST_EXPLODED)) {
			this.armedTime = NULL_ARMED_TIME;
		}

		ArrayList<Component> hologramLines = new ArrayList<>(currentArmers.size() + 1);
		//used only if bomb is armed
		Component disarmingProgressBar = null;

		//tick current clickers and update the hologram
		hologramLines.add(0, this.title);
		int i = 1;
		final int currentTick = TeamArena.getGameTick();
		TeamArenaTeam armingTeam = null;
		for(Map.Entry<TeamArenaTeam, TeamArmInfo> entry : currentArmers.entrySet()) {
			TeamArmInfo teamInfo = entry.getValue();
			float progressToAdd = 0f;

			//add current clickers arming progress and remove any that haven't clicked for too long
			var iter = teamInfo.currentlyClicking.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<Player, PlayerArmInfo> clicker = iter.next();
				PlayerArmInfo pArmInfo = clicker.getValue();

				if (currentTick - pArmInfo.startTime() <= VALID_TICKS_SINCE_LAST_CLICK) {
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
					disarmingProgressBar = TextUtils.getProgressText(PROGRESS_BAR_STRING, NamedTextColor.DARK_RED, NamedTextColor.DARK_RED,
							teamColor, totalProgress + -0.1f);
					hologramLines.add(i++,  disarmingProgressBar);
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
			this.responsibleArmers = new HashSet<>(currentArmers.get(armingTeam).currentlyClicking.keySet());
			currentArmers.clear();
			if(this.isArmed())
				this.disarm();
			else
				this.arm(armingTeam);
		}
		// if there is anyone clicking the bomb
		else if(i > 1){
			Location loc;
			if(this.isArmed())
				loc = this.tnt.getLocation();
			else
				loc = this.spawnLoc;

			if(currentTick - lastHissTime > 10) {
				lastHissTime = currentTick;
				loc.getWorld().playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.AMBIENT, 1f, 0f);
			}

			if(currentTick % 2 == 1) {
				loc.getWorld().spawnParticle(Particle.SMOKE_LARGE, loc.clone().add(0, 1, 0), 1, 0d, 0d, 0d, 0d);
			}
		}

		//process bomb countdown and move the hologram
		if(this.isArmed()) {
			//prevent TNT from being moved on X and Z by landmines and other external forces.
			// couldn't find a better solution
			Location moveTo = this.tnt.getLocation();
			if(moveTo.getX() != spawnLoc.getX() || moveTo.getZ() != spawnLoc.getZ()) {
				moveTo.setX(spawnLoc.getX());
				moveTo.setZ(spawnLoc.getZ());

				this.tnt.teleport(moveTo);
			}

			this.hologram.moveTo(moveTo.add(TNT_HOLOGRAM_OFFSET));

			final int timeLeft = BOMB_DETONATION_TIME - (currentTick - this.armedTime);
			final int secondsLeft = timeLeft / 20;

			if(timeLeft % 20 == 0) {
				this.tnt.getWorld().playSound(tnt.getLocation(), Sound.ENTITY_CREEPER_DEATH, SoundCategory.AMBIENT, 1.9f, 1.2f);
				//not when the bomb explodes, if 30 seconds or less, every 10 seconds, or if 5 seconds or less, every 5 seconds
				if(timeLeft > 0 && (timeLeft <= 5 * 20 || (timeLeft <= 30 * 20 && timeLeft % (10 * 20) == 0))) {
					this.owningTeam.getPlayerMembers().forEach(player ->
							player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_DEATH, SoundCategory.AMBIENT, 10000f, 2f));

					Component text = Component.text().append(
							Component.text(secondsLeft + " seconds left until ", NamedTextColor.GOLD),
							owningTeam.getComponentName(),
							Component.text("'s bomb goes off!", NamedTextColor.GOLD))
							.build();

					Bukkit.broadcast(text);
				}
			}

			Component detonatingIn = Component.text().append(Component.text("Explodes in ", owningTeam.getRGBTextColor()))
					.append(Component.text(secondsLeft + "s", NamedTextColor.DARK_RED)).build();

			hologramLines.set(0, detonatingIn);

			if(timeLeft <= 0) {
				this.detonate(this.explodeMode);
			}

			Component secondsText = Component.text(" "+ secondsLeft, NamedTextColor.DARK_RED);
			if(disarmingProgressBar != null) {
				this.sidebarStatus = disarmingProgressBar.append(secondsText);
			}
			else {
				this.sidebarStatus = Component.textOfChildren(Component.space(), detonatingIn);
			}
		}
		else if(this.isDetonated()){
			hologramLines.set(0, Component.text("R.I.P " + owningTeam.getName(), owningTeam.getRGBTextColor()));
		}
		else {
			this.sidebarStatus = BOMB_IS_SAFE;
		}

		Component[] finalLines = hologramLines.toArray(new Component[0]);
		this.hologram.setText(finalLines);
	}

	private ArmorStand spawnGlowerStand(Location spawnLoc) {
		return spawnLoc.getWorld().spawn(spawnLoc.clone().add(0d, -1.1875, 0d), ArmorStand.class, armorStand -> {
			armorStand.setMarker(true);
			armorStand.setInvisible(true);
			armorStand.setCanTick(false);
			armorStand.setSilent(true);
			armorStand.setGlowing(true);

			armorStand.getEquipment().setHelmet(new ItemStack(Material.REDSTONE_BLOCK), true);

			this.visualTeam.addEntity(armorStand);
			PlayerScoreboard.addMembersAll(this.visualTeam, armorStand);
		});
	}

	private void removeGlowerStand() {
		this.visualTeam.removeEntities(this.glowingBlockStand);
		PlayerScoreboard.removeMembersAll(this.visualTeam, this.glowingBlockStand);

		this.glowingBlockStand.remove();
		this.glowingBlockStand = null;
	}

	private void removeTnt() {
		this.tnt.remove();
		this.visualTeam.removeEntities(tnt);
		PlayerScoreboard.removeMembersAll(this.visualTeam, this.tnt);
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

	public void setExplodeMode(ExplosionEffect effect) {
		this.explodeMode = effect;
	}

	public TeamArmInfo getClickers(TeamArenaTeam team) {
		return currentArmers.computeIfAbsent(team, teamArenaTeam -> new TeamArmInfo(TeamArena.getGameTick(), 0f));
	}

	public TeamArenaTeam getTeam() {
		return this.owningTeam;
	}

	public Location getSpawnLoc() {
		return this.spawnLoc.clone();
	}

	public TeamArenaTeam getArmingTeam() {
		return this.armingTeam;
	}

	/**
	 * Get the specific Players that were clicking for the last arm/disarm
	 */
	public Set<Player> getResponsibleArmers() {
		return this.responsibleArmers;
	}

	public TNTPrimed getTNT() {
		return this.tnt;
	}

	public Component getSidebarStatus() {
		return this.sidebarStatus;
	}

	public double getExplosionProgress() {
		return (double) (TeamArena.getGameTick() - armedTime) / BOMB_DETONATION_TIME;
	}

	public Set<TeamArenaTeam> getArmingTeams() {
		return Collections.unmodifiableSet(currentArmers.keySet());
	}

	public double getTeamArmingProgress(TeamArenaTeam team) {
		TeamArmInfo teamArmInfo = currentArmers.get(team);
		if (teamArmInfo == null)
			throw new IllegalArgumentException(team + " is not arming!");
		return teamArmInfo.armProgress;
	}

	private record PlayerArmInfo(int startTime, float armingPower) {}

	static class TeamArmInfo {
		private int startTime;
		private float armProgress;
		private final Map<Player, PlayerArmInfo> currentlyClicking;

		private TeamArmInfo(int startTime, float armProgress) {
			this.startTime = startTime;
			this.armProgress = armProgress;

			this.currentlyClicking = new LinkedHashMap<>();
		}

		Set<Player> getPlayers() {
			return this.currentlyClicking.keySet();
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

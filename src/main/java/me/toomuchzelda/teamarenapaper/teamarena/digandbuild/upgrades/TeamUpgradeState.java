package me.toomuchzelda.teamarenapaper.teamarena.digandbuild.upgrades;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.metadata.MetaIndex;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.digandbuild.DigAndBuild;
import me.toomuchzelda.teamarenapaper.teamarena.digandbuild.DigAndBuildInfo;
import me.toomuchzelda.teamarenapaper.teamarena.digandbuild.TeamCoreState;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import me.toomuchzelda.teamarenapaper.utils.TextColors;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketEntity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class TeamUpgradeState {
	private final DigAndBuild game;

	private final TeamArenaTeam team;
	private final TeamCoreState core;

	private final TrapUpgradeInfo trapUpgrade;

	public static final int UPGRADE_SACRIFICE_END_Y = 5;

	private int traps = 0;
	private static final int TRAP_INTERVAL = 10;
	private static final int TRAP_COOLDOWN = 5 * 20;
	private int trapTriggeredAt = -TRAP_COOLDOWN;
	record AnchoredParrot(PacketEntity parrot, Location anchor, int spawnTick) {}
	private final Deque<AnchoredParrot> trapBirds = new ArrayDeque<>();
	private static final int BIRD_GUARD_INTERVAL = 100;

	public TeamUpgradeState(DigAndBuild game, TeamArenaTeam team, TeamCoreState core, DigAndBuildInfo dnbInfo) {
		this.game = game;
		this.team = team;
		this.core = core;

		trapUpgrade = dnbInfo.trapUpgrade;
		if (trapUpgrade != null) {
			for (int i = 0; i < trapUpgrade._default(); i++) {
				addTrap();
			}
		}
	}

	/**
	 * Handles interactions with the core
	 * @param player The player
	 * @param e The event
	 * @return Whether the event is handled successfully
	 */
	public boolean onInteract(Player player, PlayerInteractEvent e) {
		PlayerInfo info = Main.getPlayerInfo(player);
		if (info.team != team)
			return false;
		ItemStack item = e.getItem();
		UpgradeBase upgrade = game.getUpgradeFromItem(item);
		if (upgrade == null)
			return false;
		// check item amount
		PlayerInventory inventory = player.getInventory();
		var matchingItems = ItemUtils.findMatchingItems(inventory, item, upgrade.required());
		int matchingCount = matchingItems.values().stream().mapToInt(i -> i).sum();
		if (matchingCount < upgrade.required()) {
			player.sendMessage(Component.textOfChildren(
				Component.text("You need " + (upgrade.required() - matchingCount) + " more "),
				upgrade.displayName(),
				Component.text("!")
			).color(TextColors.ERROR_RED));
			e.setCancelled(true);
			return true;
		}

		// check if clicking core
		Location playerLoc = player.getLocation();
		Location clickLoc = e.getClickedBlock() != null ? e.getClickedBlock().getLocation() : null;
		Location oreLoc = playerLoc.clone();
		TeamCoreState ores = game.getTeamLifeOre(team);
		Block core = null;
		for (var entry : ores.getLifeOres().entrySet()) {
			entry.getKey().getLocation(oreLoc);
			TeamCoreState.LifeOreProperties props = entry.getValue();
			if (props.isInteractable(oreLoc, playerLoc) ||
				(clickLoc != null && props.isInteractable(oreLoc, clickLoc))) {
				core = entry.getKey();
				break;
			}
		}
		if (core == null) {
			player.sendMessage(Component.textOfChildren(
				Component.text("Right click", TextUtils.RIGHT_CLICK_TO),
				Component.text(" near your "),
				DigAndBuild.CORE,
				Component.text(" to apply this upgrade!")
			));
			e.setCancelled(true);
			return true;
		}
		boolean success = upgrade.apply(game, team, core, player);
		if (success) {
			ItemUtils.removeMatchingItems(inventory, matchingItems);
		}
		e.setCancelled(true);
		return true;
	}

	public int getTraps() {
		return traps;
	}

	public void addTrap() {
		traps++;

		Location randomCore = MathUtils.randomElement(core.getLifeOres().keySet().toArray(new Block[0])).getLocation().toCenterLocation();
		randomCore.add(MathUtils.random.nextGaussian(), UPGRADE_SACRIFICE_END_Y, MathUtils.random.nextGaussian());
		randomCore.setYaw(MathUtils.random.nextInt(360)); // random yaw

		EntityType entityType;
		double random = MathUtils.random.nextDouble();
		if (random < 0.01)
			entityType = EntityType.BEE;
		else if (random < 0.03)
			entityType = EntityType.ALLAY;
		else if (random < 0.1)
			entityType = EntityType.CHICKEN;
		else
			entityType = EntityType.PARROT;

		Location offsetLocation = randomCore.clone().add(0, 0, traps);
		PacketEntity parrot = new PacketEntity(PacketEntity.NEW_ID, entityType,
			offsetLocation, null, PacketEntity.VISIBLE_TO_ALL);
		if (entityType == EntityType.PARROT)
			parrot.setMetadata(MetaIndex.PARROT_VARIANT_OBJ, MathUtils.random.nextInt(Parrot.Variant.values().length));
		parrot.updateMetadataPacket();
		parrot.respawn();
//		Parrot realParrot = game.gameWorld.spawn(randomCore.clone().add(0, 0, traps), Parrot.class, e -> {
//			e.setAI(false);
//		});
		trapBirds.add(new AnchoredParrot(parrot, randomCore, TeamArena.getGameTick()));
	}

	public void tick() {
		int now = TeamArena.getGameTick();
		if (trapUpgrade != null && traps > 0) {
			if (now % TRAP_INTERVAL == 0 && now - trapTriggeredAt >= TRAP_COOLDOWN) {
				tickTrap();
			}
			// animate government drones
			if (now % 2 == 0) {
				for (AnchoredParrot anchoredParrot : trapBirds) {
					Location anchor = anchoredParrot.anchor;
					Location newLocation = anchor.clone();
					int elapsed = now - anchoredParrot.spawnTick;
					double angle = Math.TAU * (elapsed % BIRD_GUARD_INTERVAL) / BIRD_GUARD_INTERVAL;
					newLocation.setX(anchor.getX() + 3.5 * Math.sin(angle));
					newLocation.setZ(anchor.getZ() + 3.5 * Math.cos(angle));
					newLocation.setY(anchor.getY() + Math.sin(Math.TAU * (elapsed % 20d) / 20) / 2);
					newLocation.setYaw((float) Math.toDegrees(angle + Math.PI / 2));
					anchoredParrot.parrot.move(newLocation);
				}
			}
		}
	}

	private void tickTrap() {
		if (trapUpgrade == null)
			return;

		double range = trapUpgrade.triggerRadius();
		double rangeSq = range * range;

		List<Location> cores = core.getLifeOres().keySet().stream()
			.map(coreBlock -> coreBlock.getLocation().add(0.5, 0.5, 0.5))
			.toList();

		Location temp = new Location(null, 0, 0, 0);
		for (Player player : game.getPlayers()) {
			if (team.hasMember(player)) continue;
			if (game.isDead(player)) continue;

			player.getLocation(temp);
			for (Location location : cores) {
				if (location.distanceSquared(temp) <= rangeSq) {
					triggerTrap(player, location, range);
					return;
				}
			}
		}
	}

	private void triggerTrap(Player triggeredBy, Location location, double radius) {
		if (trapUpgrade == null) return;

		trapTriggeredAt = TeamArena.getGameTick();
		traps--;

		var victimParrot = trapBirds.removeFirst();

		game.animationTasks.add(new BukkitRunnable() {
			int elapsed = 0;
			World world = location.getWorld();
			@Override
			public void run() {
				if (elapsed >= 40) {
					if (elapsed == 40) {
						world.playSound(location, Sound.ENTITY_PARROT_DEATH, SoundCategory.PLAYERS, 2, 1);
						victimParrot.parrot.playStatus(PacketEntity.ENTITY_STATUS_DEATH);
//						victimParrot.realParrot.setHealth(0);
					} else if (elapsed == 50) {
						cancel();
					}
				} else if (elapsed % 5 == 0) {
					world.playSound(location, Sound.ENTITY_PARROT_HURT, SoundCategory.PLAYERS, 2, elapsed % 10 == 0 ? 1 : 0);
				}
				elapsed++;
			}

			@Override
			public synchronized void cancel() throws IllegalStateException {
				super.cancel();
				victimParrot.parrot.remove();
			}
		}.runTaskTimer(Main.getPlugin(), 0, 1));

		var teamAlert = Component.textOfChildren(
			triggeredBy.playerListName(),
			Component.text(" triggered a trap!", NamedTextColor.GOLD)
		);
		Title title = Title.title(trapUpgrade.displayName(), teamAlert);
		team.showTitle(title);
		team.sendMessage(teamAlert);

		var triggererAlert = Component.text("You triggered a trap!", NamedTextColor.RED);
		triggeredBy.showTitle(Title.title(Component.text("Oops!", NamedTextColor.DARK_RED), triggererAlert));
		triggeredBy.sendMessage(triggererAlert);
	}

	public void playSacrificeAnimation(ItemStack stack, Location location, Runnable onAnimationEnd) {
		int randomOffset = MathUtils.random.nextInt(20);
		game.animationTasks.add(new BukkitRunnable() {
			int elapsed = 0;
			static final int DURATION = 40;
			final World world = location.getWorld();

			final Location baseLocation = location.clone();
			final Location currentLocation = nextLocation(baseLocation.clone());

			final ItemDisplay display = world.spawn(currentLocation, ItemDisplay.class, display -> {
				display.setItemStack(stack);
				display.setBrightness(new Display.Brightness(15, 15));
				display.setTeleportDuration(1);
			});

			public Location nextLocation(Location location) {
				double yOffset = (double) (UPGRADE_SACRIFICE_END_Y * elapsed) / DURATION;
				double xzAngle = Math.TAU * ((elapsed + randomOffset) % 20) / 20;
				double xzRadius = (UPGRADE_SACRIFICE_END_Y - yOffset) / 2;
				location.setX(baseLocation.getX() + xzRadius * Math.sin(xzAngle));
				location.setZ(baseLocation.getZ() + xzRadius * Math.cos(xzAngle));
				location.setYaw((float) Math.toDegrees((xzAngle + Math.TAU) % Math.TAU));
				location.setY(baseLocation.getY() + yOffset);
				return location;
			}

			@Override
			public void run() {
				if (elapsed == DURATION) {
					world.spawnParticle(Particle.ITEM, currentLocation, 20, 0, 0, 0, 0.1, stack);
					onAnimationEnd.run();
					cancel();
				}
				display.teleport(nextLocation(currentLocation));
				world.spawnParticle(Particle.END_ROD, currentLocation, 0, 0, 0, 0, 0);
				elapsed++;
			}

			@Override
			public synchronized void cancel() throws IllegalStateException {
				display.remove();
				super.cancel();
			}
		}.runTaskTimer(Main.getPlugin(), 0, 1));
	}
}

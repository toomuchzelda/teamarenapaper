package me.toomuchzelda.teamarenapaper.teamarena.capturetheflag;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.*;
import me.toomuchzelda.teamarenapaper.teamarena.announcer.AnnouncerManager;
import me.toomuchzelda.teamarenapaper.teamarena.announcer.AnnouncerSound;
import me.toomuchzelda.teamarenapaper.teamarena.commands.CommandDebug;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.gamescheduler.TeamArenaMap;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import me.toomuchzelda.teamarenapaper.utils.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.map.MapCursor;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.io.File;
import java.util.*;

public class CaptureTheFlag extends TeamArena
{
	public HashMap<TeamArenaTeam, Flag> teamToFlags; //initialized in parseConfig
	public HashMap<ArmorStand, Flag> flagStands; // this too
	public HashMap<Player, Set<Flag>> flagHolders = new HashMap<>();
	public HashSet<String> flagItems;
	protected static final AttributeModifier SPEED_ATTR = new AttributeModifier("CTFSpeedBoost", 0.2d, AttributeModifier.Operation.ADD_SCALAR);
	protected Set<Player> currentSpeeders = new HashSet<>();
	protected int capsToWin;
	public static final int TIME_TO_SPEED_BOOST = 6 * 60 * 20;
	protected int timeToSpeed = TIME_TO_SPEED_BOOST;
	public static final int TIME_TO_END_AFTER_SPEED = 3 * 60 * 20;
	public static final int TIME_TO_END_MINUTES = (TIME_TO_END_AFTER_SPEED / 60) / 20;

	public static final int TAKEN_FLAG_RETURN_TIME = 3 * 60 * 20;
	public static final int DROPPED_TIME_PER_TICK = TAKEN_FLAG_RETURN_TIME / (5 * 20);
	public static final int DROPPED_PROGRESS_BAR_LENGTH = 10;
	public static final String DROPPED_PROGRESS_STRING = "█".repeat(DROPPED_PROGRESS_BAR_LENGTH);

	private static final Component GAME_NAME = Component.text("Capture the Flag", NamedTextColor.AQUA);
	public static final Component HOW_TO_PLAY = Component.text("Take other team's flags and bring them to yours to win! You can't capture another flag if your flag has been taken.", NamedTextColor.AQUA);

	public static final Component PICK_UP_MESSAGE = Component.text("%holdingTeam% has picked up %team%'s flag").color(NamedTextColor.GOLD);
	public static final Component DROP_MESSAGE = Component.text("%holdingTeam% has dropped %team%'s flag").color(NamedTextColor.GOLD);
	public static final Component RETURNED_MESSAGE = Component.text("%team%'s flag has been returned to their base").color(NamedTextColor.GOLD);
	public static final Component CAPTURED_MESSAGE = Component.text("%holdingTeam% has captured %team%'s flag!").color(NamedTextColor.GOLD);;
	public static final Component PASSED_MESSAGE = Component.text("%holdingTeam% has passed %team%'s flag to %otherTeam%").color(NamedTextColor.GOLD);
	//shorter ones for titles
	public static final Component PICK_UP_TITLE = Component.text("%holdingTeam% took %team%'s flag").color(NamedTextColor.GOLD);
	public static final Component DROP_TITLE = Component.text("%holdingTeam% dropped %team%'s flag").color(NamedTextColor.GOLD);
	public static final Component RETURNED_TITLE = Component.text("%team%'s flag returned").color(NamedTextColor.GOLD);
	public static final Component CAPTURED_TITLE = Component.text("%holdingTeam% captured %team%'s flag!").color(NamedTextColor.GOLD);
	public static final Component PASSED_TITLE = Component.text("%holdingTeam% gave %team%'s flag to %otherTeam%").color(NamedTextColor.GOLD);

	public static final Component CANT_CAPTURE_YOUR_FLAG_NOT_AT_BASE = Component.text("You can't capture until your flag is safely at your base!", TextColors.ERROR_RED);
	public static final String CANT_CAPTURE_KEY = "yrflagnotatbase";

	public static final Component CANT_TELEPORT_HOLDING_FLAG_MESSAGE = Component.text("You can't teleport while holding the flag!", TextColors.ERROR_RED);
	public static final Component CANT_TELEPORT_HOLDING_FLAG_TITLE = Component.text("Can't teleport with the flag!", TextColors.ERROR_RED);

	public static final Component ONE_MINUTE_LEFT_SPEED_MESSAGE = Component.text("In one minute flag takers will get a speed bonus! Hurry it up!", TextColors.ERROR_RED);
	public static final Component ONE_MINUTE_LEFT_SPEED_TITLE = Component.text("Flag takers get speed in 1 minute!", TextColors.ERROR_RED);
	public static final Component SPEED_NOW_MESSAGE = Component.text("Anyone who carries a flag will now run faster! This bonus will end once any flag is captured!" +
			" If you don't end the game in " + TIME_TO_END_MINUTES + " minutes, I will!!", TextColors.ERROR_RED);
	public static final Component SPEED_NOW_TITLE = Component.text("Flag takers will now run faster!", TextColors.ERROR_RED);
	public static final Component SPEED_DONE_MESSAGE = Component.text("Speed bonus for carrying a flag is gone! Game proceeds as normal.", TextColors.ERROR_RED);
	public static final Component TOOK_TOO_LONG = Component.text("Too slow! Game ended!", TextColors.ERROR_RED);

	private void addFlagHeld(Player player, Flag flag) {
		Set<Flag> flags = flagHolders.computeIfAbsent(player, k -> new HashSet<>()); //put new HashSet if no value and also return it
		flags.add(flag);

		//mount onto their head, on top of other flags if they have any
		// make a sort of vehicle chain, rather than mount all of them on the one player coz they'll just overlap each other
		Entity toBeVehicle = player;
		List<Entity> passengers;
		while(true) {
			passengers = toBeVehicle.getPassengers();
			if(passengers.size() > 0) {
				toBeVehicle = passengers.get(0);
			}
			else {
				break;
			}
		}

		toBeVehicle.addPassenger(flag.getArmorStand());

		flag.getArmorStand().setMarker(true);
	}

	private void removeFlags(Player player) {
		flagHolders.remove(player);
	}

	private void removeFlag(Player player, Flag flag) {
		Set<Flag> flags = flagHolders.get(player);
		flags.remove(flag);

		Entity flagSittingOn = flag.getArmorStand().getVehicle(); //entity this Flag is sitting on
		flagSittingOn.eject();

		List<Entity> flagsPassengers = flag.getArmorStand().getPassengers();
		if(flagsPassengers.size() > 0) {
			flag.getArmorStand().eject(); //eject stands sitting on the flag
			flagSittingOn.addPassenger(flagsPassengers.get(0));
		}

		if(flags.size() == 0)
			flagHolders.remove(player);

		flag.getArmorStand().setMarker(false);
	}

	public CaptureTheFlag(TeamArenaMap map) {
		super(map);
	}

	@Override
	public void liveTick() {

		//check if dropped flags have been left long enough to be returned
		// use their nametag for time left to return
		for(Flag flag : teamToFlags.values()) {
			if(!flag.isAtBase) {// && !flag.isBeingCarried()) {
				int returnSpeed;
				if(flag.isBeingCarried())
					returnSpeed = 1;
				else
					returnSpeed = DROPPED_TIME_PER_TICK;

				flag.ticksUntilReturn = flag.ticksUntilReturn - returnSpeed;

				//int timePassed = TAKEN_FLAG_RETURN_TIME - flag.ticksUntilReturn;
				if(flag.ticksUntilReturn <= 0) {
					returnFlagToBase(flag);
				}
				else {
					float percentage = (float) flag.ticksUntilReturn / (float) TAKEN_FLAG_RETURN_TIME;
					percentage += 0.05;
					if(percentage > 1)
						percentage = 1;
					else if (percentage < 0)
						percentage = 0;

					int splitIndex = (int) ((float) DROPPED_PROGRESS_BAR_LENGTH * percentage);

					Component firstComponent = Component.text()
							.content(DROPPED_PROGRESS_STRING.substring(0, splitIndex))
							.color(flag.team.getRGBTextColor())
							.append(Component.text().content(DROPPED_PROGRESS_STRING.substring(splitIndex))
									.color(NamedTextColor.DARK_RED)
									.build()).build();

					flag.progressBarComponent = firstComponent;

					flag.getArmorStand().customName(firstComponent);

					//set the flag item's durability so the holder can see it too
					if(flag.holder != null) {
						ItemStack invFlag = getFlagInInventory(flag, flag.holder);

						if(invFlag == null || invFlag.getType().isAir()) {
							return;
//							throw new IllegalStateException("Flag " + flag.team.getName() +
//									" holder field is not null but the player does not have the item in their inventory!");
						}

						Damageable meta = (Damageable) invFlag.getItemMeta();
						short durability = (short) (invFlag.getType().getMaxDurability() * (1 - percentage));
						meta.setDamage(durability);
						invFlag.setItemMeta(meta);
					}
				}
			}
		}
		if (!CommandDebug.ignoreWinConditions) { // disable anti-stall if debug

			this.timeToSpeed--;
			//one minute left, announce
			Component announcementMsg = null;
			Component announcementTitle = null;
			if (this.timeToSpeed == 60 * 20) {
				announcementMsg = ONE_MINUTE_LEFT_SPEED_MESSAGE;
				announcementTitle = ONE_MINUTE_LEFT_SPEED_TITLE;
			} else if (this.timeToSpeed == 0) {
				announcementMsg = SPEED_NOW_MESSAGE;
				announcementTitle = SPEED_NOW_TITLE;

				for (Player carrier : flagHolders.keySet()) {
					carrier.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addModifier(SPEED_ATTR);
					currentSpeeders.add(carrier);
				}
			}
			// out of time, end the game.
			else if (this.timeToSpeed == -TIME_TO_END_AFTER_SPEED) {
				int score = 0;
				List<TeamArenaTeam> drawList = new ArrayList<>(teams.length);
				for (TeamArenaTeam team : teams) {
					if (team.getTotalScore() > score) {
						score = team.getTotalScore();
						drawList.clear();
						drawList.add(team);
					} else if (team.getTotalScore() == score) {
						drawList.add(team);
					}

					//only 1 winner
					if (drawList.size() == 1) {
						this.winningTeam = drawList.get(0);
					}

					Bukkit.broadcast(TOOK_TOO_LONG);
					for (Player p : Bukkit.getOnlinePlayers()) {
						for (int i = 0; i < 10; i++) {
							p.playSound(p.getLocation(), SoundUtils.getRandomObnoxiousSound(), 9999f, (float) MathUtils.randomRange(-0.5, 2d));
						}
					}

					prepEnd();

					//Don't do the rest of the tick.
					return;
				}
			}

			if (announcementMsg != null) {
				Bukkit.broadcast(announcementMsg);

				var title = TextUtils.createTitle(Component.empty(), announcementTitle, 10, 40, 10);
				Main.getPlayerInfoMap().forEach((player, playerInfo) -> {
					if (playerInfo.getPreference(Preferences.RECEIVE_GAME_TITLES)) {
						player.showTitle(title);
					}
				});

				// annoy players
				for (int i = 0; i < 5; i++) {
					Bukkit.getScheduler().runTaskLater(Main.getPlugin(), () -> {
						for (var player : Bukkit.getOnlinePlayers()) {
							player.playSound(player, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, SoundCategory.AMBIENT, 1, 1);
						}
					}, i);
				}
			}
		}

		super.liveTick();
	}

	private final Map<Flag, Component> sidebarCache = new LinkedHashMap<>();
	@Override
	public Collection<Component> updateSharedSidebar() {
		sidebarCache.clear();

		var flags = teamToFlags.values().stream()
				.filter(flag -> CommandDebug.ignoreWinConditions || flag.team.isAlive())
				.sorted(Flag.BY_SCORE_DESC)
				.toList();

		for (var flag : flags) {
			var builder = Component.text();
			builder.append(flag.team.getComponentSimpleName(), Component.text(": "));
			builder.append(Component.text("⚑ " + flag.team.getTotalScore(), NamedTextColor.GREEN));
			if (flag.holdingTeam != null) {
				builder.append(
						Component.space(),
						TextUtils.getProgressBar(NamedTextColor.GRAY, flag.holdingTeam.getRGBTextColor(),
								1, (double) flag.ticksUntilReturn / TAKEN_FLAG_RETURN_TIME).decorate(TextDecoration.BOLD),
						flag.holdingTeam.colourWord(" Held").decorate(TextDecoration.BOLD)
				);
			} else if(!flag.isAtBase) {
				builder.append(
						Component.space(),
						TextUtils.getProgressText("↓ Dropped", NamedTextColor.GRAY, flag.team.getRGBTextColor(), NamedTextColor.GREEN,
						1 - (double) flag.ticksUntilReturn / TAKEN_FLAG_RETURN_TIME));
			}
			sidebarCache.put(flag, builder.build());
		}

		return Collections.singletonList(Component.textOfChildren(
				Component.text("First to ", NamedTextColor.GRAY),
				Component.text("⚑ " + capsToWin, NamedTextColor.GREEN)
		));
	}

	@Override
	public void updateSidebar(Player player, SidebarManager sidebar) {
		var playerTeam = Main.getPlayerInfo(player).team;
		sidebar.setTitle(player, getGameName());

		int teamsShown = 0;

		for (var entry : sidebarCache.entrySet()) {
			var flag = entry.getKey();
			var team = flag.team;
			Component line = entry.getValue();

			if (teamsShown >= 4 && team != playerTeam)
				continue; // don't show
			teamsShown++;
			if (team == playerTeam) {
				// blink red when flag picked up
				var teamPrefix = !flag.isAtBase && TeamArena.getGameTick() % 20 < 10 ? OWN_TEAM_PREFIX_DANGER : OWN_TEAM_PREFIX;
				sidebar.addEntry(Component.textOfChildren(teamPrefix, line));
			} else {
				sidebar.addEntry(line);
			}
		}
		// unimportant teams
		if (sidebarCache.size() != teamsShown)
			sidebar.addEntry(Component.text("+ " + (sidebarCache.size() - teamsShown) + " teams", NamedTextColor.GRAY));

	}

	@Override
	public void updateLegacySidebar(Player player, SidebarManager sidebar) {
		sidebar.setTitle(player, Component.text("CapsToWin: " + capsToWin, NamedTextColor.GOLD));
		//update the sidebar every tick
		byte numLines;
		List<Flag> aliveFlags = teamToFlags.values().stream()
				.filter(flag -> flag.team.isAlive())
				.sorted(Flag.BY_SCORE_DESC)
				.toList();

		if (aliveFlags.size() <= 7)
			numLines = 2;
		else
			numLines = 1;

		Component[] lines = new Component[numLines * aliveFlags.size()];

		int index = 0;
		for (Flag flag : aliveFlags) {
			Component first = flag.team.getComponentSimpleName();
			if (numLines == 2) {
				Component flagStatus = Component.text("Flag ").color(NamedTextColor.WHITE);
				if (flag.isAtBase)
					flagStatus = flagStatus.append(Component.text("Safe").color(NamedTextColor.GREEN));
				else if (flag.holdingTeam != null) {
					flagStatus = flagStatus.append(Component.text("Held by ")).append(flag.holdingTeam.getComponentSimpleName());
				} else {
					flagStatus = flagStatus.append(flag.progressBarComponent);//Component.text("Unsafe").color(TextColor.color(255, 85, 0)));
				}

				lines[index] = first.append(Component.text(": " + flag.team.getTotalScore()).color(NamedTextColor.WHITE));
				lines[index + 1] = flagStatus;
			} else {
				Component flagStatus;
				if (flag.isAtBase)
					flagStatus = Component.text("Safe").color(NamedTextColor.GREEN);
				else if (flag.holdingTeam != null) {
					flagStatus = Component.text("Held").color(flag.holdingTeam.getRGBTextColor());
				} else {
					flagStatus = flag.progressBarComponent;//Component.text("Unsafe").color(TextColor.color(255, 85, 0));
				}
				lines[index] = first.append(Component.text(": " + flag.team.getTotalScore() + ' ').color(NamedTextColor.WHITE).append(flagStatus));
			}

			index += numLines;
		}

		for (var line : lines) {
			sidebar.addEntry(line);
		}
	}

	@Override
	public void tick() {
		super.tick();

		//tick flags
		for(Map.Entry<ArmorStand, Flag> entry : flagStands.entrySet()) {
			ArmorStand stand = entry.getKey();
			Flag flag = entry.getValue();
			if(!gameState.isEndGame()) {
				if(!flag.isBeingCarried()) {
					//spin and bob around if not being carried
					flagPositionTick(flag);

					//check player get it
					if(gameState == GameState.LIVE) {
						for (Player p : players) {
							//if(isSpectator(p))
							//	continue;

							if(p.getBoundingBox().overlaps(stand.getBoundingBox())) {
								//return flag to base if teammate touches it and not at base
								if(flag.team.getPlayerMembers().contains(p)) {
									if(!flag.isAtBase) {
										returnFlagToBase(flag);
										break;
									}
								}
								//enemy picked up the flag
								else {
									pickUpFlag(p, flag, true);
									break;
								}
							}
						}
					}
				}
				//flag is being carried, check for capture
				else if(gameState == GameState.LIVE) {
					Player holder = flag.holder;
					//check if this player's team's flag is taken
					// if not taken, check if this player is touching their flag base thing and capture if they are
					TeamArenaTeam team = flag.holdingTeam;//Main.getPlayerInfo(holder).team;
					Flag holderTeamsFlag = teamToFlags.get(team);
					if(holder.getBoundingBox().overlaps(holderTeamsFlag.baseBox)) {
						if(holderTeamsFlag.isAtBase) //capture the flag!!
							captureTheFlag(holder, team, flag);
						else {
							PlayerInfo pinfo = Main.getPlayerInfo(holder);
							if(pinfo.messageHasCooldowned(CANT_CAPTURE_KEY, 3 * 20)) {
								holder.sendMessage(CANT_CAPTURE_YOUR_FLAG_NOT_AT_BASE);
								PlayerUtils.sendTitle(holder, Component.empty(), CANT_CAPTURE_YOUR_FLAG_NOT_AT_BASE,
										0, 25, 10);
							}
						}
					}
				}
			}
		}
	}

	@Override
	public void handleDeath(DamageEvent event) {
		if(event.getVictim() instanceof Player p) {
			dropFlags(p);
		}

		super.handleDeath(event);
	}

	public void pickUpFlag(Player player, Flag flag, boolean broadcast) {
		if (!CommandDebug.ignoreWinConditions && !flag.team.isAlive()) {
			Component text = Component.text("Taking the flag of a dead team? Talk about cheap!").color(TextColor.color(255, 20 ,20));
			player.sendMessage(text);
			player.playSound(player.getLocation(), Sound.ENTITY_HORSE_DEATH, SoundCategory.AMBIENT, 2f, 0.5f);
			return;
		}

		//the player is picking up their own flag (possible if someone else gives it to them)
		if(flag.team.getPlayerMembers().contains(player)) {
			returnFlagToBase(flag);
			return;
		}

		//flagHolders.put(player, flag);
		addFlagHeld(player, flag);
		if(flag.isAtBase)
			flag.ticksUntilReturn = TAKEN_FLAG_RETURN_TIME;

		flag.isAtBase = false;
		flag.holder = player;
		flag.holdingTeam = Main.getPlayerInfo(player).team;
		//player.addPassenger(flag.getArmorStand());
		player.setGlowing(true);
		//send a packet to remove the armor stand so it doesn't obstruct their view
		PlayerUtils.sendPacket(player, flag.getRemovePacket());

		//give them the inventory item
		player.getInventory().addItem(flag.item);

		//give speed boost if appropriate
		if(this.timeToSpeed <= 0) {
			player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addModifier(SPEED_ATTR);
			currentSpeeders.add(player);
		}

		if(broadcast) {
			final TextReplacementConfig playerConfig = TextReplacementConfig.builder().match("%holdingTeam%")
					.replacement(player.playerListName()).build();
			final TextReplacementConfig flagTeamConfig = TextReplacementConfig.builder().match("%team%")
					.replacement(flag.team.getComponentSimpleName()).build();

			Component pickupChat = PICK_UP_MESSAGE.replaceText(playerConfig).replaceText(flagTeamConfig);
			Component pickupTitle = PICK_UP_TITLE.replaceText(playerConfig).replaceText(flagTeamConfig);

			Iterator<Map.Entry<Player, PlayerInfo>> iter = Main.getPlayersIter();
			while (iter.hasNext()) {
				Map.Entry<Player, PlayerInfo> entry = iter.next();
				Player p = entry.getKey();
				if (entry.getValue().getPreference(Preferences.RECEIVE_GAME_TITLES)) {
					PlayerUtils.sendTitle(p, Component.empty(), pickupTitle, 7, 30, 7);
				}

				p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_BREAK, SoundCategory.AMBIENT, 2, 1f);

				// Different sound for flag droppper / everyone else
				if (p != player) {
					TeamArenaTeam team = entry.getValue().team;
					AnnouncerSound sound;
					if (flag.team == team) {
						sound = AnnouncerSound.GAME_FLAG_STOLEN;
					}
					else {
						sound = AnnouncerSound.GAME_FLAG_TAKEN;
					}
					AnnouncerManager.playSound(p, sound);
				}
			}

			AnnouncerManager.playSound(player, AnnouncerSound.GAME_FLAG_YOU_GOT_THE);
			Bukkit.broadcast(pickupChat);
		}
	}

	public void dropFlags(Player player) {
		Set<Flag> mapFlags = flagHolders.get(player);
		if(mapFlags != null) {
			Set<Flag> flags = new HashSet<>(flagHolders.get(player)); //avoid concurrentmodification
			for (Flag flag : flags) {
				dropFlag(player, flag, true);
			}
		}
	}

	public void dropFlag(Player player, Flag flag, boolean broadcast) {
		//player.removePassenger(flag.getArmorStand()); todo: manage this in removeFlag(Player, Flag)
		flag.currentLoc = player.getLocation();
		flag.holder = null;
		flag.holdingTeam = null;
		//flagHolders.remove(player);
		removeFlag(player, flag);
		player.setGlowing(false);
		flag.sendRecreatePackets(player);
		//player.getInventory().remove(flag.item);
		ItemUtils.removeFromPlayerInventory(getFlagInInventory(flag, player), player);

		//if there's no floor to land on when it's dropped teleport it back to base
		if(BlockUtils.getFloor(flag.currentLoc) == null) {
			flag.teleportToBase();
		}

		AttributeInstance aInst = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
		if(aInst.getModifiers().contains(SPEED_ATTR)) {
			aInst.removeModifier(SPEED_ATTR);
		}
		currentSpeeders.remove(player);

		if(broadcast) {
			final TextReplacementConfig playerConfig = TextReplacementConfig.builder().match("%holdingTeam%")
					.replacement(player.playerListName()).build();
			final TextReplacementConfig teamConfig = TextReplacementConfig.builder().match("%team%")
					.replacement(flag.team.getComponentSimpleName()).build();

			Component titleText = DROP_TITLE.replaceText(playerConfig).replaceText(teamConfig);
			Component chatText = DROP_MESSAGE.replaceText(playerConfig).replaceText(teamConfig);

			Iterator<Map.Entry<Player, PlayerInfo>> iter = Main.getPlayersIter();
			while (iter.hasNext()) {
				Map.Entry<Player, PlayerInfo> entry = iter.next();
				Player p = entry.getKey();
				if (entry.getValue().getPreference(Preferences.RECEIVE_GAME_TITLES)) {
					PlayerUtils.sendTitle(p, Component.empty(), titleText, 7, 30, 7);
				}

				p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_PLACE, SoundCategory.AMBIENT, 2, 1f);

				if (p != player) {
					AnnouncerManager.playSound(p, AnnouncerSound.GAME_FLAG_DROPPED);
				}
			}
			AnnouncerManager.playSound(player, AnnouncerSound.GAME_FLAG_YOU_LOST_THE);
			Bukkit.broadcast(chatText);
		}
	}

	public void returnFlagToBase(Flag flag) {

		if(flag.holder != null) {
			flag.holder.setGlowing(false);
			removeFlag(flag.holder, flag);
			flag.sendRecreatePackets(flag.holder);
			//flag.holder.getInventory().remove(flag.item);
			ItemUtils.removeFromPlayerInventory(getFlagInInventory(flag, flag.holder), flag.holder);
		}
		flag.teleportToBase();

		final TextReplacementConfig returnConfig = TextReplacementConfig.builder().match("%team%").replacement(flag.team.getComponentSimpleName()).build();

		Component chatText = RETURNED_MESSAGE.replaceText(returnConfig);
		Component titleText = RETURNED_TITLE.replaceText(returnConfig);

		var iter = Main.getPlayersIter();
		while(iter.hasNext()) {
			Map.Entry<Player, PlayerInfo> entry = iter.next();
			Player p = entry.getKey();

			if(entry.getValue().getPreference(Preferences.RECEIVE_GAME_TITLES)) {
				PlayerUtils.sendTitle(p, Component.empty(), titleText, 7, 30, 7);
			}

			p.playSound(p.getLocation(), Sound.BLOCK_LARGE_AMETHYST_BUD_PLACE, SoundCategory.AMBIENT, 2, 1);
		}

		AnnouncerManager.broadcastSound(AnnouncerSound.GAME_FLAG_RECOVERED);
		Bukkit.broadcast(chatText);
	}

	public void captureTheFlag(Player player, TeamArenaTeam capturingTeam, Flag capturedFlag) {
		capturingTeam.score++;
		player.setGlowing(false);
		//flagHolders.remove(player);
		removeFlag(player, capturedFlag);
		capturedFlag.teleportToBase();
		capturedFlag.sendRecreatePackets(player);
		player.getInventory().remove(capturedFlag.item);
		ItemUtils.removeFromPlayerInventory(getFlagInInventory(capturedFlag, player), player);

		updateBossBars();

		final TextReplacementConfig holdingConfig = TextReplacementConfig.builder().match("%holdingTeam%")
				.replacement(player.playerListName()).build();
		final TextReplacementConfig victimConfig = TextReplacementConfig.builder().match("%team%").replacement(capturedFlag.team.getComponentSimpleName()).build();

		Component chatText = CAPTURED_MESSAGE.replaceText(holdingConfig).replaceText(victimConfig);
		Component titleText = CAPTURED_TITLE.replaceText(holdingConfig).replaceText(victimConfig);

		var iter = Main.getPlayersIter();
		while(iter.hasNext()) {
			Map.Entry<Player, PlayerInfo> entry = iter.next();
			Player p = entry.getKey();

			if(entry.getValue().getPreference(Preferences.RECEIVE_GAME_TITLES)) {
				PlayerUtils.sendTitle(p, Component.empty(), titleText, 7, 30, 7);
			}

			p.playSound(p.getLocation(), Sound.BLOCK_LARGE_AMETHYST_BUD_BREAK, SoundCategory.AMBIENT, 2f, 1f);
		}

		Bukkit.broadcast(chatText);
		AnnouncerManager.broadcastSound(AnnouncerSound.GAME_FLAG_CAPTURED);

		if(this.timeToSpeed <= 0) {
			Bukkit.broadcast(SPEED_DONE_MESSAGE);
		}
		//reset speed timer and remove speed from current flag holders
		this.timeToSpeed = TIME_TO_SPEED_BOOST;

		Iterator<Player> speedersIter = currentSpeeders.iterator();
		while(speedersIter.hasNext()) {
			speedersIter.next().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(SPEED_ATTR);
			speedersIter.remove();
		}

		//end the game if win
		if (!CommandDebug.ignoreWinConditions && capturingTeam.score >= capsToWin) {
			winningTeam = capturingTeam;
			prepEnd();
		}
	}

	public void updateBossBars() {
		for(TeamArenaTeam team : teams) {
			float progress = (float) team.score / (float) capsToWin;
			if(progress > 1)
				progress = 1; //floating point moment
			team.bossBar.progress(progress);
		}
	}

	public void flagPositionTick(Flag flag) {
		ArmorStand stand = flag.getArmorStand();
		Location loc;
		if(flag.isAtBase) {
			loc = flag.baseLoc.clone();
		} else {
			RayTraceResult result = flag.currentLoc.getWorld().rayTraceBlocks(flag.currentLoc,
					new Vector(0, -1, 0), 383, FluidCollisionMode.SOURCE_ONLY, true);

			if(result != null) {
				double distance = flag.currentLoc.toVector().distance(result.getHitPosition());
				//max fall of 0.1 blocks per tick
				if(distance > 0.5) {
					distance = 0.1;
					flag.currentLoc.subtract(0, distance, 0);
				}
			}
			else {
				Main.logger().warning("Flag has been dropped and left above void, should be impossible!");
				Thread.dumpStack();
				flag.teleportToBase();
				return;
			}
			loc = flag.currentLoc.clone();
		}
		loc.setY(loc.getY() + Math.sin(TeamArena.getGameTick() / 5d) / 10);
		loc.setYaw((stand.getLocation().getYaw() + 5f) % 360);
		stand.teleport(loc);
	}

	@Override
	public void onDamage(DamageEvent event) {
		super.onDamage(event);

		if(event.getVictim() instanceof ArmorStand stand && flagStands.containsKey(stand)) {
			event.setCancelled(true);
		}
	}

	//passing the flag from one player to another
	@Override
	public void onInteractEntity(PlayerInteractEntityEvent event) {
		super.onInteractEntity(event);
		if(event.getRightClicked() instanceof Player receiver) {
			ItemStack usedItem = event.getPlayer().getEquipment().getItem(event.getHand());
			Set<Flag> flagsHeld = flagHolders.get(event.getPlayer());
			if(flagsHeld != null) {
				for(Flag flag : flagsHeld) {
					if(itemIsThisFlagsItem(flag, usedItem)) {
						final TextReplacementConfig dropperConfig = TextReplacementConfig.builder().match("%holdingTeam%").replacement(event.getPlayer().playerListName()).build();
						final TextReplacementConfig teamMessageConfig = TextReplacementConfig.builder().match("%team%").replacement(flag.team.getComponentName()).build();
						final TextReplacementConfig teamTitleConfig = TextReplacementConfig.builder().match("%team%").replacement(flag.team.getComponentSimpleName()).build();
						final TextReplacementConfig newHolderConfig = TextReplacementConfig.builder().match("%otherTeam%").replacement(receiver.playerListName()).build();

						Component chatMessage = PASSED_MESSAGE.replaceText(dropperConfig).replaceText(teamMessageConfig).replaceText(newHolderConfig);
						Component titleMessage = PASSED_TITLE.replaceText(dropperConfig).replaceText(teamTitleConfig).replaceText(newHolderConfig);

						var iter = Main.getPlayersIter();
						while(iter.hasNext()) {
							var entry = iter.next();
							Player p = entry.getKey();

							if(entry.getValue().getPreference(Preferences.RECEIVE_GAME_TITLES)) {
								PlayerUtils.sendTitle(p, Component.empty(), titleMessage, 7, 30 ,7);
							}

							p.playSound(p.getLocation(), Sound.BLOCK_SMALL_AMETHYST_BUD_BREAK, SoundCategory.AMBIENT, 2f, 1f);
						}

						Bukkit.broadcast(chatMessage);

						//pass the flag on to who was clicked
						dropFlag(event.getPlayer(), flag, false);
						pickUpFlag(receiver, flag, false);

						break;
					}
				}
			}
		}
	}

	public boolean isFlagItem(ItemStack item) {
		if(item == null) return false;
		ItemMeta meta = item.getItemMeta();
		return meta != null && flagItems.contains(meta.getDisplayName());
	}

	@Override
	public boolean isWearableArmorPiece(ItemStack item) {
		return !isFlagItem(item) && super.isWearableArmorPiece(item);
	}

	/**
	 * Get the copy of a flag item in a players inventory e.g the specific flag item in the inventory of a player holding the flag
	 */
	public @Nullable ItemStack getFlagInInventory(Flag flag, Player player) {
		Iterator<ItemStack> iter = player.getInventory().iterator();
		while(iter.hasNext()) {
			ItemStack item = iter.next();

			if(itemIsThisFlagsItem(flag, item))
				return item;
		}

		//they may be holding it on their mouse in their inventory
		ItemStack cursor = player.getItemOnCursor();
		if(itemIsThisFlagsItem(flag, cursor))
			return cursor;

		return null;
	}

	public boolean itemIsThisFlagsItem(Flag flag, ItemStack item) {
		if(item == null)
			return false;

		if(item.getType() == Material.LEATHER_CHESTPLATE) {
			ItemStack cloneItem = item.clone();
			Damageable meta = (Damageable) cloneItem.getItemMeta();
			meta.setDamage(0);
			cloneItem.setItemMeta(meta);

			return flag.item.isSimilar(cloneItem);
		}

		return false;
	}

	@Override
	public void onInteract(PlayerInteractEvent event) {
		if((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)
				&& isFlagItem(event.getItem())) {
			event.setUseItemInHand(Event.Result.DENY);
		}

		super.onInteract(event);
	}

	@Override
	public void prepLive() {
		super.prepLive();

	}

	@Override
	public void setupMiniMap() {
		// register flag cursors
		for (var entry : teamToFlags.entrySet()) {
			TeamArenaTeam team = entry.getKey();
			Flag flag = entry.getValue();

			ArmorStand stand = flag.getArmorStand();
			MapCursor.Type icon = MapCursor.Type.valueOf("BANNER_" + team.getDyeColour().name());
			Component flagText = Component.text(team.getSimpleName() + " flag", team.getRGBTextColor());
			Component yourFlagText = Component.text("Your flag", team.getRGBTextColor());
			miniMap.registerCursor(
				// hide dead flags
				(ignored1, ignored2) -> CommandDebug.ignoreWinConditions || gameState == GameState.PREGAME || team.isAlive(),
				(player, playerInfo) -> {
					// display extra information for own flag
					if (playerInfo.team == team) {
						if (flag.holder != null && gameTick % 40 < 20) {
							return new MiniMapManager.CursorInfo(flag.holder.getLocation(), true, MapCursor.Type.RED_POINTER, yourFlagText);
						} else {
							return new MiniMapManager.CursorInfo(stand.getLocation(), false, icon, yourFlagText);
						}
					} else {
						return new MiniMapManager.CursorInfo(stand.getLocation(), false, icon, flagText);
					}
				}
			);
		}
	}

	@Override
	public void prepEnd() {
		super.prepEnd();

		flagItems.clear();

		for (Player carrier : currentSpeeders) {
			carrier.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(SPEED_ATTR);
		}
	}

	@Override
	public void prepDead() {
		super.prepDead();

		for(Flag flag : flagStands.values()) {
			flag.getArmorStand().remove();
			flag.unregisterTeam();
		}
	}

	@Override
	public boolean canSelectKitNow() {
		return !gameState.isEndGame();
	}

	@Override
	public boolean canSelectTeamNow() {
		return gameState == GameState.PREGAME;
	}

	@Override
	public boolean canTeamChatNow(Player player) {
		return gameState != GameState.PREGAME && gameState != GameState.DEAD;
	}

	@Override
	public boolean canHeal(Player medic, LivingEntity target) {
		if(flagStands.containsKey(target))
			return false;

		return super.canHeal(medic, target);
	}

	@Override
	public boolean isRespawningGame() {
		return true;
	}

	public boolean isFlagCarrier(Player p) {
		return flagHolders.containsKey(p);
	}

	@Override
	public Component getGameName() {
		return GAME_NAME;
	}

	@Override
	public Component getHowToPlayBrief() {
		return HOW_TO_PLAY;
	}

	@Override
	public void loadConfig(TeamArenaMap map) {
		super.loadConfig(map);

		flagStands = new HashMap<>();
		teamToFlags = new HashMap<>();

		TeamArenaMap.CTFInfo ctfConfig = map.getCtfInfo();
		if(ctfConfig == null) {
			throw new IllegalArgumentException("CaptureTheFlag constructor called with non-CTF map");
		}

		flagItems = new HashSet<>();
		this.capsToWin = ctfConfig.capsToWin();
		for (Map.Entry<String, Vector> entry : ctfConfig.teamFlags().entrySet()) {
			TeamArenaTeam team = getTeamByLegacyConfigName(entry.getKey());
			if (team == null) {
				throw new IllegalArgumentException("Unknown team " + entry.getKey());
			}

			Location teamsFlagLoc = entry.getValue().toLocation(gameWorld);
			Flag flag = new Flag(this, team, teamsFlagLoc);

			teamToFlags.put(team, flag);
		}
	}

	@Override
	public File getMapPath() {
		return new File(super.getMapPath(), "CTF");
	}
}

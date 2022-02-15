package me.toomuchzelda.teamarenapaper.teamarena.capturetheflag;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.core.BlockUtils;
import me.toomuchzelda.teamarenapaper.core.ItemUtils;
import me.toomuchzelda.teamarenapaper.core.PlayerUtils;
import me.toomuchzelda.teamarenapaper.teamarena.*;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.*;

public class CaptureTheFlag extends TeamArena
{
	public HashMap<TeamArenaTeam, Flag> teamToFlags; //initialized in parseConfig
	public HashMap<ArmorStand, Flag> flagStands; // this too
	public HashMap<Player, Set<Flag>> flagHolders = new HashMap<>();
	public HashSet<String> flagItems;
	public int capsToWin;
	public static final int TAKEN_FLAG_RETURN_TIME = 3 * 60 * 20;
	public static final int DROPPED_TIME_PER_TICK = TAKEN_FLAG_RETURN_TIME / (5 * 20);
	public static final int DROPPED_PROGRESS_BAR_LENGTH = 10;
	public static final String DROPPED_PROGRESS_STRING;
	
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
	
	public static final Component CANT_CAPTURE_YOUR_FLAG_NOT_AT_BASE = Component.text("You can't capture until your flag is safely at your base!").color(TextColor.color(255, 20, 20));
	public static final String CANT_CAPTURE_KEY = "yrflagnotatbase";
	
	public static final Component CANT_TELEPORT_HOLDING_FLAG_MESSAGE = Component.text("You can't teleport while holding the flag!").color(TextColor.color(255, 20, 20));
	public static final Component CANT_TELEPORT_HOLDING_FLAG_TITLE = Component.text("Can't teleport with the flag!").color(TextColor.color(255, 20, 20));
	
	static {
		StringBuilder builder = new StringBuilder(DROPPED_PROGRESS_BAR_LENGTH);
		for(int i = 0; i < DROPPED_PROGRESS_BAR_LENGTH; i++) {
			builder.append('█');
		}
		DROPPED_PROGRESS_STRING = builder.toString();
	}

	private Set<Flag> getFlagsHeld(Player player) {
		return flagHolders.get(player);
	}

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
	}
	
	public CaptureTheFlag() {
		super();
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
						
						if(invFlag == null || invFlag.getType().isAir())
							throw new IllegalStateException("Flag " + flag.team.getName() +
									" holder field is not null but the player does not have the item in their inventory!");
						
						Damageable meta = (Damageable) invFlag.getItemMeta();
						short durability = (short) (invFlag.getType().getMaxDurability() * (1 - percentage));
						meta.setDamage(durability);
						invFlag.setItemMeta(meta);
					}
				}
			}
		}
		
		updateLiveSidebar();
		
		super.liveTick();
	}
	
	public void updateLiveSidebar() {
		//update the sidebar every tick
		byte numLines;
		LinkedList<Flag> aliveFlags = new LinkedList<>();
		
		Component[] lines;
		for(Flag flag : teamToFlags.values()) {
			if(flag.team.isAlive())
				aliveFlags.add(flag);
		}
		
		Comparator<Flag> byScore = (teamArenaTeam, t1) -> (t1.team.getTotalScore()) - (teamArenaTeam.team.getTotalScore());
		aliveFlags.sort(byScore);
		
		if(aliveFlags.size() <= 7)
			numLines = 2;
		else
			numLines = 1;
		
		lines = new Component[numLines * aliveFlags.size()];
		
		int index = 0;
		for (Flag flag : aliveFlags) {
			Component first = flag.team.getComponentSimpleName();
			if(numLines == 2) {
				Component flagStatus = Component.text("Flag ").color(NamedTextColor.WHITE);
				if(flag.isAtBase)
					flagStatus = flagStatus.append(Component.text("Safe").color(NamedTextColor.GREEN));
				else if(flag.holdingTeam != null) {
					flagStatus = flagStatus.append(Component.text("Held by ")).append(flag.holdingTeam.getComponentSimpleName());
				}
				else {
					flagStatus = flagStatus.append(flag.progressBarComponent);//Component.text("Unsafe").color(TextColor.color(255, 85, 0)));
				}
				
				lines[index] = first.append(Component.text(": " + flag.team.getTotalScore()).color(NamedTextColor.WHITE));
				lines[index + 1] = flagStatus;
			}
			else {
				Component flagStatus;
				if(flag.isAtBase)
					flagStatus = Component.text("Safe").color(NamedTextColor.GREEN);
				else if(flag.holdingTeam != null) {
					flagStatus = Component.text("Held").color(flag.holdingTeam.getRGBTextColor());
				}
				else {
					flagStatus = flag.progressBarComponent;//Component.text("Unsafe").color(TextColor.color(255, 85, 0));
				}
				lines[index] = first.append(Component.text(": " + flag.team.getTotalScore() + ' ').color(NamedTextColor.WHITE).append(flagStatus));
			}
			
			index += numLines;
		}
		
		SidebarManager.setLines(lines);
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
		/*if(flagHolders.containsKey(player)) {
			player.sendMessage("ur already hold flag ");
			return;
		}*/
		if(!flag.team.isAlive()) {
			final Component text = Component.text("Taking the flag of a dead team? Talk about cheap!").color(TextColor.color(255, 20 ,20));
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

				//todo maybe a preference for game sounds
				p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_BREAK, SoundCategory.AMBIENT, 2, 1f);
			}

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

		if(broadcast) {
			Iterator<Map.Entry<Player, PlayerInfo>> iter = Main.getPlayersIter();

			final TextReplacementConfig playerConfig = TextReplacementConfig.builder().match("%holdingTeam%")
					.replacement(player.playerListName()).build();
			final TextReplacementConfig teamConfig = TextReplacementConfig.builder().match("%team%")
					.replacement(flag.team.getComponentSimpleName()).build();

			Component titleText = DROP_TITLE.replaceText(playerConfig).replaceText(teamConfig);
			Component chatText = DROP_MESSAGE.replaceText(playerConfig).replaceText(teamConfig);

			while (iter.hasNext()) {
				Map.Entry<Player, PlayerInfo> entry = iter.next();
				Player p = entry.getKey();
				// dae use unsafe type casts because the preference system is so bad
				if (entry.getValue().getPreference(Preferences.RECEIVE_GAME_TITLES)) {
					PlayerUtils.sendTitle(p, Component.empty(), titleText, 7, 30, 7);
				}

				p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_PLACE, SoundCategory.AMBIENT, 2, 1f);
			}
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
		
		//end the game if win
		if(capturingTeam.score >= capsToWin) {
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
		if(flag.isAtBase)
			loc = flag.baseLoc.clone();
		else {
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
		loc.setY(loc.getY() + (Math.sin((double) System.currentTimeMillis() / 2) / 5));
		loc.setYaw(((stand.getLocation().getYaw() + 5f) % 360));//- 180f);
		stand.teleport(loc);
		//net.minecraft.world.entity.decoration.ArmorStand nmsStand = ((CraftArmorStand) stand).getHandle();
		//nmsStand.moveTo(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
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

		SidebarManager.setTitle(Component.text("CapsToWin: " + capsToWin).color(NamedTextColor.GOLD));
	}

	@Override
	public void prepEnd() {
		super.prepEnd();
		
		flagItems.clear();
		
		
		Bukkit.getScheduler().runTaskLater(Main.getPlugin(), bukkitTask -> {
			
			for(Flag flag : flagStands.values()) {
				flag.getArmorStand().remove();
				flag.unregisterTeam();
			}
		
		}, END_GAME_TIME - 4); //one tick before the scheduled tasks in super
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
	public boolean isRespawningGame() {
		return true;
	}
	
	public boolean isFlagCarrier(Player p) {
		return flagHolders.containsKey(p);
	}
	
	@Override
	public void parseConfig(Map<String, Object> map) {
		super.parseConfig(map);
		
		flagStands = new HashMap<>();
		teamToFlags = new HashMap<>();
		
		Map<String, Object> customFlags = (Map<String, Object>) map.get("Custom");
		
		Main.logger().info("Custom Info: ");
		Main.logger().info(customFlags.toString());

		flagItems = new HashSet<>();
		for (Map.Entry<String, Object> entry : customFlags.entrySet()) {
			if (entry.getKey().equalsIgnoreCase("CapsToWin")) {
				try {
					capsToWin = (Integer) entry.getValue();//Integer.parseInt(entry.getValue());
				} catch (NullPointerException | ClassCastException e) {
					Main.logger().warning("Invalid CapsToWin! Must be an integer number (no decimals!). Defaulting to 3");
					e.printStackTrace();
					capsToWin = 3;
				}
			} else {
				TeamArenaTeam team = getTeamByName(entry.getKey());
				if (team == null) {
					throw new IllegalArgumentException("Unknown team " + entry.getKey() + "!!!! Use the team's full name i.e \"Red Team:\"");
				}
				
				Location teamsFlagLoc = BlockUtils.parseCoordsToVec((String) entry.getValue(), 0.5, -0.4, 0.5).toLocation(gameWorld);
				Flag flag = new Flag(this, team, teamsFlagLoc);
				
				teamToFlags.put(team, flag);
			}
		}
	}
	
	@Override
	public String mapPath() {
		return super.mapPath() + "CTF";
	}
}

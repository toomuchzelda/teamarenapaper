package me.toomuchzelda.teamarenapaper.teamarena.searchanddestroy;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.*;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import me.toomuchzelda.teamarenapaper.utils.BlockUtils;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.BlockVector;
import org.intellij.lang.annotations.RegExp;

import java.io.File;
import java.util.*;

/**
 * Search and destroy implementation class.
 *
 * @author toomuchzelda
 */
public class SearchAndDestroy extends TeamArena
{
	//record it here from the map config but won't use it for anything
	protected boolean randomBases = false;
	//initialised in parseConfig
	protected Map<TeamArenaTeam, List<Bomb>> teamBombs;
	protected Map<BlockVector, Bomb> bombPositions;
	protected Map<TNTPrimed, Bomb> bombTNTs;

	protected final ItemStack BASE_FUSE;
	public static final Component FUSE_NAME = ItemUtils.noItalics(Component.text("Bomb Fuse", NamedTextColor.GOLD));
	public static final List<Component> FUSE_LORE;
	public static final int SPAM_PERIOD = 4 * 20;


	//===========MESSAGE STUFF
	@RegExp
	public static final String BOMB_TEAM_KEY = "%bombTeam%";
	@RegExp
	public static final String ARMING_TEAM_KEY = "%armingTeam%";

	public static final Component TEAM_ARMED_TEAM_MESSAGE;
	public static final Component TEAM_ARMED_TEAM_TITLE;

	public static final Component TEAM_DEFUSED_MESSAGE = Component.text(BOMB_TEAM_KEY + " has just defused their bomb!", NamedTextColor.GOLD);
	public static final Component TEAM_DEFUSED_TITLE = Component.text(BOMB_TEAM_KEY + " defused their bomb!", NamedTextColor.GOLD);

	public static final Component TEAM_EXPLODED_MESSAGE;
	public static final Component TEAM_EXPLODED_TITLE;
	//===========MESSAGE STUFF

	static {
		FUSE_LORE = new ArrayList<>(2);
		FUSE_LORE.add(ItemUtils.noItalics(Component.text("Hold right click on a team's bomb to arm it", TextUtils.RIGHT_CLICK_TO)));
		FUSE_LORE.add(ItemUtils.noItalics(Component.text("Hold right click on your own bomb to disarm it", TextUtils.RIGHT_CLICK_TO)));

		Component obfuscated = Component.text("ab", NamedTextColor.GOLD, TextDecoration.OBFUSCATED);

		TEAM_ARMED_TEAM_MESSAGE = Component.text().append(
				obfuscated,
				Component.text(" " + ARMING_TEAM_KEY + " has just armed " + BOMB_TEAM_KEY + "'s bomb! ",
						NamedTextColor.GOLD),
				obfuscated
		).build();
		TEAM_ARMED_TEAM_TITLE = Component.text().append(
				obfuscated,
				Component.text(" " + ARMING_TEAM_KEY + " armed " + BOMB_TEAM_KEY + "'s bomb! ", NamedTextColor.GOLD),
				obfuscated
		).build();


		TEAM_EXPLODED_MESSAGE = Component.text().append(
				obfuscated,
				Component.text(" " + BOMB_TEAM_KEY + "'s bomb has exploded! ", NamedTextColor.GOLD),
				obfuscated
		).build();
		TEAM_EXPLODED_TITLE = Component.text().append(
				obfuscated,
				Component.text(" " + BOMB_TEAM_KEY + "'s bomb exploded! ", NamedTextColor.GOLD),
				obfuscated
		).build();
	}

	public SearchAndDestroy() {
		super();

		for(List<Bomb> bombsList : teamBombs.values()) {
			for(Bomb bomb : bombsList) {
				bomb.init();
			}
		}

		this.BASE_FUSE = new ItemStack(Material.BLAZE_POWDER);
		ItemMeta meta = BASE_FUSE.getItemMeta();
		meta.displayName(FUSE_NAME);
		meta.lore(FUSE_LORE);
		BASE_FUSE.setItemMeta(meta);

		this.bombTNTs = new HashMap<>();
	}

	public void liveTick() {
		int currentTick = getGameTick();
		for(Bomb bomb : bombPositions.values()) {
			bomb.tick();

			//if armed now announce it and all
			int armedTime = bomb.getArmedTime();
			if(armedTime == currentTick) {
				announceBombEvent(bomb, BOMB_EVENT_ARMED);
				//record the tnt block so can check it in entityInteract events
				this.bombTNTs.put(bomb.getTNT(), bomb);
			}
			//just been disarmed, remove TNT from map
			else if(armedTime == Bomb.JUST_BEEN_DISARMED) {
				announceBombEvent(bomb, BOMB_EVENT_DISARMED);
				this.bombTNTs.remove(bomb.getTNT());
			}
			else if(armedTime == Bomb.JUST_EXPLODED) {
				announceBombEvent(bomb, BOMB_EVENT_EXPLODED);
				//leave it in the Map so we can cancel the DamageEvents from it
				//this.bombTNTs.remove(bomb.getTNT());
			}
		}

		super.liveTick();
	}

	public void onInteract(PlayerInteractEvent event) {
		super.onInteract(event);
		if(event.useItemInHand() != Event.Result.DENY) {
			if(gameState == GameState.LIVE && event.getMaterial() == BASE_FUSE.getType() &&
					event.getAction().isRightClick() &&
					event.getClickedBlock() != null) {

				Block block = event.getClickedBlock();
				if(block.getType() == Material.TNT) {
					// check if team tnt or enemy tnt
					BlockVector blockLocation = block.getLocation().toVector().toBlockVector();
					Bomb clickedBomb = bombPositions.get(blockLocation);
					if(clickedBomb != null) {
						event.setUseItemInHand(Event.Result.DENY);
						PlayerInfo pinfo = Main.getPlayerInfo(event.getPlayer());
						if(clickedBomb.getTeam() == pinfo.team) {
							if(!clickedBomb.isArmed()) {
								final String key = "sndDisarmOwnBomb";
								if(pinfo.messageHasCooldowned(key, SPAM_PERIOD)) {
									event.getPlayer().sendMessage(Component.text("You cannot arm your own bomb!", TextUtils.ERROR_RED));
								}
							}
							//else Shouldn't be able to interact with the bomb via this method while it's armed
							// as there is no block, just TNT entity. Stil check that !bomb.isArmed() as it's possible
							// some delayed packet can cause the event to be called. Better safe than sorry I suppose.
						}
						else {
							if(!clickedBomb.isArmed()) {
								clickedBomb.addClicker(pinfo.team, event.getPlayer(), getGameTick(), Bomb.getArmProgressPerTick(event.getItem()));
							}
							//else handled in interactEntity
						}
					}
				}
			}
		}
	}

	@Override
	public void onInteractEntity(PlayerInteractEntityEvent event) {
		Player clicker = event.getPlayer();
		//get the item they used and check if it's a fuse
		ItemStack item = clicker.getEquipment().getItem(event.getHand());
		if(item != null && BASE_FUSE.getType() == item.getType()) {
			Bomb clickedBomb = bombTNTs.get(event.getRightClicked());
			if(clickedBomb != null) {
				PlayerInfo pinfo = Main.getPlayerInfo(clicker);
				if(clickedBomb.getTeam() == pinfo.team) {
					if(clickedBomb.isArmed()) {
						clickedBomb.addClicker(pinfo.team, clicker, getGameTick(), Bomb.getArmProgressPerTick(item));
					}
					//else Shouldn't be possible to interact with bomb as a TNT in disarmed state.
				}
				else {
					if(clickedBomb.isArmed()) {
						final String key = "sndDisarmEnemyBomb";
						if(pinfo.messageHasCooldowned(key, SPAM_PERIOD)) {
							event.getPlayer().sendMessage(Component.text("You cannot disarm an enemy's bomb!", TextUtils.ERROR_RED));
						}
					}
				}
			}

		}
	}

	@Override
	public void onDamage(DamageEvent event) {
		super.onDamage(event);

		if(event.getAttacker() instanceof TNTPrimed tnt && bombTNTs.containsKey(tnt)) {
			event.setCancelled(true);
		}
	}

	@Override
	protected void givePlayerItems(Player player, PlayerInfo pinfo, boolean clear) {
		//need to clear and give the fuse first to put it in 1st slot
		player.getInventory().clear();
		player.getInventory().addItem(BASE_FUSE);
		super.givePlayerItems(player, pinfo, false);
	}

	public static final byte BOMB_EVENT_ARMED = 0;
	public static final byte BOMB_EVENT_DISARMED = 1;
	public static final byte BOMB_EVENT_EXPLODED = 2;

	public static void announceBombEvent(Bomb bomb, byte bombEvent) {
		final Component message;
		final Component title;
		//sounds to play and the pitches to play them at
		final Sound[] sounds = new Sound[2]; // 0 = sound heard by bomb team, 1 = sound heard by others
		final float[] pitches = new float[2];
		final float volume = 99f;
		final TextReplacementConfig bombTeamConfig;

		if(bombEvent == BOMB_EVENT_ARMED) {
			TeamArenaTeam armingTeam = bomb.getArmingTeam();

			bombTeamConfig = TextReplacementConfig.builder().match(BOMB_TEAM_KEY).replacement(bomb.getTeam().getComponentName()).build();
			final TextReplacementConfig armingTeamConfig = TextReplacementConfig.builder().match(ARMING_TEAM_KEY).replacement(armingTeam.getComponentName()).build();

			message = TEAM_ARMED_TEAM_MESSAGE.replaceText(bombTeamConfig).replaceText(armingTeamConfig);
			title = TEAM_ARMED_TEAM_TITLE.replaceText(bombTeamConfig).replaceText(armingTeamConfig);

			sounds[0] = Sound.ENTITY_BLAZE_DEATH;
			sounds[1] = Sound.ENTITY_LIGHTNING_BOLT_THUNDER;
			pitches[0] = 0.5f;
			pitches[1] = 1f;
		}
		else if(bombEvent == BOMB_EVENT_DISARMED) {
			bombTeamConfig = TextReplacementConfig.builder().match(BOMB_TEAM_KEY)
					.replacement(bomb.getTeam().getComponentName()).build();

			message = TEAM_DEFUSED_MESSAGE.replaceText(bombTeamConfig);
			title = TEAM_DEFUSED_TITLE.replaceText(bombTeamConfig);

			sounds[0] = Sound.ENTITY_AXOLOTL_SPLASH;
			sounds[1] = Sound.BLOCK_BAMBOO_HIT;
			pitches[0] = 1f;
			pitches[1] = 0.5f;
		}
		else {
			bombTeamConfig = TextReplacementConfig.builder().match(BOMB_TEAM_KEY)
					.replacement(bomb.getTeam().getComponentName()).build();

			message = TEAM_EXPLODED_MESSAGE.replaceText(bombTeamConfig);
			title = TEAM_EXPLODED_TITLE.replaceText(bombTeamConfig);

			sounds[0] = null;
			sounds[1] = null;
		}

		Bukkit.broadcast(message);
		//play sounds and put title on screens
		var iter = Main.getPlayersIter();
		while(iter.hasNext()) {
			var entry = iter.next();
			Player receiver = entry.getKey();
			PlayerInfo pinfo = entry.getValue();
			if(pinfo.getPreference(Preferences.RECEIVE_GAME_TITLES)) {
				PlayerUtils.sendTitle(receiver, Component.empty(), title, 10, 40, 10);
			}

			int idx;
			if(pinfo.team == bomb.getTeam()) {
				idx = 0;
			}
			else {
				idx = 1;
			}

			if(sounds[idx] != null)
				receiver.playSound(receiver.getLocation(), sounds[idx], SoundCategory.AMBIENT, volume, pitches[idx]);
		}
	}

	@Override
	public void parseConfig(Map<String, Object> map) {
		super.parseConfig(map);

		Map<String, Object> customFlags = (Map<String, Object>) map.get("Custom");

		Main.logger().info("Custom Info: ");
		Main.logger().info(customFlags.toString());

		this.teamBombs = new HashMap<>(customFlags.size());
		this.bombPositions = new HashMap<>();
		for (Map.Entry<String, Object> entry : customFlags.entrySet()) {
			if (entry.getKey().equalsIgnoreCase("Random Base")) {
				try {
					randomBases = (boolean) entry.getValue();
				} catch (NullPointerException | ClassCastException e) {
					//do nothing
				}
			}
			else {
				TeamArenaTeam team = getTeamByRWFConfig(entry.getKey());
				if (team == null) {
					throw new IllegalArgumentException("Unknown team " + entry.getKey() + " Use BLUE or RED etc.(proper support coming later)");
				}

				List<String> configBombs = (List<String>) entry.getValue();
				List<Bomb> bombs = new ArrayList<>(configBombs.size());
				for(String bombCoords : configBombs) {
					BlockVector blockVector = BlockUtils.parseCoordsToVec(bombCoords, 0, 0, 0).toBlockVector();
					Bomb bomb = new Bomb(team, blockVector.toLocation(this.gameWorld));
					bombs.add(bomb);

					Main.logger().info("Adding " + bomb.toString());

					if(bombPositions.put(blockVector, bomb) != null) {
						throw new IllegalArgumentException("Two bombs are in the same position! Check the map's config.yml");
					}
				}

				teamBombs.put(team, bombs);
			}
		}
	}

	/**
	 * For compatibility with RWF 2 snd map config.yml
	 */
	protected TeamArenaTeam getTeamByRWFConfig(String name) {
		int spaceInd = name.indexOf(' ');
		name = name.substring(0, spaceInd);
		for(TeamArenaTeam team : teams) {
			if(team.getSimpleName().toLowerCase().replace(' ', '_').equals(name.toLowerCase())) {
				return team;
			}
		}

		return null;
	}

	@Override
	public void updateSidebar(Player player, SidebarManager sidebar) {
		//TODO
	}

	@Override
	public boolean canSelectKitNow() {
		return this.gameState.isPreGame();
	}

	@Override
	public boolean canSelectTeamNow() {
		return this.gameState == GameState.PREGAME;
	}

	@Override
	public boolean isRespawningGame() {
		return false;
	}

	@Override
	public File getMapPath() {
		return new File(super.getMapPath(), "SND");
	}
}

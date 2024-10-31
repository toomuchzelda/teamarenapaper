package me.toomuchzelda.teamarenapaper.teamarena.searchanddestroy;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.scoreboard.PlayerScoreboard;
import me.toomuchzelda.teamarenapaper.teamarena.*;
import me.toomuchzelda.teamarenapaper.teamarena.announcer.AnnouncerManager;
import me.toomuchzelda.teamarenapaper.teamarena.announcer.AnnouncerSound;
import me.toomuchzelda.teamarenapaper.teamarena.commands.CommandDebug;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.teamarena.gamescheduler.TeamArenaMap;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import me.toomuchzelda.teamarenapaper.utils.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.util.Ticks;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.map.MapCursor;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;
import org.intellij.lang.annotations.RegExp;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.File;
import java.text.DecimalFormat;
import java.util.*;

/**
 * Search and destroy implementation class.
 *
 * @author toomuchzelda
 */
public class SearchAndDestroy extends TeamArena
{
	public static final Component GAME_NAME = Component.text("Search and Destroy", NamedTextColor.GOLD);
	public static final Component HOW_TO_PLAY = Component.text("Arm and detonate other team's bombs or kill every enemy to win! Remember: there's no respawning, so play carefully!", NamedTextColor.GOLD);
	public static final @NotNull TextComponent LAST_TO_STAND = Component.text("Last to stand", NamedTextColor.GRAY);
	public static final int POISON_ANNOUNCEMENT = 60 * 20;

	//record it here from the map config but won't use it for anything
	protected boolean randomBases = false;
	//initialised in loadConfig
	protected Map<TeamArenaTeam, List<Bomb>> teamBombs;
	protected Map<BlockVector, Bomb> bombPositions;
	protected Map<TNTPrimed, Bomb> bombTNTs;

	// Minecraft scoreboard teams for the bomb entities to be put on for visual effects
	protected Map<TeamArenaTeam, Team> bombVisualTeams;

	public static final int POISON_TIME = 60 * 5 * 20;
	//starts counting down when game is live
	// is increased by player death and bomb arms
	protected int poisonTimeLeft = POISON_TIME;
	protected int bombAddPoison = POISON_ANNOUNCEMENT;
	protected boolean isPoison = false;
	//keeps track of which player to next strike with lightning
	protected ArrayList<Player> poisonVictims;
	protected int poisonRingIndex = 0;
	protected double poisonDamage = 1d;
	protected final double midToFurthestBombDistance;
	protected Firework poisonFirework;

	protected final ItemStack BASE_FUSE;
	public static final Component FUSE_NAME = ItemUtils.noItalics(Component.text("Bomb Fuse", NamedTextColor.GOLD));
	public static final String FUSE_ENCHANT_NAME = "Bomb Technician";
	private static final List<Component> FUSE_LORE = List.of(
        ItemUtils.noItalics(Component.text("Hold right click on a team's bomb to arm it", TextUtils.RIGHT_CLICK_TO)),
        ItemUtils.noItalics(Component.text("Hold right click on your own bomb to disarm it", TextUtils.RIGHT_CLICK_TO))
    );
	public static final int SPAM_PERIOD = 4 * 20;

	public static final int TEAM_DEAD_SCORE = 1;
	public static final int TEAM_LASTMAN_SCORE = 2;

	//sidebar
	private final Map<TeamArenaTeam, SidebarManager.SidebarEntry> sidebarCache;

	private static final DamageType FUSE_KILL = new DamageType(DamageType.MELEE, "%Killed% was pummelled to death by %Killer%'s Bomb Fuse");

	//===========MESSAGE STUFF
	//<editor-fold description="Messages">
	public static final Component SHORT_OBFUSCATED = Component.text("ab", NamedTextColor.GOLD, TextDecoration.OBFUSCATED);
	public static final Component LONG_OBFUSCATED = Component.text("Herobrine", NamedTextColor.DARK_RED, TextDecoration.OBFUSCATED);


	@RegExp
	public static final String BOMB_TEAM_KEY = "%bombTeam%";
	@RegExp
	public static final String ARMING_TEAM_KEY = "%armingTeam%";

	public static final Component TEAM_ARMED_TEAM_MESSAGE = Component.textOfChildren(
        SHORT_OBFUSCATED,
            Component.text(" " + ARMING_TEAM_KEY + " has just armed " + BOMB_TEAM_KEY + "'s bomb! ",
                    NamedTextColor.GOLD),
        SHORT_OBFUSCATED
    );
	public static final Component TEAM_ARMED_TEAM_TITLE = Component.textOfChildren(
        SHORT_OBFUSCATED,
            Component.text(" " + ARMING_TEAM_KEY + " armed " + BOMB_TEAM_KEY + "'s bomb! ", NamedTextColor.GOLD),
        SHORT_OBFUSCATED
    );

	public static final Component TEAM_DEFUSED_MESSAGE = Component.text(BOMB_TEAM_KEY + " has just defused their bomb!", NamedTextColor.GOLD);
	public static final Component TEAM_DEFUSED_TITLE = Component.text(BOMB_TEAM_KEY + " defused their bomb!", NamedTextColor.GOLD);

	public static final Component TEAM_EXPLODED_MESSAGE = Component.textOfChildren(
        SHORT_OBFUSCATED,
            Component.text(" " + BOMB_TEAM_KEY + "'s bomb has exploded! ", NamedTextColor.GOLD),
        SHORT_OBFUSCATED
    );
	public static final Component TEAM_EXPLODED_TITLE = Component.textOfChildren(
        SHORT_OBFUSCATED,
            Component.text(" " + BOMB_TEAM_KEY + "'s bomb exploded! ", NamedTextColor.GOLD),
        SHORT_OBFUSCATED
    );

	public static final Component TEAM_DEFEATED_MESSAGE = Component.text(BOMB_TEAM_KEY + " has been defeated!", NamedTextColor.GOLD);

	public static final Component MIN_TO_POISON_MESSAGE = Component.text("One minute until ", NamedTextColor.DARK_RED).append(LONG_OBFUSCATED).append(Component.text(" appears.", NamedTextColor.DARK_RED));
	public static final Component MIN_TO_POISON_TITLE = Component.text("One minute until ", NamedTextColor.DARK_RED).append(LONG_OBFUSCATED);

	public static final Component POISON_NOW_TITLE = Component.text("abcdefghijklmnopqrstuvwxyz12345", NamedTextColor.DARK_RED, TextDecoration.OBFUSCATED);
	public static final Component POISON_NOW_MESSAGE = Component.join(JoinConfiguration.newlines(), Collections.nCopies(5, POISON_NOW_TITLE));
	//</editor-fold>
	//===========END MESSAGE STUFF

    public SearchAndDestroy(TeamArenaMap map) {
		super(map);

		initBombVisualTeams(); // init before init'ing Bombs as Bombs will put themselves onto visual teams.

		double longest = 0d;
		for(List<Bomb> bombsList : teamBombs.values()) {
			for(Bomb bomb : bombsList) {
				bomb.init(bombVisualTeams.get(bomb.getTeam()));
				double distance = bomb.getSpawnLoc().distance(this.spawnPos);
				longest = Math.max(distance, longest);
			}
		}
		this.midToFurthestBombDistance = longest;

		this.BASE_FUSE = new ItemStack(Material.BLAZE_POWDER);
		ItemMeta meta = BASE_FUSE.getItemMeta();
		meta.displayName(FUSE_NAME);
		meta.lore(FUSE_LORE);
		BASE_FUSE.setItemMeta(meta);

		this.bombTNTs = new HashMap<>();
		this.poisonVictims = null;

		this.sidebarCache = new LinkedHashMap<>(teams.length);
	}

	public void prepLive() {
		super.prepLive();

		for(TeamArenaTeam team : teams) {
			team.bossBar.progress(1f);
		}
	}

	@Override
	public void setupMiniMap() {
		teamBombs.forEach((team, bombs) -> {
			MapCursor.Type icon = MapCursor.Type.valueOf("BANNER_" + team.getDyeColour().name());
			var bombText = Component.text(team.getSimpleName() + "'s bomb", team.getRGBTextColor());
			miniMap.registerCursors(
				(ignored1, ignored2) -> CommandDebug.ignoreWinConditions || gameState == GameState.PREGAME || team.hasLivingOrRespawningMembers(),
				null,
				(player, playerInfo) -> bombs.stream()
					.map(bomb -> new MiniMapManager.CursorInfo(
						bomb.getSpawnLoc(), false, icon,
						playerInfo.team == team ?
							Component.text("Your bomb", team.getRGBTextColor()) :
							bombText))
					.toArray(MiniMapManager.CursorInfo[]::new)
			);
		});
	}

	@Override
	public void prepDead() {
		super.prepDead();

		for (Team visualTeam : bombVisualTeams.values()) {
			PlayerScoreboard.removeGlobalTeam(visualTeam);
			visualTeam.unregister();
		}
	}

	public void liveTick() {
		final int currentTick = getGameTick();

		for(Map.Entry<TeamArenaTeam, List<Bomb>> entry : teamBombs.entrySet()) {
			TeamArenaTeam team = entry.getKey();

			int soonestExplodingBomb = 0;
			for(Bomb bomb : entry.getValue()) {
				bomb.tick();

				//if armed now announce it and all
				int armedTime = bomb.getArmedTime();
				if(armedTime == currentTick) {
					announceBombEvent(bomb, BombEvent.ARMED);
					//record the tnt block so can check it in entityInteract events
					this.bombTNTs.put(bomb.getTNT(), bomb);
					this.poisonTimeLeft += this.bombAddPoison;
					this.bombAddPoison /= 2;

					// Remove the last man standing enchantment if they have it.
					if(bomb.getArmingTeam().score == TEAM_LASTMAN_SCORE) {
						for(Player armer : bomb.getResponsibleArmers()) {
							// Check isDead as someone may have died immediately before, and may remain in the
							// bomb as a "clicker" for a short time.
							if(!isDead(armer)) {
								administerInstantFuse(armer, false);
							}
						}
					}

					// Announcer sound
					AnnouncerManager.broadcastSound(AnnouncerSound.GAME_BOMB_ARMED);
				}
				//just been disarmed, remove TNT from map
				else if(armedTime == Bomb.JUST_BEEN_DISARMED) {
					announceBombEvent(bomb, BombEvent.DISARMED);
					this.bombTNTs.remove(bomb.getTNT());

					// Remove last man standing ench.
					if(bomb.getTeam().score == TEAM_LASTMAN_SCORE) {
						for(Player disarmer : bomb.getResponsibleArmers()) {
							// Check isDead as someone may have died immediately before, and may remain in the
							// bomb as a "clicker" for a short time.
							if(!isDead(disarmer)) {
								administerInstantFuse(disarmer, false);
							}
						}
					}

					AnnouncerManager.broadcastSound(AnnouncerSound.GAME_BOMB_DISARMED);
				}
				else if(armedTime == Bomb.JUST_EXPLODED) {
					announceBombEvent(bomb, BombEvent.EXPLODED);
					//leave the TNT in the Map so we can cancel the DamageEvents from it

					for(Player loser : bomb.getTeam().getPlayerMembers()) {
						DamageEvent death = DamageEvent.newDamageEvent(loser, 1d, DamageType.BOMB_EXPLODED, null, false);
						this.queueDamage(death);
					}

					//score 1 for dead
					bomb.getTeam().score = TEAM_DEAD_SCORE;

					AnnouncerManager.broadcastSound(AnnouncerSound.GAME_BOMB_DETONATED);
				}
				else if(bomb.isArmed()) {
					soonestExplodingBomb = Math.max(soonestExplodingBomb, bomb.getArmedTime());
				}
			}

			//calculate bossbar progress and display it
			if(soonestExplodingBomb == 0) {
				team.bossBar.progress(1f);
			}
			else {
				int ticksPassed = currentTick - soonestExplodingBomb;
				ticksPassed = Bomb.BOMB_DETONATION_TIME - ticksPassed;
				float progressLeft = (float) ticksPassed / (float) Bomb.BOMB_DETONATION_TIME;
				progressLeft = MathUtils.clamp(0f, progressLeft, 1f);
				team.bossBar.progress(progressLeft);
			}
		}

		super.liveTick();

		if(this.winningTeam != null) { //possibly assigned by checkWinner() call during the damage tick.
			this.prepEnd();
		}
		else {
			//if no winner run end game poison
			if(gameState == GameState.LIVE) {
				poisonTick();
			}
			this.checkWinner();
		}
	}

	/**
	 * Check if there is one team left alive and end the game
	 */
	public void checkWinner() {
		TeamArenaTeam winnerTeam = null;
		int aliveTeamCount = 0;
		//check if any alive teams have all members dead
		for(TeamArenaTeam team : teams) {
			//skip if already dead
			if(team.score == TEAM_DEAD_SCORE)
				continue;

			int aliveTeamMemberCount = 0;
			Player lastMan = null; // if only 1 player left this should be that one player
			for(Player teamMember : team.getPlayerMembers()) {
				if(!isSpectator(teamMember)) {
					aliveTeamMemberCount++;
					lastMan = teamMember;
				}
			}

			if(aliveTeamMemberCount == 0) {
				for (Bomb teamBomb : teamBombs.get(team)) {
					teamBomb.setGrave();
				}

				team.score = TEAM_DEAD_SCORE;

				final TextReplacementConfig defeatedConfig = TextReplacementConfig.builder().match(BOMB_TEAM_KEY).replacement(team.getComponentName()).build();
				Component message = TEAM_DEFEATED_MESSAGE.replaceText(defeatedConfig);
				Bukkit.broadcast(message);
			}
			else {
				aliveTeamCount++;
				winnerTeam = team;
				// announce the last man standing.
				if(aliveTeamMemberCount == 1 && team.score != TEAM_LASTMAN_SCORE) {
					team.score = TEAM_LASTMAN_SCORE; // Use this to ensure this code block only runs once
					announceLastManStanding(lastMan, team, this.spawnPos);
					administerInstantFuse(lastMan, true);
				}
			}
		}

		if(!CommandDebug.ignoreWinConditions && winnerTeam != null && aliveTeamCount == 1) {
			//this method may be called during a damage tick, so signal to end game later instead by assigning
			// winningTeam
			this.winningTeam = winnerTeam;
		}
	}

	public static void announceLastManStanding(final Player lastMan, final TeamArenaTeam team, Location spawnPos) {
		final Component lastStanding = Component.text(" is last standing!", team.getRGBTextColor());
		final Component message = lastMan.playerListName().append(lastStanding);

		Bukkit.broadcast(message);
		PlayerUtils.sendOptionalTitle(Component.empty(), message, 10, 20, 10);

		lastMan.getWorld().playSound(spawnPos, Sound.ENTITY_ENDER_DRAGON_HURT, 99999f, 1f);
		AnnouncerManager.playSound(lastMan, AnnouncerSound.GAME_LAST_MAN_STANDING);
	}

	private void administerInstantFuse(final Player player, boolean give) {
		List<ItemStack> playersFuses = ItemUtils.getItemsInInventory(BASE_FUSE.getType(), player.getInventory());

		final int kitFuseLevel = Main.getPlayerInfo(player).activeKit.getFuseEnchantmentLevel();
		if(give) {
			playersFuses.forEach(fuse -> setFuseEnchantmentLevel(fuse, 10));
			player.sendMessage(Component.text()
					.append(Component.text("You get a ", NamedTextColor.GOLD))
					.append(Component.text("one time use", NamedTextColor.GOLD, TextDecoration.UNDERLINED))
					.append(Component.text(" Bomb Technician X ", NamedTextColor.RED, TextDecoration.BOLD))
					.append(Component.text("fuse!", NamedTextColor.GOLD)));
		}
		else {
			playersFuses.forEach(fuse -> setFuseEnchantmentLevel(fuse, kitFuseLevel));
		}
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
									event.getPlayer().sendMessage(Component.text("You cannot arm your own bomb!", TextColors.ERROR_RED));
								}
							}
							//else Shouldn't be able to interact with the bomb via this method while it's armed
							// as there is no block, just TNT entity. Stil check that !bomb.isArmed() as it's possible
							// some delayed packet can cause the event to be called. Better safe than sorry I suppose.
						}
						else {
							if(!clickedBomb.isArmed()) {
								clickedBomb.addClicker(pinfo.team, event.getPlayer(), getGameTick(), this.getArmProgressPerTick(event.getItem()));
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
		super.onInteractEntity(event);
		Player clicker = event.getPlayer();
		//get the item they used and check if it's a fuse
		ItemStack item = clicker.getEquipment().getItem(event.getHand());
		if(item != null && BASE_FUSE.getType() == item.getType()) {
			Bomb clickedBomb = bombTNTs.get(event.getRightClicked());
			if(clickedBomb != null) {
				PlayerInfo pinfo = Main.getPlayerInfo(clicker);
				if(clickedBomb.getTeam() == pinfo.team) {
					if(clickedBomb.isArmed()) {
						clickedBomb.addClicker(pinfo.team, clicker, getGameTick(), this.getArmProgressPerTick(item));
					}
					//else Shouldn't be possible to interact with bomb as a TNTPrimed in disarmed state.
				}
				else {
					if(clickedBomb.isArmed()) {
						final String key = "sndDisarmEnemyBomb";
						if(pinfo.messageHasCooldowned(key, SPAM_PERIOD)) {
							event.getPlayer().sendMessage(Component.text("You cannot disarm an enemy's bomb!", TextColors.ERROR_RED));
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
		else if(event.getAttacker() instanceof Firework firework && firework == poisonFirework) {
			event.setCancelled(true);
		}
	}

	public void poisonTick() {
		final int currentTick = getGameTick();
		this.poisonTimeLeft--;
		if (this.poisonTimeLeft <= 0) {
			if (!isPoison) {
				this.isPoison = true;
				this.poisonVictims = new ArrayList<>(this.getPlayers());
				Bukkit.broadcast(POISON_NOW_MESSAGE);
				PlayerUtils.sendOptionalTitle(Component.empty(), POISON_NOW_TITLE, 0, 30, 30);

				//teleport everyone to the middle
				Location specPos = spawnPos.clone().add(0, 5, 0).setDirection(new Vector(0, -1, 0));
				for (Player p : Bukkit.getOnlinePlayers()) {
					if (isSpectator(p)) {
						p.teleport(specPos);
					}
					else {
						removeHealing(p);

						Location toTele = spawnPos.clone().add(MathUtils.randomRange(-2d, 2d), 0, MathUtils.randomRange(-2d, 2d));
						toTele.setDirection(spawnPos.toVector().subtract(toTele.toVector()).normalize());

						boolean isFinite = false;
						while(!isFinite) {
							try {
								toTele.checkFinite();
								isFinite = true;
							}
							catch(IllegalArgumentException e) {
								toTele = spawnPos.clone().add(MathUtils.randomRange(-2d, 2d), 0, MathUtils.randomRange(-2d, 2d));
							}
						}

						p.teleport(toTele);
					}
				}
			}
		}
		else if (this.poisonTimeLeft == POISON_ANNOUNCEMENT) {
			Bukkit.broadcast(MIN_TO_POISON_MESSAGE);
			PlayerUtils.sendOptionalTitle(Component.empty(), MIN_TO_POISON_TITLE, 30, 30, 30);

			//move any players out of the way before building
			spawnPos.getNearbyPlayers(2, 3)
					.forEach(player -> EntityUtils.setVelocity(player, player.getLocation().toVector().subtract(spawnPos.toVector()).normalize().multiply(0.3d)));

			buildShrine(spawnPos.clone().subtract(0, 2, 0));

			spawnPos.getWorld().strikeLightningEffect(spawnPos.clone().add(0, 2, 0));
		}
		else if (this.poisonTimeLeft < POISON_ANNOUNCEMENT || isPoison) {
			if (currentTick % (10 * 20) == 0) {
				Firework firework = (Firework) gameWorld.spawnEntity(this.spawnPos.clone().add(0, 1, 0), EntityType.FIREWORK_ROCKET);
				FireworkMeta meta = firework.getFireworkMeta();
				meta.setPower(3);
				meta.clearEffects();
				meta.addEffect(FireworkEffect.builder().trail(true).with(FireworkEffect.Type.BALL_LARGE).withColor(TeamArenaTeam.convert(NamedTextColor.DARK_RED))
						.withFade(TeamArenaTeam.convert(NamedTextColor.GOLD)).build());
				firework.setFireworkMeta(meta);

				//store it here so can cancel damage events it causes
				this.poisonFirework = firework;
			}
		}

		if (isPoison && !CommandDebug.ignoreWinConditions) {
			if (currentTick % 4 == 0) {
				int idx = ++poisonRingIndex % poisonVictims.size();
				if (idx == 0) {
					//increase lightning damage on every cycle
					poisonDamage += 0.75d;

					poisonVictims = new ArrayList<>(this.players);
					Collections.shuffle(poisonVictims, MathUtils.random);
				}

				Player unfortunateVictim = poisonVictims.get(idx);

				gameWorld.strikeLightningEffect(unfortunateVictim.getLocation());
				DamageEvent damage = DamageEvent.newDamageEvent(unfortunateVictim, poisonDamage, DamageType.END_GAME_LIGHTNING, null, false);
				queueDamage(damage);
			}
		}
	}

	@Override
	public void handleDeath(DamageEvent event) {
		// Fuse kills
		if (event.getFinalAttacker() instanceof LivingEntity killer &&
			event.getVictim() instanceof Player playerVictim &&
			event.getDamageType().is(DamageType.MELEE) &&
			event.getMeleeWeapon().getType() == BASE_FUSE.getType()) {

			event.setDamageType(FUSE_KILL);

			AnnouncerManager.playSound(playerVictim, AnnouncerSound.CHAT_MY_GOD);
			if (killer instanceof Player playerKiller)
				AnnouncerManager.playSound(playerKiller, AnnouncerSound.CHAT_DISGRACEFUL);
		}

		super.handleDeath(event);

		this.checkWinner();

		this.poisonTimeLeft += 10 * 20;
		if(isPoison && event.getVictim() instanceof Player p) {
			poisonVictims.remove(p);
		}
	}

	@Override
	protected void giveKitAndGameItems(Player player, PlayerInfo pinfo, boolean clear) {
		//need to clear and give the fuse first to put it in 1st slot
		if(clear)
			player.getInventory().clear();

		super.giveKitAndGameItems(player, pinfo, false);
		player.getInventory().addItem(createFuse(pinfo.kit.getFuseEnchantmentLevel()));
	}

	private ItemStack createFuse(int levels) {
		if(levels <= 0)
			return BASE_FUSE;

		ItemStack fuse = BASE_FUSE.clone();
		ItemMeta fuseMeta = fuse.getItemMeta();

		//we are storing the actual level in the unbreaking enchantment which should be hidden
		fuseMeta.addEnchant(Enchantment.UNBREAKING, levels, true);

		List<Component> lore = fuseMeta.lore();
		Component enchantLore = getFuseEnchantLore(levels);
		if(enchantLore != null)
			lore.add(0, getFuseEnchantLore(levels));
		fuseMeta.lore(lore);

		fuse.setItemMeta(fuseMeta);
		//hide enchantments so we can add the unbreaking enchantment to give it the glint, without it
		// appearing in the item lore.
		fuse.addItemFlags(ItemFlag.HIDE_ENCHANTS);

		return fuse;
	}

	public int getFuseEnchantmentLevel(ItemStack fuse) {
		if(fuse.getType() == BASE_FUSE.getType())
			return fuse.getEnchantmentLevel(Enchantment.UNBREAKING);
		else
			return 0;
	}

	public void setFuseEnchantmentLevel(ItemStack fuse, int level) {
		if(fuse.getType() != BASE_FUSE.getType()) {
			Main.logger().warning("SearchAndDestroy setFuseEnchantment called with non type-matching fuse");
			Thread.dumpStack();
			return;
		}

		ItemMeta fuseMeta = fuse.getItemMeta();

		// Update the lore
		if(!fuseMeta.hasLore()) {
			Main.logger().warning("setFuseEnchantment called with lore-less fuse");
			Thread.dumpStack();
			return;
		}

		List<Component> lore = fuseMeta.lore();
		if(fuseMeta.hasEnchant(Enchantment.UNBREAKING)) { //had bomb tech enchantment
			if(level == 0) {
				lore.remove(0);
			}
			else {
				lore.set(0, getFuseEnchantLore(level));
			}
		}
		else if (level > 0) {
			lore.add(0, getFuseEnchantLore(level));
		}

		// Add the enchantment.
		if(level == 0) {
			fuseMeta.removeEnchant(Enchantment.UNBREAKING);
		}
		else if (level > 0) {
			fuseMeta.addEnchant(Enchantment.UNBREAKING, level, true);
		}

		fuseMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		fuseMeta.lore(lore);
		fuse.setItemMeta(fuseMeta);
	}

	private @Nullable Component getFuseEnchantLore(int levels) {
		if(levels == 0)
			return null;

		//need roman numerals 1-10
		String roman;
		switch (levels) {
			case 1 -> roman = "I";
			case 2 -> roman = "II";
			case 3 -> roman = "III";
			case 4 -> roman = "IV";
			case 5 -> roman = "V";
			case 6 -> roman = "VI";
			case 7 -> roman = "VII";
			case 8 -> roman = "VIII";
			case 9 -> roman = "IX";
			case 10 -> roman = "X";
			default -> roman = "" + levels;
		}

		return Component.text(FUSE_ENCHANT_NAME + " " + roman, NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false);
	}

	/**
	 * Get how much per tick an item should arm a Bomb. Used for special fuse enchantments
	 * @param fuse The fuse.
	 * @return Arm percent per tick in range 0.0 to 1.0
	 */
	public float getArmProgressPerTick(ItemStack fuse) {
		if(fuse.getType() != BASE_FUSE.getType()) {
			Main.logger().warning("SearchAndDestroy.getArmProgressPerTick called with non-fuse ItemStack");
			Thread.dumpStack();
			return 0;
		}

		float level = (float) this.getFuseEnchantmentLevel(fuse);
		//Bukkit.broadcastMessage("" + level);
		level = MathUtils.clamp(0f, 1f, (1f + level) / 10f / 20f);
		return level;
	}

	protected void removeHealing(Player player) {
		PlayerInventory inventory = player.getInventory();
		inventory.remove(Material.GOLDEN_APPLE);
		inventory.remove(Material.ENCHANTED_GOLDEN_APPLE);
		inventory.remove(Material.COOKED_BEEF);
		inventory.remove(Material.FISHING_ROD);
		inventory.remove(Material.POTION);

		player.setAbsorptionAmount(0d);

		PotionEffect effect = player.getPotionEffect(PotionEffectType.REGENERATION);
		if(effect != null) {
			player.sendMessage(Component.text("Think regeneration will help you? Lose half your health and think twice!", NamedTextColor.DARK_RED));
			player.removePotionEffect(PotionEffectType.REGENERATION);
			EntityUtils.setMaxHealth(player, 10d);
		}
	}

	public static void buildShrine(Location baseLocation) {
		World world = baseLocation.getWorld();

		final int baseX = baseLocation.getBlockX();
		final int baseY = baseLocation.getBlockY();
		final int baseZ = baseLocation.getBlockZ();

		//clear out the surrounding space first
		for(int x = -2; x < 3; x++) {
			for(int y = 0; y < 4; y++) {
				for(int z = -2; z < 3; z++) {
					world.getBlockAt(baseX + x, baseY + y, baseZ + z).setType(Material.AIR);
				}
			}
		}

		for(int x = -1; x < 2; x++) {
			for(int z = -1; z < 2; z++) {
				world.getBlockAt(baseX + x, baseY, baseZ + z).setType(Material.GOLD_BLOCK);
			}
		}

		world.getBlockAt(baseX, baseY + 1, baseZ).setType(Material.NETHERRACK);
		world.getBlockAt(baseX, baseY + 2, baseZ).setType(Material.FIRE);

		for(int x = -1; x < 2; x += 2)
			world.getBlockAt(baseX + x, baseY + 1, baseZ).setType(Material.REDSTONE_TORCH);

		for(int z = -1; z < 2; z += 2)
			world.getBlockAt(baseX, baseY + 1, baseZ + z).setType(Material.REDSTONE_TORCH);
	}

	public static void announceBombEvent(Bomb bomb, BombEvent bombEvent) {
		final Component message;
		final Component title;
		//sounds to play and the pitches to play them at
		final Sound[] sounds = new Sound[2]; // 0 = sound heard by bomb team, 1 = sound heard by others
		final float[] pitches = new float[2];
		final float volume = 99f;
		final TextReplacementConfig bombTeamConfig = TextReplacementConfig.builder().match(BOMB_TEAM_KEY).replacement(bomb.getTeam().getComponentName()).build();;
		final TextReplacementConfig bombTeamTitleConfig = TextReplacementConfig.builder().match(BOMB_TEAM_KEY).replacement(bomb.getTeam().getComponentSimpleName()).build();

		if(bombEvent == BombEvent.ARMED) {
			TeamArenaTeam armingTeam = bomb.getArmingTeam();

			final TextReplacementConfig armingTeamConfig = TextReplacementConfig.builder().match(ARMING_TEAM_KEY).replacement(armingTeam.getComponentName()).build();
			final TextReplacementConfig armingTeamTitleConfig = TextReplacementConfig.builder().match(ARMING_TEAM_KEY).replacement(armingTeam.getComponentSimpleName()).build();

			message = TEAM_ARMED_TEAM_MESSAGE.replaceText(bombTeamConfig).replaceText(armingTeamConfig);
			title = TEAM_ARMED_TEAM_TITLE.replaceText(bombTeamTitleConfig).replaceText(armingTeamTitleConfig);

			sounds[0] = Sound.ENTITY_BLAZE_DEATH;
			sounds[1] = Sound.ENTITY_LIGHTNING_BOLT_THUNDER;
			pitches[0] = 0.5f;
			pitches[1] = 1f;
		}
		else if(bombEvent == BombEvent.DISARMED) {
			message = TEAM_DEFUSED_MESSAGE.replaceText(bombTeamConfig);
			title = TEAM_DEFUSED_TITLE.replaceText(bombTeamTitleConfig);

			sounds[0] = Sound.ENTITY_AXOLOTL_SPLASH;
			sounds[1] = Sound.BLOCK_BAMBOO_HIT;
			pitches[0] = 1f;
			pitches[1] = 0.5f;
		}
		else {
			message = TEAM_EXPLODED_MESSAGE.replaceText(bombTeamConfig);
			title = TEAM_EXPLODED_TITLE.replaceText(bombTeamTitleConfig);

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
				PlayerUtils.sendTitle(receiver, Component.empty(), title, 5, 40, 10);
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

	private void initBombVisualTeams() {
		this.bombVisualTeams = new HashMap<>();
		for (TeamArenaTeam team : teams) {
			final String teamName = team.getName() + "Bomb";
			Team bukkitTeam = PlayerScoreboard.SCOREBOARD.getTeam(teamName);
			if (bukkitTeam != null)
				bukkitTeam.unregister();

			bukkitTeam = PlayerScoreboard.SCOREBOARD.registerNewTeam(teamName);
			bukkitTeam.color(NamedTextColor.nearestTo(team.getRGBTextColor()));
			bukkitTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
			bukkitTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);

			bombVisualTeams.put(team, bukkitTeam);
			PlayerScoreboard.addGlobalTeam(bukkitTeam);
		}
	}

	@Override
	protected void loadConfig(TeamArenaMap map) {
		super.loadConfig(map);

		TeamArenaMap.SNDInfo sndConfig = map.getSndInfo();
		if(sndConfig == null) {
			throw new IllegalArgumentException("SearchAndDestroy constructor called with a non-SND map.");
		}

		this.teamBombs = new HashMap<>(sndConfig.teamBombs().size());
		this.bombPositions = new HashMap<>();

		this.randomBases = sndConfig.randomBases();
		for (Map.Entry<String, List<BlockVector>> entry : sndConfig.teamBombs().entrySet()) {
			TeamArenaTeam team = getTeamByLegacyConfigName(entry.getKey());
			if (team == null) {
				throw new IllegalArgumentException("Bad team " + entry.getKey() + " in SND map config");
			}

			List<BlockVector> bombVecs = entry.getValue();
			List<Bomb> bombs = new ArrayList<>(bombVecs.size());
			for(BlockVector bombLoc : bombVecs) {
				Bomb bomb = new Bomb(team, bombLoc.toLocation(this.gameWorld));
				bombs.add(bomb);

				Main.logger().info("Adding " + bomb.toString());

				if(bombPositions.put(bombLoc, bomb) != null) {
					throw new IllegalArgumentException("Two bombs are in the same position! Check the map's SNDConfig.yml");
				}
			}

			teamBombs.put(team, bombs);
		}
	}

	private static final DecimalFormat ONE_DP = new DecimalFormat("0.0");
	private static final String BOMB_EMOJI = "\uD83D\uDCA3"; // 💣
	@Override
	public Collection<Component> updateSharedSidebar() {
		this.sidebarCache.clear();
		int currentTick = TeamArena.getGameTick();
		for (var entry : teamBombs.entrySet()) {
			var team = entry.getKey();
			var bombs = entry.getValue();

			int playersAlive = 0;
			for (var player : team.getPlayerMembers()) {
				if (!isDead(player)) {
					playersAlive++;
				}
			}

			if (!CommandDebug.ignoreWinConditions && playersAlive == 0)
				continue;

			var builder = Component.text();
			builder.append(team.getComponentSimpleName(), Component.text(": "));
			Bomb bomb;
			if (bombs.size() == 1 && (bomb = bombs.get(0)).isArmed()) {
				int timeLeft = Bomb.BOMB_DETONATION_TIME - (currentTick - bomb.getArmedTime());
				// defusing
				if (bomb.getArmingTeams().contains(team) && bomb.getTeamArmingProgress(team) != 0) {
					builder.append(Component.text("✂ Defusing", team.getRGBTextColor()));// scissors emoji
				} else {
					var textColor = timeLeft % 40 < 20 ? NamedTextColor.RED : NamedTextColor.YELLOW;
					String timeLeftText = timeLeft >= 200 ?
							Integer.toString(timeLeft / 20) :
							ONE_DP.format(timeLeft / 20d);
					builder.append(Component.text(BOMB_EMOJI + " " + timeLeftText + "s", textColor));
				}
			} else if (bombs.stream().anyMatch(Bomb::isArmed)) {
				// weird map
				builder.append(Component.text(BOMB_EMOJI + " In danger", NamedTextColor.DARK_RED));
			} else {
				builder.append(Component.text("Safe", NamedTextColor.DARK_GREEN));
			}

			sidebarCache.put(team, new SidebarManager.SidebarEntry(builder.build(), Component.text(playersAlive + " alive")));
		}
		if (poisonTimeLeft >= POISON_ANNOUNCEMENT && !isPoison)
			return List.of(LAST_TO_STAND);
		Component herobrine = Component.text("???", NamedTextColor.DARK_RED);
		return List.of(LAST_TO_STAND,
			isPoison ?
				Component.textOfChildren(herobrine, Component.text(" ongoing", NamedTextColor.GRAY)) :
				Component.textOfChildren(herobrine,
					Component.text(" in "),
					TextUtils.formatDurationMmSs(Ticks.duration(poisonTimeLeft))
				)
		);
	}

	@Override
	public void updateSidebar(Player player, SidebarManager sidebar) {
		sidebar.setTitle(player, getGameName());
		TeamArenaTeam playerTeam = Main.getPlayerInfo(player).team;
		int teamsShown = 0;

		for (var entry : sidebarCache.entrySet()) {
			var team = entry.getKey();
			var line = entry.getValue();

			if (teamsShown >= 4 && team != playerTeam)
				continue; // don't show
			teamsShown++;
			if (team == playerTeam) {
				// blink red when flag picked up
				boolean inDanger = false;
				for (Bomb bomb : teamBombs.get(team)) {
					if (bomb.isArmed()) {
						inDanger = true;
						break;
					}
				}
				var teamPrefix = inDanger && TeamArena.getGameTick() % 20 < 10 ? OWN_TEAM_PREFIX_DANGER : OWN_TEAM_PREFIX;
				sidebar.addEntry(Component.textOfChildren(teamPrefix, line.text()), line.numberFormat());
			} else {
				sidebar.addEntry(line);
			}
		}
		// unimportant teams
		if (sidebarCache.size() != teamsShown)
			sidebar.addEntry(Component.empty(), Component.text("+ " + (sidebarCache.size() - teamsShown) + " teams", NamedTextColor.GRAY));
	}

	@Override
	public boolean isVandalisableBlock(Block block) {
		if (this.bombPositions.get(block.getLocation().toVector().toBlockVector()) == null) {
			return super.isVandalisableBlock(block);
		}
		else return false;
	}

	@Override
	public boolean canSelectKitNow(Player player) {
		return this.gameState.isPreGame();
	}

	@Override
	public boolean canSelectTeamNow() {
		return this.gameState == GameState.PREGAME;
	}

	@Override
	public boolean canTeamChatNow(Player player) {
		return (gameState == GameState.LIVE || gameState.teamsChosen()) && !isDead(player);
	}

	@Override
	public boolean isRespawningGame() {
		return false;
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
	public File getMapPath() {
		return new File(super.getMapPath(), "SND");
	}

	@Override
	public String getDebugAntiStall() {
		return """
   			*poisonTimeLeft: %d
   			POISON_ANNOUNCEMENT: %d""".formatted(poisonTimeLeft, POISON_ANNOUNCEMENT);
	}

	@Override
	public void setDebugAntiStall(int antiStallCountdown) {
		poisonTimeLeft = antiStallCountdown;
	}

	enum BombEvent {
		ARMED,
		DISARMED,
		EXPLODED
	}
}

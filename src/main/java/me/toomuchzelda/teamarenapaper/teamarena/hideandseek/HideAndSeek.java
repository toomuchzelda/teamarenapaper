package me.toomuchzelda.teamarenapaper.teamarena.hideandseek;

import me.toomuchzelda.teamarenapaper.CompileAsserts;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.metadata.MetaIndex;
import me.toomuchzelda.teamarenapaper.teamarena.*;
import me.toomuchzelda.teamarenapaper.teamarena.commands.CommandDebug;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.gamescheduler.TeamArenaMap;
import me.toomuchzelda.teamarenapaper.teamarena.killstreak.KillStreakManager;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.filter.FilterAction;
import me.toomuchzelda.teamarenapaper.teamarena.kits.filter.FilterRule;
import me.toomuchzelda.teamarenapaper.teamarena.kits.filter.KitFilter;
import me.toomuchzelda.teamarenapaper.teamarena.kits.hideandseek.KitHider;
import me.toomuchzelda.teamarenapaper.teamarena.kits.hideandseek.KitRadarSeeker;
import me.toomuchzelda.teamarenapaper.utils.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class HideAndSeek extends TeamArena {

	private static final Component GAME_NAME = Component.text("Hide and Seek", GameType.HNS.shortName.color());
	private static final Component HOW_TO_PLAY = Component.text(
		"Seekers have to find and kill the Hider King before time is up.",
		GameType.HNS.shortName.color()
	).append(Component.text(" Hiders don't respawn!", GameType.HNS.shortName.color(), TextDecoration.BOLD));
	private static final Component SEEKERS_RELEASED_CHAT = Component.text("Ready or not, here I come!", NamedTextColor.GOLD);
	private static final Component SEEKERS_RELEASED_TITLE = Component.text("Seekers released", NamedTextColor.GOLD);
	private static final Component SEEKERS_RELEASED_IN_CHAT = Component.text("Seekers will be released in ", NamedTextColor.GOLD);
	private static final Component PRESIDENT_DIED = Component.text("Hider King has been killed!", NamedTextColor.GOLD);
	private static final Component HIDERS_WIN = Component.text("Time's up!", NamedTextColor.GOLD, TextDecoration.BOLD);
	private static final Component NO_RESPAWNING = Component.text("Hiders don't respawn", TextColors.ERROR_RED);

	private static final double HIDER_SEEKER_RATIO = 2d; // 2 Hiders for every seeker

	private static final int DEFAULT_SEEK_TIME = 4 * 60 * 20; // 3 minutes?

	private static final FilterRule GLOBAL_RULE = new FilterRule("hns/global", "", FilterAction.allow("hider", "radar"));
	private static final FilterRule SEEKER_RULE = new FilterRule("hns/seeker_team", "Seeker team restrictions", FilterAction.allow("radar"));
	private static final FilterRule HIDER_RULE = new FilterRule("hns/hider_team", "Hider team restrictions", FilterAction.allow("hider"));

	public TeamArenaTeam hiderTeam;
	public TeamArenaTeam seekerTeam;

	private Location seekerSpawnLoc;
	private final int hideTimeTicks;
	private Set<Material> allowedBlocks;
	private Set<EntityType> allowedEntities;

	private final ArrayList<BlockCoords> allowedBlockCoords;

	private boolean isHidingTime;
	private int remainingSeekTime; // Assigned in prepLive

	private Player president;
	private Set<Player> presidentSet; // cache

	private final Kit hiderKit;
	private final Kit seekerKit;

	public HideAndSeek(TeamArenaMap map) {
		super(map);

		this.gameWorld.setGameRule(GameRule.RANDOM_TICK_SPEED, 0);

		for (TeamArenaTeam team : this.teams) {
			if (team.getName().equals("Hiders"))
				this.hiderTeam = team;
			else if (team.getName().equals("Seekers"))
				this.seekerTeam = team;
			else {
				throw new IllegalArgumentException("Unknown team " + team.getName() + " in HNS map");
			}
		}

		this.allowedBlockCoords = BlockUtils.getAllBlocks(this.allowedBlocks, this);
		this.hideTimeTicks = this.gameMap.getHnsInfo().hideTime() * 20;
		this.isHidingTime = true;

		this.hiderKit = this.kits.get(KitHider.NAME.toLowerCase(Locale.ENGLISH));
		this.seekerKit = this.kits.get(KitRadarSeeker.NAME.toLowerCase(Locale.ENGLISH));

		KillStreakManager ksManager = this.getKillStreakManager();
		ksManager.disableKillStreak(KillStreakManager.KillStreakID.COMPASS);
		ksManager.disableKillStreak(KillStreakManager.KillStreakID.WOLVES);
		ksManager.disableKillStreak(KillStreakManager.KillStreakID.IRON_GOLEM);

		// Make some animals look around randomly so they are not all looking straight ahead,
		//  making Hiders stand out because they look slightly up/down
		for (LivingEntity living : this.gameWorld.getLivingEntities()) {
			if (this.allowedEntities.contains(living.getType())) {
				if (MathUtils.randomRange(0, 7) == 0) continue;

				new PacketFlyingPoint(
					living,
					MathUtils.randomRange(2d, 13d),  MathUtils.randomRange(3d, 10d),
					MathUtils.randomRange(0.02d, 0.5d), MathUtils.randomRange(0.85, 1d),
					MathUtils.randomRange(0.2, 1),
					Math.max(1, MathUtils.randomMax(5 * 20))
				).respawn();
			}
		}
	}

	public boolean isAllowedBlockType(Material mat) {
		return this.allowedBlocks.contains(mat);
	}

	public boolean isAllowedEntityType(EntityType type) {
		return type != EntityType.PLAYER && this.allowedEntities.contains(type);
	}

	public ArrayList<BlockCoords> getAllowedBlockCoords() {
		return this.allowedBlockCoords;
	}

	public Player getPresident() {
		return this.president;
	}

	@Override
	protected void registerKits() {
		super.registerKits();
	}

	@Override
	protected void applyKitFilters() {
		KitFilter.addGlobalRule(GLOBAL_RULE);

		KitFilter.addTeamRule("Hiders", HIDER_RULE);
		KitFilter.addTeamRule("Seekers", SEEKER_RULE);
	}

	@Override
	protected void removeKitFilters() {
		KitFilter.removeGlobalRule(GLOBAL_RULE.key());

		KitFilter.removeTeamRule("Hiders", HIDER_RULE.key());
		KitFilter.removeTeamRule("Seekers", SEEKER_RULE.key());
	}

	@Override
	public void prepTeamsDecided() {
		super.prepTeamsDecided();

		this.president = this.hiderTeam.getRandomPlayer();
		this.presidentSet = Collections.singleton(this.president);

		Component hiderKing = Component.text(" Hider King", this.hiderTeam.getRGBTextColor(), TextDecoration.BOLD);
		Component presIsPres = Component.textOfChildren(
			this.president.playerListName(),
			Component.text(" is ", NamedTextColor.GOLD),
			hiderKing
		);

		Component youArePres = Component.text("You are the", NamedTextColor.GOLD)
			.append(hiderKing);

		for (Player hider : this.hiderTeam.getPlayerMembers()) {
			PlayerInfo pinfo = Main.getPlayerInfo(hider);
			pinfo.kit = this.hiderKit;
			if (hider == this.president) {
				hider.sendMessage(youArePres);
				this.informOfTeam(hider, youArePres);
			}
			else {
				hider.sendMessage(presIsPres);
				this.informOfTeam(hider, presIsPres);
				pinfo.getMetadataViewer().updateBitfieldValue(this.president,
					MetaIndex.BASE_BITFIELD_IDX, MetaIndex.BASE_BITFIELD_GLOWING_IDX, true);
			}
		}

		for (Player seeker : this.seekerTeam.getPlayerMembers()) {
			Main.getPlayerInfo(seeker).kit = this.seekerKit;
			this.informOfTeam(seeker, Component.empty()); // hack
		}
	}

	@Override
	public void informOfTeam(Player p) {
		// Override to not use the default behaviour.
	}

	// for debug changing teams
	@Override
	public void onTeamSwitch(Player player, @Nullable TeamArenaTeam oldTeam, @Nullable TeamArenaTeam newTeam) {
		super.onTeamSwitch(player, oldTeam, newTeam);

		PlayerInfo pinfo = Main.getPlayerInfo(player);
		if (newTeam == this.hiderTeam) {
			pinfo.kit = this.hiderKit;
		}
		else if (newTeam == this.seekerTeam) {
			Kit kit = KitFilter.filterKit(this, newTeam, player, this.seekerKit);
			if (kit != null)
				pinfo.kit = kit;
		}

		if (this.gameState == GameState.LIVE && !isDead(player)) {
			this.giveKitAndGameItems(player, pinfo, true);
		}
	}

	@Override
	public void setViewingGlowingTeammates(PlayerInfo pinfo, boolean glow, boolean message) {
		this.setViewingGlowingTeammates(pinfo, glow, message, this.presidentSet);
	}

	@Override
	public void prepLive() {
		super.prepLive();
		assert CompileAsserts.OMIT || this.president != null;
	}

	@Override
	public void handleDeath(DamageEvent event) {
		super.handleDeath(event);

		if (event.getVictim() instanceof Player hider && this.hiderTeam.hasMember(hider)) {
			this.setToRespawn(hider, false);
			hider.sendActionBar(NO_RESPAWNING);
		}
	}

	@Override
	protected boolean spawnLockingAngel(Player p, @Nullable DamageEvent killer) {
		if (this.hiderTeam.getPlayerMembers().contains(p)) return false;
		return super.spawnLockingAngel(p, killer);
	}

	@Override
	public void liveTick() {
		assert CompileAsserts.OMIT || this.president != null;

		if (!CommandDebug.ignoreWinConditions && this.isDead(this.president)) {
			Bukkit.broadcast(PRESIDENT_DIED);
			this.winningTeam = seekerTeam;
			prepEnd();

			return;
		}

		final int currentTick = TeamArena.getGameTick();
		if (isHidingTime) {
			if (currentTick >= this.gameLiveTime + this.hideTimeTicks) {
				assert CompileAsserts.OMIT || currentTick == this.gameLiveTime + this.hideTimeTicks;

				this.isHidingTime = false;
				this.remainingSeekTime = DEFAULT_SEEK_TIME;

				this.seekerTeam.getPlayerMembers().forEach(player -> {
					player.getInventory().remove(kitMenuItem);
					player.teleport(this.seekerSpawnLoc);
				});

				Bukkit.broadcast(SEEKERS_RELEASED_CHAT);
				PlayerUtils.sendOptionalTitle(Component.empty(), SEEKERS_RELEASED_TITLE, 5, 40, 20);
				Bukkit.getOnlinePlayers().forEach(player -> player.playSound(player, Sound.ITEM_GOAT_HORN_SOUND_0, SoundCategory.PLAYERS, 1f, 2f));
			}
			else { // Countdown
				final int timeDiff = (this.gameLiveTime + this.hideTimeTicks) - currentTick;
				final int secs = timeDiff / 20;
				if (timeDiff % 20 == 0 &&
					(secs % 60 == 0 || secs % 30 == 0 || secs == 15 || secs <= 5)) {

					Bukkit.broadcast(
						SEEKERS_RELEASED_IN_CHAT.append(Component.text(secs + "s", NamedTextColor.GOLD, TextDecoration.BOLD))
					);
					Bukkit.getOnlinePlayers().forEach(player -> player.playSound(player, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, 1f));
				}
			}
		}
		else {
			assert CompileAsserts.OMIT || currentTick >= this.gameLiveTime + this.hideTimeTicks;

			if (--this.remainingSeekTime <= 0 && !CommandDebug.ignoreWinConditions) {
				assert CompileAsserts.OMIT || this.remainingSeekTime == 0;

				Bukkit.broadcast(HIDERS_WIN);
				this.winningTeam = hiderTeam;
				prepEnd();

				return;
			}
			else {
				final int secs = this.remainingSeekTime / 20;
				if (remainingSeekTime % 20 == 0 &&
					(secs % DEFAULT_SEEK_TIME == 0 || secs % (DEFAULT_SEEK_TIME / 2) == 0 ||
						secs % 60 == 0 || secs == 30 || secs == 15 || secs <= 5)) {

					Component timeLeft;
					float pitch;
					if (secs > 60) {
						int min = secs / 60;
						int secsOfTheMin = secs % 60;
						timeLeft = Component.text(min + "m " + secsOfTheMin + "s remaining", NamedTextColor.GOLD, TextDecoration.BOLD);
						pitch = 1f;
					}
					else {
						timeLeft = Component.text(secs + " seconds remaining", NamedTextColor.GOLD, TextDecoration.BOLD);
						pitch = 1.7f;
					}
					Bukkit.broadcast(timeLeft);
					Bukkit.getOnlinePlayers().forEach(player -> {
						PlayerUtils.sendOptionalTitle(Component.empty(), timeLeft, 15, 30, 30);
						player.playSound(player, Sound.BLOCK_NOTE_BLOCK_HAT, SoundCategory.PLAYERS, 1f, pitch);
					});
				}
			}
		}

		super.liveTick();
	}

	@Override
	public TeamArenaTeam addToLowestTeam(Player player, boolean add) {
		double hiderCount = (double) this.hiderTeam.getPlayerMembers().size() / HIDER_SEEKER_RATIO;
		double seekerCount = (double) this.seekerTeam.getPlayerMembers().size();

		TeamArenaTeam chosenTeam;
		if (hiderCount <= seekerCount)
			chosenTeam = this.hiderTeam;
		else
			chosenTeam = this.seekerTeam;

		if (add)
			chosenTeam.addMembers(player);

		return chosenTeam;
	}

	protected void loadConfig(TeamArenaMap map) {
		super.loadConfig(map);

		TeamArenaMap.HNSInfo hnsInfo = map.getHnsInfo();
		seekerSpawnLoc = hnsInfo.seekerSpawn().toLocation(this.gameWorld);

		this.allowedBlocks = new HashSet<>(hnsInfo.allowedBlocks());
		this.allowedEntities = new HashSet<>(hnsInfo.allowedEntities());
	}

	@Override
	public void updateSidebar(Player player, SidebarManager sidebar) {
		if (hiderTeam.hasMember(player)) {
			sidebar.addEntry(Component.text("Hider King", NamedTextColor.GOLD));
			sidebar.addEntry(president.playerListName());
		}
	}

	@Override
	protected Location getSpawnPoint(PlayerInfo pinfo) {
		if (pinfo.team == this.seekerTeam) {
			return this.seekerSpawnLoc;
		}
		return super.getSpawnPoint(pinfo);
	}

	@Override
	public void onDamage(DamageEvent event) {
		if (event.getFinalAttacker() instanceof Player p && this.seekerTeam.hasMember(p)) {
			if (this.isAllowedEntityType(event.getVictim().getType())) {
				event.setBroadcastDeathMessage(false);
			}
		}

		super.onDamage(event);
	}

	@Override
	public void prepDead() {
		super.prepDead();

		for (Player hider : this.hiderTeam.getPlayerMembers()) {
			Main.getPlayerInfo(hider).getMetadataViewer().removeBitfieldValue(this.president,
				MetaIndex.BASE_BITFIELD_IDX, MetaIndex.BASE_BITFIELD_GLOWING_IDX);
		}
	}

	// Can't break any blocks including foliage
	@Override
	protected boolean onBreakBlockSub(BlockBreakEvent event) {
		event.setCancelled(true);
		return true;
	}

	@Override
	public boolean canSelectKitNow(Player player) {
		return gameState.teamsChosen() || (gameState == GameState.LIVE && isHidingTime);
	}

	@Override
	public void selectKit(@NotNull Player player, @NotNull Kit kit) {
		if (canSelectKitNow(player)) {
			super.selectKit(player, kit);

			if (gameState == GameState.LIVE && isHidingTime && !isDead(player)) {
				// let players swap kits. very safe!
				PlayerInfo playerInfo = Main.getPlayerInfo(player);
				if (playerInfo.activeKit != null) {
					playerInfo.activeKit.removeKit(player);
				}
				playerInfo.kit = kit;
				this.giveKitAndGameItems(player, playerInfo, true);
			}
		}
	}

	@Override
	public void giveKitAndGameItems(Player player, PlayerInfo info, boolean clear) {
		super.giveKitAndGameItems(player, info, clear);

		if (this.seekerTeam.getPlayerMembers().contains(player) && this.gameState == GameState.LIVE && this.isHidingTime) {
			player.getInventory().addItem(kitMenuItem);
		}
	}

	@Override
	public boolean canSelectTeamNow() {
		return false;
	}

	@Override
	public boolean canTeamChatNow(Player player) {
		return true;
	}

	@Override
	public boolean isRespawningGame() {
		return true;
	}

	@Override
	protected boolean canJoinMidGame() {
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
	public String getDebugAntiStall() {
		return "hideandseek debugAntiStall xd";
	}

	@Override
	public void setDebugAntiStall(int antiStallCountdown) {}
}

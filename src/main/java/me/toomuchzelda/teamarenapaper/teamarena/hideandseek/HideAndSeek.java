package me.toomuchzelda.teamarenapaper.teamarena.hideandseek;

import me.toomuchzelda.teamarenapaper.CompileAsserts;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.*;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.gamescheduler.TeamArenaMap;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.filter.KitFilter;
import me.toomuchzelda.teamarenapaper.teamarena.kits.hideandseek.KitHider;
import me.toomuchzelda.teamarenapaper.teamarena.kits.hideandseek.KitSeeker;
import me.toomuchzelda.teamarenapaper.utils.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;

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
	private static final Component NO_RESPAWNING = Component.text("Hiders don't respawn", TextColors.ERROR_RED);

	private static final double HIDER_SEEKER_RATIO = 2d; // 2 Hiders for every seeker

	private TeamArenaTeam hiderTeam;
	private TeamArenaTeam seekerTeam;

	private Location seekerSpawnLoc;
	private final int hideTimeTicks;
	private Set<Material> allowedBlocks;
	private Set<EntityType> allowedEntities;

	private final ArrayList<BlockCoords> allowedBlockCoords;

	private boolean isHidingTime;

	private Player president;

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

		KitFilter.setAllowed(this, Set.of("hider", "seeker"));

		this.allowedBlockCoords = BlockUtils.getAllBlocks(this.allowedBlocks, this);
		this.hideTimeTicks = this.gameMap.getHnsInfo().hideTime() * 20;
		this.isHidingTime = true;
	}

	public boolean isAllowedBlockType(Material mat) {
		return this.allowedBlocks.contains(mat);
	}

	public boolean isAllowedEntityType(EntityType type) {
		return this.allowedEntities.contains(type);
	}

	public ArrayList<BlockCoords> getAllowedBlockCoords() {
		return this.allowedBlockCoords;
	}

	@Override
	public void prepTeamsDecided() {
		super.prepTeamsDecided();

		Kit hiderKit = this.kits.get(KitHider.NAME.toLowerCase(Locale.ENGLISH));
		Kit seekerKit = this.kits.get(KitSeeker.NAME.toLowerCase(Locale.ENGLISH));

		this.president = this.hiderTeam.getRandomPlayer();
		for (Player hider : this.hiderTeam.getPlayerMembers()) {
			Main.getPlayerInfo(hider).kit = hiderKit;
		}
		for (Player seeker : this.seekerTeam.getPlayerMembers()) {
			Main.getPlayerInfo(seeker).kit = seekerKit;
		}
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
	public void liveTick() {
		assert CompileAsserts.OMIT || this.president != null;

		if (this.isDead(this.president)) {
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

				this.seekerTeam.getPlayerMembers().forEach(player -> player.teleport(this.seekerSpawnLoc));

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
		}

		super.liveTick();
	}

	@Override
	public TeamArenaTeam addToLowestTeam(Player player, boolean add) {
		double hiderCount = (double) this.hiderTeam.getPlayerMembers().size();
		double seekerCount = ((double) this.seekerTeam.getPlayerMembers().size()) / HIDER_SEEKER_RATIO;

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

	// Can't break any blocks including foliage
	@Override
	protected boolean onBreakBlockSub(BlockBreakEvent event) {
		event.setCancelled(true);
		return true;
	}

	@Override
	public boolean canSelectKitNow() { // Kits forced onto players
		return false;
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
		return "";
	}

	@Override
	public void setDebugAntiStall(int antiStallCountdown) {}
}

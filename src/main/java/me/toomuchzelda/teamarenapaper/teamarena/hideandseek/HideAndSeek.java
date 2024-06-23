package me.toomuchzelda.teamarenapaper.teamarena.hideandseek;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.*;
import me.toomuchzelda.teamarenapaper.teamarena.gamescheduler.TeamArenaMap;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.filter.KitFilter;
import me.toomuchzelda.teamarenapaper.teamarena.kits.hideandseek.KitHider;
import me.toomuchzelda.teamarenapaper.teamarena.kits.hideandseek.KitSeeker;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class HideAndSeek extends TeamArena {

	private static final Component GAME_NAME = Component.text("Hide and Seek", GameType.HNS.shortName.color());
	private static final Component HOW_TO_PLAY = Component.text(
		"Hunters have to find and kill the Hiders, who look like blocks and animals before time is up",
		GameType.HNS.shortName.color()
	);

	private static final double HIDER_SEEKER_RATIO = 2d; // 2 Hiders for every seeker

	private TeamArenaTeam hiderTeam;
	private TeamArenaTeam seekerTeam;

	private Location seekerSpawnLoc;
	private Set<Material> allowedBlocks;
	private Set<EntityType> allowedEntities;

	public HideAndSeek(TeamArenaMap map) {
		super(map);

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
	}

	@Override
	public void prepTeamsDecided() {
		super.prepTeamsDecided();

		Kit hiderKit = this.kits.get(KitHider.NAME.toLowerCase(Locale.ENGLISH));
		Kit seekerKit = this.kits.get(KitSeeker.NAME.toLowerCase(Locale.ENGLISH));

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

		for (Player seeker : this.seekerTeam.getPlayerMembers()) {
			seeker.teleport(seekerSpawnLoc);
		}
	}

	@Override
	public TeamArenaTeam addToLowestTeam(Player player, boolean add) {
		double hiderCount = (double) this.hiderTeam.getPlayerMembers().size();
		double seekerCount = (double) this.seekerTeam.getPlayerMembers().size() / HIDER_SEEKER_RATIO;

		TeamArenaTeam chosenTeam;
		if (hiderCount <= seekerCount)
			chosenTeam = hiderTeam;
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

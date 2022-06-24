package me.toomuchzelda.teamarenapaper.teamarena.searchanddestroy;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.GameState;
import me.toomuchzelda.teamarenapaper.teamarena.SidebarManager;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.capturetheflag.Flag;
import me.toomuchzelda.teamarenapaper.utils.BlockUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;

public class SearchAndDestroy extends TeamArena
{
	//record it here from the map config but won't use it for anything
	protected boolean randomBases = false;
	//initialised in parseConfig
	protected Map<TeamArenaTeam, List<Bomb>> teamBombs;

	public SearchAndDestroy() {
		super();

		for(List<Bomb> bombsList : teamBombs.values()) {
			for(Bomb bomb : bombsList) {
				bomb.init();
			}
		}
	}

	public void tick() {
		super.tick();
	}

	@Override
	public void parseConfig(Map<String, Object> map) {
		super.parseConfig(map);

		Map<String, Object> customFlags = (Map<String, Object>) map.get("Custom");

		Main.logger().info("Custom Info: ");
		Main.logger().info(customFlags.toString());

		for (Map.Entry<String, Object> entry : customFlags.entrySet()) {
			this.teamBombs = new HashMap<>(customFlags.size());
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
					Bomb bomb = new Bomb(team, BlockUtils.parseCoordsToVec(bombCoords, 0, 0, 0).toBlockVector().toLocation(this.gameWorld));
					bombs.add(bomb);
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

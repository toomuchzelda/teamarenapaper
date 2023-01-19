package me.toomuchzelda.teamarenapaper.teamarena.gamescheduler;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.GameType;
import me.toomuchzelda.teamarenapaper.utils.BlockUtils;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 * @author toomuchzelda
 *
 * Class that represents a Team Arena map.
 *
 * Contains:
 * - the path to the world folder
 * - the name of the Map (not the world's directory)
 * - all the map's configurations
 */
public class TeamArenaMap
{
	public record KothHill(String name, Vector minCorner, Vector maxCorner, int time) {}
	public record KOTHInfo(boolean randomOrder, List<KothHill> hills) {}

	public record CTFInfo(int capsToWin, Map<String, Vector> teamFlags) {}

	public record SNDInfo(boolean randomBases, Map<String, List<BlockVector>> teamBombs) {}

	private final String name;
	private final String authors;
	private final String description;

	private final boolean doDaylightCycle;
	private final boolean doWeatherCycle;

	private final Vector minBorderCorner;
	private final Vector maxBorderCorner;
	//if there is a floor/ceiling border or not.
	private final boolean noVerticalBorder;

	private final Map<String, Vector[]> teamSpawns;

	private final List<GameType> gameTypes;
	private final KOTHInfo kothInfo;
	private final CTFInfo ctfInfo;
	private final SNDInfo sndInfo;

	private final File file;
	//info about the map that gets sent to players when they join / map loads
	private Component infoComponent;

	private final boolean inRotation;

	public String toString() {
		return "name: " + name
				+ ", authors: " + authors
				+ ", description: " + description
				+ ", doDaylightCycle: " + doDaylightCycle
				+ ", doWeatherCycle: " + doWeatherCycle
				+ ", inRotation: " + inRotation
				+ ", minBorderCorner: " + minBorderCorner.toString()
				+ ", maxBorderCorner: " + maxBorderCorner.toString()
				+ ", noVerticalBorder: " + noVerticalBorder
				+ ", teamSpawns keys: " + teamSpawns.keySet().toString()
				+ ", kothInfo: " + (kothInfo != null ? kothInfo.toString() : "null")
				+ ", ctfInfo: " + (ctfInfo != null ? ctfInfo.toString() : "null")
				+ ", sndInfo: " + (sndInfo != null ? sndInfo.toString() : "null");
	}

	TeamArenaMap(File worldFolder) throws IOException {
		this.file = worldFolder;
		this.gameTypes = new ArrayList<>(GameType.values().length);
		//parse the Main config (MainConfig.yml)
		File mainFile = new File(worldFolder, "MainConfig.yml");
		Yaml yaml = new Yaml();
		try (FileInputStream mainInput = new FileInputStream(mainFile)) {
			Map<String, Object> mainMap = yaml.load(mainInput);

			this.name = (String) mainMap.get("Name");
			this.authors = (String) mainMap.get("Author");
			this.description = (String) mainMap.get("Description");

			boolean doDayCycle;
			try {
				doDayCycle = (boolean) mainMap.get("DoDaylightCycle");
			}
			//the element doesn't exist, or spelled incorrectly and recognized by snakeyaml as a String instead of a boolean
			catch(NullPointerException | ClassCastException e) {
				doDayCycle = false;
			}
			this.doDaylightCycle = doDayCycle;

			boolean doWeatherCycle;
			try {
				doWeatherCycle = (boolean) mainMap.get("DoWeatherCycle");
			}
			catch(NullPointerException | ClassCastException e) {
				doWeatherCycle = false;
			}
			this.doWeatherCycle = doWeatherCycle;

			boolean inRotation;
			try {
				inRotation = (boolean) mainMap.get("InRotation");
			}
			catch(NullPointerException | ClassCastException e) {
				inRotation = true;
			}
			this.inRotation = inRotation;

			//Map border
			// Only supports rectangular prism borders as of now
			List<String> borders = (List<String>) mainMap.get("Border");
			Vector vec1 = BlockUtils.parseCoordsToVec(borders.get(0), 0, 0, 0);
			Vector vec2 = BlockUtils.parseCoordsToVec(borders.get(1), 0, 0, 0);
			this.minBorderCorner = Vector.getMinimum(vec1, vec2);
			this.maxBorderCorner = Vector.getMaximum(vec1, vec2).add(new Vector(1, 1, 1));

			//if the Y values are the same consider there no verticle borders
			this.noVerticalBorder = vec1.getY() == vec2.getY();

			//load team spawns
			Map<String, Map<String, List<String>>> teamsMap =
					(Map<String, Map<String, List<String>>>) mainMap.get("Teams");

			int numOfTeams = teamsMap.size();
			this.teamSpawns = new HashMap<>(numOfTeams);
			for (Map.Entry<String, Map<String, List<String>>> entry : teamsMap.entrySet()) {
				String teamName = entry.getKey();

				Map<String, List<String>> spawnsYaml = entry.getValue();

				List<String> spawnsList = spawnsYaml.get("Spawns");
				Vector[] vecArray = new Vector[spawnsList.size()];

				int index = 0;
				for (String loc : spawnsList) {
					Vector coords = BlockUtils.parseCoordsToVec(loc, 0.5, 0, 0.5);

					vecArray[index] = coords;
					index++;
				}
				this.teamSpawns.put(teamName, vecArray);
			}
		}

		//parse KOTH config if present
		File kothFile = new File(worldFolder, "KOTHConfig.yml");
		KOTHInfo kothInfo = null;
		if(kothFile.exists() && kothFile.isFile()) {
			try (FileInputStream kothInput = new FileInputStream(kothFile)) {
				Map<String, Object> kothMap = yaml.load(kothInput);

				boolean randomHillOrder;
				try {
					randomHillOrder = (boolean) kothMap.get("RandomHillOrder");
				}
				catch(NullPointerException | ClassCastException e) {
					Main.logger().warning("Invalid RandomHillOrder! Must be true/false. Defaulting to false. In file " +
							kothFile.getName());
					randomHillOrder = false;
				}

				Map<String, List<String>> hillsMap = (Map<String, List<String>>) kothMap.get("Hills");
				Iterator<Map.Entry<String, List<String>>> hillsIter = hillsMap.entrySet().iterator();

				KothHill[] hills = new KothHill[hillsMap.size()];
				int index = 0;
				while(hillsIter.hasNext()) {
					Map.Entry<String, List<String>> entry = hillsIter.next();

					String name = entry.getKey();
					String coordOne = entry.getValue().get(0);
					String coordTwo = entry.getValue().get(1);
					String timeString = entry.getValue().get(2);

					int time = Integer.parseInt(timeString.split(",")[1]);

					Vector one = BlockUtils.parseCoordsToVec(coordOne, 0, 0, 0);
					Vector two = BlockUtils.parseCoordsToVec(coordTwo, 0, 0, 0);

					Vector minCorner = Vector.getMinimum(one, two);
					Vector maxCorner = Vector.getMaximum(one, two);

					hills[index++] = new KothHill(name, minCorner, maxCorner, time);
				}

				kothInfo = new KOTHInfo(randomHillOrder, List.of(hills));
			}
			//just run with koth not available
			catch (Exception e) {
				Main.logger().warning("Error when parsing Koth config for " + worldFolder.getName());
				kothInfo = null;
			}
		}
		this.kothInfo = kothInfo;
		if(kothInfo != null)
			this.gameTypes.add(GameType.KOTH);

		//parse CTF config if present
		File ctfFile = new File(worldFolder, "CTFConfig.yml");
		CTFInfo ctfInfo = null;
		if(ctfFile.exists() && ctfFile.isFile()) {
			try (FileInputStream ctfInput = new FileInputStream(ctfFile)) {
				Map<String, Object> ctfMap = yaml.load(ctfInput);

				int capsToWin = 3;
				Map<String, Vector> teamFlags = new HashMap<>();
				for (Map.Entry<String, Object> entry : ctfMap.entrySet()) {
					if (entry.getKey().equalsIgnoreCase("CapsToWin")) {
						try {
							capsToWin = (Integer) entry.getValue();
						} catch (NullPointerException | ClassCastException e) {
							Main.logger().warning("Invalid CapsToWin! Must be an integer number (no decimals!). Defaulting to 3");
						}
					} else if(this.teamSpawns.containsKey(entry.getKey())) {
						//if is a team that was added in MainConfig.yml
						Vector teamsFlagLoc = BlockUtils.parseCoordsToVec((String) entry.getValue(), 0.5, -0.4, 0.5);
						teamFlags.put(entry.getKey(), teamsFlagLoc);
					}
					else {
						Main.logger().warning("Unknown entry in CTF config for " + worldFolder.getName() + ": " + entry.getKey());
					}
				}
				ctfInfo = new CTFInfo(capsToWin, teamFlags);
			}
			catch (Exception e) {
				Main.logger().warning("Error when parsing CTF config for " + worldFolder.getName());
			}
		}
		this.ctfInfo = ctfInfo;
		if(ctfInfo != null)
			this.gameTypes.add(GameType.CTF);

		//parse SND config if present
		File sndFile = new File(worldFolder, "SNDConfig.yml");
		SNDInfo sndInfo = null;
		if(sndFile.exists() && sndFile.isFile()) {
			try (FileInputStream sndInput = new FileInputStream(sndFile)) {
				Map<String, Object> sndMap = yaml.load(sndInput);

				Map<String, List<BlockVector>> teamBombs = new HashMap<>(sndMap.size());
				boolean randomBases = false;

				for (Map.Entry<String, Object> entry : sndMap.entrySet()) {
					if (entry.getKey().equalsIgnoreCase("Random Base")) {
						try {
							randomBases = (boolean) entry.getValue();
						} catch (NullPointerException | ClassCastException e) {
							Main.logger().warning("Invalid random base value in SND config for " + worldFolder.getName());
						}
					}
					else if (this.teamSpawns.containsKey(entry.getKey())) {
						List<String> configBombs = (List<String>) entry.getValue();
						List<BlockVector> bombs = new ArrayList<>(configBombs.size());
						for(String bombCoords : configBombs) {
							BlockVector blockVector = BlockUtils.parseCoordsToVec(bombCoords, 0, 0, 0).toBlockVector();
							bombs.add(blockVector);
						}
						teamBombs.put(entry.getKey(), bombs);
					}
					else {
						Main.logger().warning("Unknown entry " + entry.getKey() + " in SND config for " + worldFolder.getName());
					}
				}
				sndInfo = new SNDInfo(randomBases, teamBombs);
			}
			catch (Exception e) {
				Main.logger().warning("Error when parsing SND config for " + worldFolder.getName());
			}
		}
		this.sndInfo = sndInfo;
		if(sndInfo != null)
			this.gameTypes.add(GameType.SND);
	}

	public Component getMapInfoComponent() {
		if(infoComponent == null) {
			infoComponent = Component.text()
					.append(Component.text("Map Name: " , NamedTextColor.GOLD), Component.text(name, NamedTextColor.YELLOW), Component.newline(),
							Component.text("Author(s): ", NamedTextColor.GOLD), Component.text(authors, NamedTextColor.YELLOW), Component.newline(),
							Component.text("Description: ", NamedTextColor.GOLD), Component.text(description, NamedTextColor.YELLOW))
					.build();
		}

		return this.infoComponent;
	}

	public String getName() {
		return name;
	}

	public String getAuthors() {
		return authors;
	}

	public String getDescription() {
		return description;
	}

	public boolean isDoDaylightCycle() {
		return doDaylightCycle;
	}

	public boolean isDoWeatherCycle() {
		return doWeatherCycle;
	}

	public boolean isInRotation() {
		return this.inRotation;
	}

	public Vector getMinBorderCorner() {
		return minBorderCorner;
	}

	public Vector getMaxBorderCorner() {
		return maxBorderCorner;
	}

	public boolean hasVerticalBorder() {
		return !noVerticalBorder;
	}

	public Map<String, Vector[]> getTeamSpawns() {
		return teamSpawns;
	}

	public boolean hasGameType(GameType type) {
		return this.gameTypes.contains(type);
	}

	public GameType getRandomGameType() {
		return this.gameTypes.get(MathUtils.random.nextInt(gameTypes.size()));
	}

	public List<GameType> getGameTypes() {
		return this.gameTypes;
	}

	public KOTHInfo getKothInfo() {
		return this.kothInfo;
	}

	public CTFInfo getCtfInfo() {
		return this.ctfInfo;
	}

	public SNDInfo getSndInfo() {
		return this.sndInfo;
	}

	public File getFile() {
		return this.file;
	}
}

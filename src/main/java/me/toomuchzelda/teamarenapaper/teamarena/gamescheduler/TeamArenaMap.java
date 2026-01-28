package me.toomuchzelda.teamarenapaper.teamarena.gamescheduler;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.GameType;
import me.toomuchzelda.teamarenapaper.teamarena.digandbuild.StatusOreType;
import me.toomuchzelda.teamarenapaper.utils.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentBuilder;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.Nullable;
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
	public record TeamInfo(String name, String simpleName, Color colour1, Color colour2, NamedTextColor glowColour,
						   DyeColor dyeColour, Material icon, Vector[] spawns) {}

	public record KothHill(String name, Vector minCorner, Vector maxCorner, int time) {}
	public record KOTHInfo(boolean randomOrder, List<KothHill> hills) {}

	public record CTFInfo(int capsToWin, Map<String, Vector> teamFlags) {}

	public record SNDInfo(boolean randomBases, Map<String, List<BlockVector>> teamBombs) {}

	/** Info per team for dnb */
	public record DNBTeamInfo(BlockCoords oreCoords, double protectionRadius, @Nullable BlockCoords teamChest) {}
	public record DNBStatusOreInfo(Material oreType, Material itemType, int required, List<Vector> hologramLocs,
								   List<BlockCoords> coords) {}
	public record DNBInfo(Vector middle, Material oreType, List<Material> tools, List<Material> blocks,
						  List<IntBoundingBox> noBuildZones, Map<StatusOreType, DNBStatusOreInfo> statusOres,
						  Map<String, DNBTeamInfo> teams) {}

	public record HNSInfo(List<Material> allowedBlocks, List<EntityType> allowedEntities, Vector seekerSpawn,
						  int hideTime) {}

	private final String name;
	private final String authors;
	private final String description;
	private final String url; // for example linking author's website

	private final boolean doDaylightCycle;
	private final boolean doWeatherCycle;
	private final boolean tntPrimable; // For maps downloaded from miner movies replays.

	private final Vector minBorderCorner;
	private final Vector maxBorderCorner;
	//if there is a floor/ceiling border or not.
	private final boolean noVerticalBorder;

	private final boolean inRotation;
	private final int minPlayers;
	private final int maxPlayers;

	private final Map<String, TeamInfo> teamSpawns;

	private final List<GameType> gameTypes;
	private final KOTHInfo kothInfo;
	private final CTFInfo ctfInfo;
	private final SNDInfo sndInfo;
	private final DNBInfo dnbInfo;
	private final HNSInfo hnsInfo;

	private final File file;
	//info about the map that gets sent to players when they join / map loads
	private Component infoComponent;

	public String toString() {
		return "name: " + name
				+ ", authors: " + authors
				+ ", description: " + description
				+ ", url: " + url
				+ ", doDaylightCycle: " + doDaylightCycle
				+ ", doWeatherCycle: " + doWeatherCycle
				+ ", tntPrimable: " + tntPrimable
				+ ", inRotation: " + inRotation
				+ ", minPlayers: " + minPlayers
				+ ", maxPlayers: " + maxPlayers
				+ ", minBorderCorner: " + minBorderCorner.toString()
				+ ", maxBorderCorner: " + maxBorderCorner.toString()
				+ ", noVerticalBorder: " + noVerticalBorder
				+ ", teamSpawns keys: " + teamSpawns.keySet().toString()
				+ ", kothInfo: " + (kothInfo != null ? kothInfo.toString() : "null")
				+ ", ctfInfo: " + (ctfInfo != null ? ctfInfo.toString() : "null")
				+ ", sndInfo: " + (sndInfo != null ? sndInfo.toString() : "null")
				+ ", dnbInfo: " + (dnbInfo != null ? dnbInfo.toString() : "null")
				+ ", hnsInfo: " + (hnsInfo != null ? hnsInfo.toString() : "null");
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

			String url;
			try {
				url = (String) mainMap.get("URL");
			}
			catch (NullPointerException | ClassCastException e) {
				url = null;
			}
			this.url = url;

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

			boolean tntPrimable;
			try {
				tntPrimable = (boolean) mainMap.get("TntPrimable");
			}
			catch (NullPointerException | ClassCastException e) {
				tntPrimable = true;
			}
			this.tntPrimable = tntPrimable;

			boolean inRotation;
			try {
				inRotation = (boolean) mainMap.get("InRotation");
			}
			catch(NullPointerException | ClassCastException e) {
				inRotation = true;
			}
			this.inRotation = inRotation;

			int minPlayers;
			try {
				minPlayers = (int) mainMap.get("MinPlayers");
			}
			catch (NullPointerException e) {
				minPlayers = 0;
			}
			catch (ClassCastException e) {
				Main.logger().warning("Bad MinPlayers for " + this.name);
				minPlayers = 0;
			}
			this.minPlayers = minPlayers;

			int maxPlayers;
			try {
				maxPlayers = (int) mainMap.get("MaxPlayers");
			}
			catch (NullPointerException e) {
				maxPlayers = Integer.MAX_VALUE;
			}
			catch (ClassCastException e) {
				Main.logger().warning("Bad MaxPlayers for " + this.name);
				maxPlayers = Integer.MAX_VALUE;
			}
			this.maxPlayers = maxPlayers;

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
			Map<String, Map<String, Object>> teamsMap =
					(Map<String, Map<String, Object>>) mainMap.get("Teams");

			this.teamSpawns = HashMap.newHashMap(teamsMap.size());
			for (Map.Entry<String, Map<String, Object>> entry : teamsMap.entrySet()) {
				final String teamName = entry.getKey();

				try {
					Map<String, Object> spawnsYaml = entry.getValue();

					List<String> spawnsList = (List<String>) spawnsYaml.get("Spawns");
					Vector[] vecArray = new Vector[spawnsList.size()];

					int index = 0;
					for (String loc : spawnsList) {
						Vector coords = BlockUtils.parseCoordsToVec(loc, 0.5, 0, 0.5);

						vecArray[index] = coords;
						index++;
					}

					if (!teamName.startsWith("Custom")) {
						// Remaining fields provided by LegacyTeams
						final TeamInfo basicTeam = new TeamInfo(teamName, null, null, null, null, null, null, vecArray);
						if (this.teamSpawns.put(teamName, basicTeam) != null) {
							throw new IllegalArgumentException("Duplicate team " + teamName + " defined.");
						}
					} else { // a custom team
						// Name
						// SimpleName
						// Colour1
						// Colour2 (optional)
						// ColourPattern (optional) (add later)
						// - Gradient if unspecified and Colour2 specified
						// GlowColour (NMS team colour)
						// DyeColour
						// Icon
						final String name = (String) spawnsYaml.get("Name");
						final String simpleName = spawnsYaml.containsKey("SimpleName") ?
							(String) spawnsYaml.get("SimpleName") : name;

						final Color colour1 = TextUtils.readConfigColour((String) spawnsYaml.get("Colour1"));
						final Color colour2;
						if (spawnsYaml.containsKey("Colour2"))
							colour2 = TextUtils.readConfigColour((String) spawnsYaml.get("Colour2"));
						else
							colour2 = null;

						final NamedTextColor glowColour;
						if (spawnsYaml.containsKey("GlowColour"))
							glowColour = NamedTextColor.NAMES.value(((String) spawnsYaml.get("GlowColour")).toLowerCase(Locale.ENGLISH));
						else
							throw new IllegalArgumentException("No GlowColour specified. ex. blue, yellow, light_purple");

						final DyeColor dyeColour;
						if (spawnsYaml.containsKey("DyeColour"))
							dyeColour = DyeColor.valueOf(((String) spawnsYaml.get("DyeColour")).toUpperCase(Locale.ENGLISH));
						else
							throw new IllegalArgumentException("No DyeColour specified. ex. blue, red, etc.");

						final Material icon = Material.valueOf((String) spawnsYaml.get("Icon"));

						final TeamInfo tinfo = new TeamInfo(
							name, simpleName, colour1, colour2, glowColour, dyeColour, icon, vecArray
						);

						if (this.teamSpawns.put(name, tinfo) != null) {
							throw new IllegalArgumentException("Duplicate team " + teamName + " defined.");
						}
					}
				}
				catch (Exception e) {
					throw new RuntimeException("Error processing team " + teamName, e);
				}
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
				Main.logger().warning("Error when parsing Koth config for " + worldFolder.getName() + ": " + e);
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
				Main.logger().warning("Error when parsing CTF config for " + worldFolder.getName() + ": " + e);
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
				Main.logger().warning("Error when parsing SND config for " + worldFolder.getName() + ": " + e);
			}
		}
		this.sndInfo = sndInfo;
		if(sndInfo != null)
			this.gameTypes.add(GameType.SND);

		File dnbFile = new File(worldFolder, "DNBConfig.yml");
		DNBInfo dnbInfo = null;
		if (dnbFile.exists() && dnbFile.isFile()) {
			try (FileInputStream dnbInput = new FileInputStream(dnbFile)) {
				Map<String, Object> dnbMap = yaml.load(dnbInput);

				Vector middle;
				try {
					middle = BlockUtils.parseCoordsToVec((String) dnbMap.get("Middle"), 0.5, 0.5, 0.5);
				}
				catch (NullPointerException | ClassCastException e) {
					Main.logger().warning("Invalid Middle value in DNB config for " + worldFolder.getName() + ". " + e.getMessage());
					throw e;
				}

				Material oreType;
				try {
					String oreTypeStr = (String) dnbMap.get("OreType");
					oreType = Material.valueOf(oreTypeStr);
				}
				catch (ClassCastException | NullPointerException e) {
					Main.logger().warning("Invalid OreType value in DNB config for " + worldFolder.getName() + ". " + e.getMessage());
					throw e;
				}

				List<Material> tools;
				try {
					List<String> toolsStrList = (List<String>) dnbMap.get("Tools");
					tools = new ArrayList<>(toolsStrList.size());
					for (String s : toolsStrList) {
						tools.add(Material.valueOf(s));
					}
				}
				catch (ClassCastException | NullPointerException e) {
					Main.logger().warning("Invalid Tools value in DNB config for " + worldFolder.getName() + ". " + e.getMessage());
					tools = new ArrayList<>(0); // Default to no tools
				}

				List<Material> blocks;
				try {
					List<String> blocksStrList = (List<String>) dnbMap.get("Blocks");
					blocks = new ArrayList<>(blocksStrList.size());
					for (String s : blocksStrList) {
						blocks.add(Material.valueOf(s));
					}
				}
				catch (ClassCastException | NullPointerException e) {
					Main.logger().warning("Invalid Blocks value in DNB config for " + worldFolder.getName() + ". " + e.getMessage());
					blocks = new ArrayList<>(0); // Default to no tools
				}

				List<IntBoundingBox> noBuildZones;
				try {
					List<List<String>> cornersList = (List<List<String>>) dnbMap.get("NoBuildZones");
					noBuildZones = new ArrayList<>(cornersList.size());
					for (List<String> corners : cornersList) {
						if (corners.size() != 2) {
							Main.logger().warning("Bad NoBuildZone entry on DNBConfig of " + worldFolder.getName()
								+ ". Found not 2 corners in an entry of the NoBuildZones");
							continue;
						}

						BlockCoords cornerOne = BlockUtils.parseCoordsToBlockCoords(corners.get(0));
						BlockCoords cornerTwo = BlockUtils.parseCoordsToBlockCoords(corners.get(1));

						noBuildZones.add(new IntBoundingBox(cornerOne, cornerTwo));
					}
				}
				catch (ClassCastException | NullPointerException e) {
					Main.logger().warning("Invalid value for NoBuildZones in DNBConfig of " + worldFolder.getName() + ". Defaulting to no no-build-zones."
						+ e.getMessage());
					noBuildZones = new ArrayList<>();
				}

				Map<StatusOreType, DNBStatusOreInfo> statusOres;
				try {
					Map<String, Map<String, Object>> oresConfig = (Map<String, Map<String, Object>>) dnbMap.get("StatusOres");
					statusOres = new EnumMap<>(StatusOreType.class);
					for (var entry : oresConfig.entrySet()) {

						StatusOreType type = StatusOreType.valueOf(entry.getKey().toUpperCase(Locale.ENGLISH));
						Material statusOreType = Material.valueOf(((String) entry.getValue().get("OreType")).toUpperCase(Locale.ENGLISH));
						Material itemType = Material.valueOf(((String) entry.getValue().get("Item")).toUpperCase(Locale.ENGLISH));
						int required = (Integer) entry.getValue().get("Required");

						List<String> holograms = (List<String>) entry.getValue().get("Holograms");
						List<Vector> hologramCoords = new ArrayList<>(holograms.size());
						for (String hologramCoordStr : holograms) {
							hologramCoords.add(BlockUtils.parseCoordsToVec(hologramCoordStr, 0.5d, 0.5d, 0.5d));
						}

						List<String> coords = (List<String>) entry.getValue().get("Locations");
						List<BlockCoords> oreStatusBlockCoordsList = new ArrayList<>(coords.size());
						for (String oreStatusCoordStr : coords) {
							BlockCoords oreStatusBlockCoords = BlockUtils.parseCoordsToBlockCoords(oreStatusCoordStr);
							oreStatusBlockCoordsList.add(oreStatusBlockCoords);
						}

						DNBStatusOreInfo statusOreInfo = new DNBStatusOreInfo(statusOreType, itemType, required, hologramCoords,
							oreStatusBlockCoordsList);
						statusOres.put(type, statusOreInfo);
					}
				}
				catch (ClassCastException | NullPointerException | IllegalArgumentException e) {
					Main.logger().warning("Bad value in DNB status ore config of " + worldFolder.getName() + ". " + e.getMessage());
					statusOres = Collections.emptyMap();
				}

				Map<String, DNBTeamInfo> teamInfo;
				try {
					Map<String, Map<String, Object>> teamConfigs = (Map<String, Map<String, Object>>) dnbMap.get("Teams");
					teamInfo = new HashMap<>();
					for (var entry : teamConfigs.entrySet()) {
						if (this.teamSpawns.containsKey(entry.getKey())) {
							BlockCoords oreCoords = BlockUtils.parseCoordsToBlockCoords((String) entry.getValue().get("Ore"));
							double radius = (double) entry.getValue().get("Radius");
							String chestCoordStr = (String) entry.getValue().get("Chest");
							BlockCoords chestCoords;
							if (chestCoordStr != null)
								 chestCoords = BlockUtils.parseCoordsToBlockCoords(chestCoordStr);
							else
								chestCoords = null;

							DNBTeamInfo tinfo = new DNBTeamInfo(oreCoords, radius, chestCoords);
							teamInfo.put(entry.getKey(), tinfo);
						}
						else {
							Main.logger().warning("Unknown team " + entry.getKey() + " in DNBConfig of " + worldFolder.getName() + ". Team is not declared in MainConfig");
						}
					}
				}
				catch (NullPointerException | ClassCastException e) {
					Main.logger().warning("Invalid Teams value in DNB config of " + worldFolder.getName() + ". " + e.getMessage());
					throw e;
				}

				dnbInfo = new DNBInfo(middle, oreType, tools, blocks, noBuildZones, statusOres, teamInfo);
			}
			catch (Exception e) {
				Main.logger().warning("Error when parsing DNB config for " + worldFolder.getName() + ": " + e);
			}
		}
		this.dnbInfo = dnbInfo;
		if(dnbInfo != null)
			this.gameTypes.add(GameType.DNB);

		File hnsFile = new File(worldFolder, "HNSConfig.yml");
		HNSInfo hnsInfo = null;
		if (hnsFile.exists() && hnsFile.isFile()) {
			try (FileInputStream hnsInput = new FileInputStream(hnsFile)) {
				Map<String, Object> lists = yaml.load(hnsInput);

				List<Material> blocks;
				try {
					List<String> allowedBlocks = (List<String>) lists.get("AllowedBlocks");
					if (allowedBlocks != null) {
						blocks = new ArrayList<>(allowedBlocks.size());
						for (String s : allowedBlocks) blocks.add(Material.valueOf(s));
					} else {
						blocks = Collections.emptyList();
					}
				}
				catch (NullPointerException | ClassCastException e) {
					Main.logger().warning("Error in AllowedBlocks of HNS config of " + worldFolder.getName() + ". " + e);
					blocks = Collections.emptyList();
				}

				List<EntityType> entities;
				try {
					List<String> allowedEntities = (List<String>) lists.get("AllowedEntities");
					if (allowedEntities != null) {
						entities = new ArrayList<>(allowedEntities.size());
						for (String s : allowedEntities) {
							EntityType type = EntityType.valueOf(s);
							if (LivingEntity.class.isAssignableFrom(type.getEntityClass()))
								entities.add(type);
							else {
								Main.logger().severe(s + " is not a valid EntityType. In HNS config of " + worldFolder.getName());
							}
						}
					}
					else {
						entities = Collections.emptyList();
					}
				}
				catch (NullPointerException| ClassCastException e) {
					Main.logger().warning("Error in AllowedEntities of HNS config of " + worldFolder.getName() + ". " + e);
					entities = Collections.emptyList();
				}

				Vector spawn;
				try {
					String vec = (String) lists.get("SeekerSpawn");
					spawn = BlockUtils.parseCoordsToVec(vec, 0.5, 0, 0.5);
				}
				catch (NullPointerException | ClassCastException e) {
					Main.logger().severe("Error in HunterSpawn of HNS config of " + worldFolder.getName() + ". " + e);
					throw e;
				}

				int hideTime;
				try {
					hideTime = (int) (Integer) lists.get("HideTime");
				}
				catch (NullPointerException | ClassCastException e) {
					Main.logger().severe("Error in HideTime of HNS config of " + worldFolder.getName() + e);
					throw e;
				}

				hnsInfo = new HNSInfo(blocks, entities, spawn, hideTime);
			}
		}
		this.hnsInfo = hnsInfo;
		if (hnsInfo != null)
			this.gameTypes.add(GameType.HNS);
	}

	public Component getMapInfoComponent() {
		if(infoComponent == null) {
			ComponentBuilder<TextComponent, TextComponent.Builder> builder = Component.text().append(
					Component.text("Map Name: " , NamedTextColor.GOLD), Component.text(name, NamedTextColor.YELLOW), Component.newline(),
					Component.text("Author(s): ", NamedTextColor.GOLD), Component.text(authors, NamedTextColor.YELLOW), Component.newline(),
					Component.text("Description: ", NamedTextColor.GOLD), Component.text(description, NamedTextColor.YELLOW));

			if (this.url != null) {
				builder.append(
					Component.newline(),
					Component.text("URL: ", NamedTextColor.GOLD),
					Component.text(this.url, Style.style()
						.color(NamedTextColor.YELLOW).decorate(TextDecoration.UNDERLINED)
						.clickEvent(ClickEvent.openUrl(this.url))
						.build())
				);
			}

			this.infoComponent = builder.build();
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

	public boolean isTntPrimable() {
		return this.tntPrimable;
	}

	public boolean isInRotation() {
		return this.inRotation;
	}

	public int getMinPlayers() {
		return this.minPlayers;
	}

	public int getMaxPlayers() {
		return this.maxPlayers;
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

	public Map<String, TeamInfo> getTeamSpawns() {
		return teamSpawns;
	}

	public boolean hasGameType(GameType type) {
		return this.gameTypes.contains(type);
	}

	public GameType getRandomGameType() {
		if (this.gameTypes.isEmpty())
			return null;
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

	public DNBInfo getDnbInfo() {
		return this.dnbInfo;
	}

	public HNSInfo getHnsInfo() {
		return this.hnsInfo;
	}

	public File getFile() {
		return this.file;
	}
}

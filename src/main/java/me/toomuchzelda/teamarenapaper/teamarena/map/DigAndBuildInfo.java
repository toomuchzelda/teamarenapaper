package me.toomuchzelda.teamarenapaper.teamarena.map;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.digandbuild.StatusOreType;
import me.toomuchzelda.teamarenapaper.utils.BlockCoords;
import me.toomuchzelda.teamarenapaper.utils.BlockUtils;
import me.toomuchzelda.teamarenapaper.utils.IntBoundingBox;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;

public record DigAndBuildInfo(Vector middle, Material oreType, List<Material> tools, List<Material> blocks,
							  List<IntBoundingBox> noBuildZones, Map<StatusOreType, DNBStatusOreInfo> statusOres,
							  Map<String, DNBTeamInfo> teams) {
	/** Info per team for dnb */
	public record DNBTeamInfo(BlockCoords oreCoords, double protectionRadius, @Nullable BlockCoords teamChest) {}

	public record DNBStatusOreInfo(Material oreType, Material itemType, int required, List<Vector> hologramLocs,
								   List<BlockCoords> coords) {}

	@Nullable
	public static DigAndBuildInfo parse(TeamArenaMap teamArenaMap, Path worldFolder) {
		Path config = worldFolder.resolve("map-config-dnb.yml");
		if (Files.isRegularFile(config)) {
			try (var br = Files.newBufferedReader(config)) {
				YamlConfiguration yaml = YamlConfiguration.loadConfiguration(br);
				int configVersion = yaml.getInt("config-version");
				return switch (configVersion) {
					case 1 -> parseV1(teamArenaMap, yaml);
					default -> throw new IllegalStateException("Invalid config version " + configVersion);
				};
			} catch (Exception ex) {
				Main.logger().log(Level.WARNING, "Failed to load map-config-dnb.yml for " + teamArenaMap.getName(), ex);
			}
		}
		// legacy
		Path legacyConfig = worldFolder.resolve("DNBConfig.yml");
		if (Files.isRegularFile(legacyConfig)) {
			try (var br = Files.newBufferedReader(legacyConfig)) {
				return parseLegacy(teamArenaMap, worldFolder.toFile(), new Yaml().load(br));
			} catch (Exception ex) {
				Main.logger().log(Level.WARNING, "Failed to load DNBConfig.yml for " + teamArenaMap.getName(), ex);
			}
		}
		return null;
	}

	public static DigAndBuildInfo parseV1(TeamArenaMap teamArenaMap, YamlConfiguration yamlConfiguration) {
		return null;
	}

	public static DigAndBuildInfo parseLegacy(TeamArenaMap teamArenaMap, File worldFolder, Map<String, Object> dnbMap) {
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
				if (teamArenaMap.teamSpawns.containsKey(entry.getKey())) {
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
		return null;
//		return new DigAndBuildInfo(middle, oreType, tools, blocks, noBuildZones, statusOres, teamInfo);
	}
}
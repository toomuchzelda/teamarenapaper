package me.toomuchzelda.teamarenapaper.teamarena.digandbuild;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.digandbuild.upgrades.HasteUpgradeInfo;
import me.toomuchzelda.teamarenapaper.teamarena.digandbuild.upgrades.HealUpgradeInfo;
import me.toomuchzelda.teamarenapaper.teamarena.digandbuild.upgrades.TrapUpgradeInfo;
import me.toomuchzelda.teamarenapaper.teamarena.digandbuild.upgrades.UpgradeSpawning;
import me.toomuchzelda.teamarenapaper.teamarena.map.TeamArenaMap;
import me.toomuchzelda.teamarenapaper.utils.*;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;

public class DigAndBuildInfo {
	/** Info per team for dnb */
	public record DNBTeamInfo(BlockCoords oreCoords, double protectionRadius, @Nullable BlockCoords teamChest) {}

	public record DNBStatusOreInfo(Material oreType, Material itemType, int required, List<Vector> hologramLocs,
								   List<BlockCoords> coords) {}

	public int configVersion;
	public Vector middle;
	@Nullable @ConfigOptional
	public BlockData defaultLifeOreBlock;
	public record LifeOreInfo(BlockCoords location,
							  double protectionRadius, @ConfigOptional Double interactionRadius,
							  @Nullable @ConfigOptional BlockData block, boolean showHologram) {
		public LifeOreInfo {
			if (interactionRadius == null)
				interactionRadius = protectionRadius;
		}
	}
	public record TeamInfo(@Nullable @ConfigOptional BlockCoords chest, List<LifeOreInfo> lifeOres) {}

	public Map<String, TeamInfo> teams;

	public List<IntBoundingBox> noBuildZones;

	@ConfigPath("team-upgrades.heal")
	public HealUpgradeInfo healUpgrade;

	@ConfigPath("team-upgrades.trap")
	public TrapUpgradeInfo trapUpgrade;

	@ConfigPath("team-upgrades.haste")
	public HasteUpgradeInfo hasteUpgrade;

	public record ItemFountain(Vector at, int interval, List<Material> sequence) {}
	public List<ItemFountain> itemFountains;

	public List<Material> defaultTools;
	public List<Material> defaultBlocks;

	@ConfigPath("__replace-wool-with-team-color")
	public boolean specialReplaceWoolWithTeamColor;
	@ConfigPath("__instantly-prime-tnt")
	public boolean specialInstantlyPrimeTnt;
	@ConfigPath("__no-block-regeneration")
	public boolean specialNoBlockRegeneration;

	@Nullable
	public static DigAndBuildInfo parse(TeamArenaMap teamArenaMap, Path worldFolder) {
		Path config = worldFolder.resolve("map-config-dnb.yml");
		if (Files.isRegularFile(config)) {
			try (var br = Files.newBufferedReader(config)) {
				Map<String, Object> yaml = new Yaml().load(br);
				int configVersion = (Integer) yaml.get("config-version");
				return switch (configVersion) {
					case 1 -> parseV1(yaml);
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
				DigAndBuildInfo dnbInfo = new DigAndBuildInfo();
				dnbInfo.loadLegacy(teamArenaMap, worldFolder.toFile(), new Yaml().load(br));
				return dnbInfo;
			} catch (Exception ex) {
				Main.logger().log(Level.WARNING, "Failed to load DNBConfig.yml for " + teamArenaMap.getName(), ex);
			}
		}
		return null;
	}

	public static DigAndBuildInfo parseV1(Map<String, Object> yaml) {
		return ConfigUtils.parseConfig(yaml, DigAndBuildInfo.class);
	}

	public void loadLegacy(TeamArenaMap teamArenaMap, File worldFolder, Map<String, Object> dnbMap) {
		try {
			middle = BlockUtils.parseCoordsToVec((String) dnbMap.get("Middle"), 0.5, 0.5, 0.5);
		}
		catch (NullPointerException | ClassCastException e) {
			Main.logger().warning("Invalid Middle value in DNB config for " + worldFolder.getName() + ". " + e.getMessage());
			throw e;
		}

		try {
			String oreTypeStr = (String) dnbMap.get("OreType");
			defaultLifeOreBlock = Material.valueOf(oreTypeStr).createBlockData();
		}
		catch (ClassCastException | NullPointerException e) {
			Main.logger().warning("Invalid OreType value in DNB config for " + worldFolder.getName() + ". " + e.getMessage());
			throw e;
		}

		try {
			List<String> toolsStrList = (List<String>) dnbMap.get("Tools");
			defaultTools = new ArrayList<>(toolsStrList.size());
			for (String s : toolsStrList) {
				defaultTools.add(Material.valueOf(s));
			}
		}
		catch (ClassCastException | NullPointerException e) {
			Main.logger().warning("Invalid Tools value in DNB config for " + worldFolder.getName() + ". " + e.getMessage());
			defaultTools = new ArrayList<>(0); // Default to no tools
		}

		try {
			List<String> blocksStrList = (List<String>) dnbMap.get("Blocks");
			defaultBlocks = new ArrayList<>(blocksStrList.size());
			for (String s : blocksStrList) {
				defaultBlocks.add(Material.valueOf(s));
			}
		}
		catch (ClassCastException | NullPointerException e) {
			Main.logger().warning("Invalid Blocks value in DNB config for " + worldFolder.getName() + ". " + e.getMessage());
			defaultBlocks = new ArrayList<>(0); // Default to no tools
		}

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

		Map<String, DNBStatusOreInfo> statusOres;
		try {
			Map<String, Map<String, Object>> oresConfig = (Map<String, Map<String, Object>>) dnbMap.get("StatusOres");
			statusOres = new HashMap<>();
			for (var entry : oresConfig.entrySet()) {
				String type = entry.getKey().toUpperCase(Locale.ENGLISH);
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
		statusOres.forEach((statusOreType, statusOreInfo) -> {
			switch (statusOreType) {
				case "HEAL" -> healUpgrade = new HealUpgradeInfo(
					statusOreInfo.itemType, null, statusOreInfo.required, null,
					new UpgradeSpawning(statusOreInfo.hologramLocs, statusOreInfo.oreType.createBlockData(), null, statusOreInfo.coords)
				);
				case "HASTE" -> hasteUpgrade = new HasteUpgradeInfo(
					statusOreInfo.itemType, null, statusOreInfo.required,
					new UpgradeSpawning(statusOreInfo.hologramLocs, statusOreInfo.oreType.createBlockData(), null, statusOreInfo.coords)
				);
			}
		});

		Map<String, DNBTeamInfo> teamInfo;
		try {
			Map<String, Map<String, Object>> teamConfigs = (Map<String, Map<String, Object>>) dnbMap.get("Teams");
			teamInfo = new HashMap<>();
			for (var entry : teamConfigs.entrySet()) {
				if (teamArenaMap.getTeamSpawns().containsKey(entry.getKey())) {
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
		teams = new LinkedHashMap<>();
		teamInfo.forEach((team, info) -> {
			teams.put(team, new TeamInfo(info.teamChest, List.of(new LifeOreInfo(
				info.oreCoords, info.protectionRadius, null, null, true
			))));
		});
	}
}

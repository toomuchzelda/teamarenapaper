package me.toomuchzelda.teamarenapaper.teamarena.kits.engineer;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.Inventories;
import me.toomuchzelda.teamarenapaper.metadata.MetaIndex;
import me.toomuchzelda.teamarenapaper.metadata.MetadataViewer;
import me.toomuchzelda.teamarenapaper.scoreboard.PlayerScoreboard;
import me.toomuchzelda.teamarenapaper.teamarena.building.BuildingInventory;
import me.toomuchzelda.teamarenapaper.teamarena.building.BuildingManager;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.KitCategory;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.RayTraceResult;

import java.util.HashMap;
import java.util.Map;

//Kit Description:
/*
    Kit Goal: Utility/Ranged
    Ideally, Defensive and Offensive play should be equally viable

    Main Ability: Sentry
        Active Time / CD = ~15 seconds
        Similar to TF2 Sentry, Sentry will rotate 90 degrees total.
        Turret will slowly rotate back and forth within its range.
        Initial angle at which sentry is placed is determined by where the builder is looking.
        Turret will target the closest enemy within its sight

        Upon placement, the turret will self-destruct after its Active Time is up.

        Active sentry(s) can be manually aimed and fired with the Wrangler

        Turret Further Details:
            -Visible Angle: 90 Degrees (Able to turn 360 degrees to track a locked-on enemy)
            -Health: 20 Hearts + Full Leather Armor
            -Fire-Rate: 1 shot every second (20 ticks)
            -DMG: 1.35 DMG per shot
            -Completely impervious to Invis (Give ghost some usefulness back)

    Sub Ability: Teleporter
        RWF Tele, but no more remote teleporter
        Added set-up time and cooldown to building TPs

    Sub Ability: Destruction PDA
    	Instantly destroy buildings remotely.

    Note: Spies disguised as allies are seen as teammates by buildings as well
    (Spies can use teleporters and will not be targetted by sentries if disguised)
*/

/**
 * @author onett425
 */
public class KitEngineer extends Kit {
	public static final ItemStack SENTRY;
	public static final ItemStack WRANGLER;
	static final Team[] COLOUR_TEAMS;
	static final Team RED_GLOWING_TEAM;
	static final Team GREEN_GLOWING_TEAM;
	private static final String PROJECTION_STATUS = "ProjectionStatus";

	static {
		SENTRY = new ItemStack(Material.CHEST_MINECART);
		ItemMeta sentryMeta = SENTRY.getItemMeta();
		sentryMeta.displayName(ItemUtils.noItalics(Component.text("Sentry Kit")));
		SENTRY.setItemMeta(sentryMeta);

		WRANGLER = new ItemStack(Material.STICK);
		ItemMeta wranglerMeta = WRANGLER.getItemMeta();
		wranglerMeta.displayName(ItemUtils.noItalics(Component.text("Manual Sentry Fire")));
		WRANGLER.setItemMeta(wranglerMeta);

		//Stolen from toomuchzelda
		//Sentry Projection changes color based on whether it is a valid spot or not
		COLOUR_TEAMS = new Team[2];

		NamedTextColor[] matchingColours = new NamedTextColor[]{NamedTextColor.RED, NamedTextColor.GREEN};

		for (int i = 0; i < 2; i++) {
			COLOUR_TEAMS[i] = PlayerScoreboard.SCOREBOARD.registerNewTeam(PROJECTION_STATUS + matchingColours[i].value());
			COLOUR_TEAMS[i].color(matchingColours[i]);
			COLOUR_TEAMS[i].setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);

			PlayerScoreboard.addGlobalTeam(COLOUR_TEAMS[i]);
		}

		//Red = Invalid, Green = Valid
		RED_GLOWING_TEAM = COLOUR_TEAMS[0];
		GREEN_GLOWING_TEAM = COLOUR_TEAMS[1];
	}

	public KitEngineer() {
		super("Engineer", "A utility kit that uses its buildings to support its team. " +
				"Gun down enemies with your automatic sentry and set up teleporters to " +
				"transport your allies across the map!", Material.IRON_SHOVEL);

		ItemStack pants = new ItemStack(Material.LEATHER_LEGGINGS);
		LeatherArmorMeta pantsMeta = (LeatherArmorMeta) pants.getItemMeta();
		pantsMeta.setColor(Color.WHITE);
		pants.setItemMeta(pantsMeta);
		setArmor(new ItemStack(Material.GOLDEN_HELMET), new ItemStack(Material.IRON_CHESTPLATE),
				pants, new ItemStack(Material.GOLDEN_BOOTS));

		ItemStack wrench = new ItemStack(Material.IRON_SHOVEL);
		ItemMeta wrenchMeta = wrench.getItemMeta();
		wrenchMeta.displayName(ItemUtils.noItalics(Component.text("Wrench")));
		wrench.setItemMeta(wrenchMeta);

		ItemStack tele = new ItemStack(Material.QUARTZ);
		ItemMeta teleMeta = tele.getItemMeta();
		teleMeta.displayName(ItemUtils.noItalics(Component.text("Create Teleporter")));
		tele.setItemMeta(teleMeta);

		ItemStack pda = new ItemStack(Material.BOOK);
		ItemMeta pdaMeta = pda.getItemMeta();
		pdaMeta.displayName(ItemUtils.noItalics(Component.text("Destruction PDA")));
		pda.setItemMeta(pdaMeta);

		setItems(wrench, SENTRY, tele, pda);
		setAbilities(new EngineerAbility());

		setCategory(KitCategory.SUPPORT);
	}

	public static class EngineerAbility extends Ability {

		public static final Map<Player, Mob> activePlayerProjections = new HashMap<>();
		@Deprecated // temporary API
		public static final Map<LivingEntity, Sentry> sentryEntityToSentryMap = new HashMap<>();
		//SENTRY_CD should be 300, it may be altered for testing purposes
		public static final int SENTRY_CD = 300;
		public static final int SENTRY_PLACEMENT_RANGE = 3;


		//Note: Currently designed so buildings persist even if engineer dies
		//Modifications will be made to accommodate for SnD, so buildings die when engineer dies
		@Override
		public void registerAbility() {
			//Cleaning up is done in registerAbility so structures remain after game ends
			activePlayerProjections.clear();
			sentryEntityToSentryMap.clear();
		}

		@Override
		public void unregisterAbility() {
			//Cleaning up display of sentry projection color
			for (Team team : COLOUR_TEAMS) {
				PlayerScoreboard.removeEntriesAll(team, team.getEntries());
				team.removeEntries(team.getEntries());
			}
		}

		public void giveAbility(Player player) {

		}

		public void removeAbility(Player player) {
			if (activePlayerProjections.containsKey(player)) {
				destroyProjection(player);
			}
			Inventories.closeInventory(player, BuildingInventory.class);
			// remove all player buildings
			BuildingManager.getAllPlayerBuildings(player).forEach(BuildingManager::destroyBuilding);
		}

		@Override
		public void onInteract(PlayerInteractEvent event) {
			if (!event.getAction().isRightClick())
				return;
			if (event.useItemInHand() == Event.Result.DENY)
				return;

			Player player = event.getPlayer();
			ItemStack item = event.getItem();
			Material mat = event.getMaterial();
			Block block = event.getClickedBlock();
			BlockFace blockFace = event.getBlockFace();

			// will be uncancelled later if not handled
			event.setUseItemInHand(Event.Result.DENY);
			event.setUseInteractedBlock(Event.Result.DENY);
			if (mat == Material.QUARTZ) {
				// Creating / Destroying Teleporters
				// validate placement first
				if (block == null || blockFace != BlockFace.UP || !BuildingManager.isLocationValid(block.getRelative(BlockFace.UP))) {
					return;
				}
				Component message;

				// check if block occupied
				var building = BuildingManager.getBuildingAt(block);
				if (building != null) {
					if (building instanceof Teleporter teleporter && teleporter.owner.equals(player)) {
						// break player's own teleporter
						BuildingManager.destroyBuilding(teleporter);
						message = Component.text("You removed your teleporter.", NamedTextColor.BLUE);
					} else {
						message = Component.text("Another teleporter already exists as this spot.", TextUtils.ERROR_RED);
					}
				} else {
					var playerTeleporters = BuildingManager.getPlayerBuildings(player, Teleporter.class);
					//Creating TP
					if (playerTeleporters.size() >= 2) {
						//Failure: 2 TPs already exist
						message = Component.text("Two teleporters are already active! Destroy one with your Destruction PDA!", TextUtils.ERROR_RED);
					} else {
						//Success: TP is created
						var teleporter = new Teleporter(player, block.getLocation());
						BuildingManager.placeBuilding(teleporter);
						if (playerTeleporters.size() == 1) {
							//Syncing the Cooldowns for the newly created TP.
							int lastUsedTick = teleporter.getLastUsedTick();
							playerTeleporters.get(0).setLastUsedTick(lastUsedTick);
						}
						message = Component.text("Successfully placed your teleporter.", NamedTextColor.GREEN);
					}
				}
				player.sendMessage(message);
			} else if (mat == Material.CHEST_MINECART) {
				//Initializing Sentry Build
				if (activePlayerProjections.containsKey(player) &&
						isValidProjection(activePlayerProjections.get(player).getLocation()) &&
						!player.hasCooldown(Material.CHEST_MINECART)) {
					createSentry(player, item);
				}
			} else if (mat == Material.STICK) {
				//Manual Fire using Wrangler
				if (!player.hasCooldown(Material.STICK)) {
					BuildingManager.getPlayerBuildings(player, Sentry.class).stream()
							.filter(sentry -> sentry.currState != Sentry.State.STARTUP)
							.forEach(sentry -> sentry.shoot(sentry.sentry.getLocation().getDirection()));
					player.setCooldown(Material.STICK, Sentry.SENTRY_FIRE_RATE);
				}
			} else if (mat == Material.BOOK) {
				// Destruction PDA
				Inventories.openInventory(player, new BuildingInventory());
			} else {
				// undo cancelling if not handled
				event.setUseItemInHand(Event.Result.DEFAULT);
				event.setUseInteractedBlock(Event.Result.DEFAULT);
			}

		}

		//Converts the Projection into a Sentry + Handles static hashmaps + Inventory
		public void createSentry(Player player, ItemStack currItem) {
			PlayerInventory inv = player.getInventory();
			boolean isMainHand = inv.getItemInMainHand().equals(currItem);
			Mob projection = activePlayerProjections.get(player);
			Sentry sentry = new Sentry(player, projection);
			BuildingManager.placeBuilding(sentry);

			sentryEntityToSentryMap.put(projection, sentry);
			activePlayerProjections.remove(player);
			Main.getPlayerInfo(player).getMetadataViewer().removeViewedValues(projection);
			player.setCooldown(Material.CHEST_MINECART, SENTRY_CD);

			toggleWranglerItem(player, true);
		}

		//Destroys sentry + Handles static hashmaps + Inventory
		public void destroySentry(Player player, Sentry sentry) {
			BuildingManager.destroyBuilding(sentry);
			sentryEntityToSentryMap.remove(sentry.sentry);

			toggleWranglerItem(player, false);
		}

		public void toggleWranglerItem(Player player, boolean show) {
			PlayerInventory inventory = player.getInventory();
			boolean hasActiveSentries = BuildingManager.getPlayerBuildings(player, Sentry.class).size() != 0;
			boolean hasSentryItem = false;
			for (var iterator = inventory.iterator(); iterator.hasNext(); ) {
				var stack = iterator.next();
				if (SENTRY.isSimilar(stack)) {
					hasSentryItem = true;
					if (show) { // show wrangler item, replace
						iterator.set(WRANGLER);
					}
				} else if (WRANGLER.isSimilar(stack)) {
					if (!show && !hasActiveSentries) { // only replace when no active sentries exist
						iterator.set(SENTRY);
						hasSentryItem = true;
					}
				}
			}

			if (!show && hasActiveSentries && !hasSentryItem) {
				// only add item if not already there
				inventory.addItem(SENTRY);
			}
		}

		@Override
		public void onPlayerTick(Player player) {
			//Initializing Sentry Projection
			if (PlayerUtils.isHolding(player, SENTRY) &&
					!activePlayerProjections.containsKey(player) &&
					!player.hasCooldown(Material.CHEST_MINECART)) {
				createProjection(player);
			}

			//Cancel Sentry Projection
			if ((!PlayerUtils.isHolding(player, SENTRY) ||
					player.hasCooldown(Material.CHEST_MINECART)) &&
					activePlayerProjections.containsKey(player)) {
				destroyProjection(player);
			}

			//Controlling position of Sentry Projection
			if (activePlayerProjections.containsKey(player) &&
					!player.hasCooldown(Material.CHEST_MINECART)) {
				LivingEntity projection = activePlayerProjections.get(player);
				//Y Coordinate is lowered so the projection doesn't obstruct the Engineer's view
				Location playerLoc = player.getEyeLocation().clone().add(0, -0.8, 0);
				Location projPos = projectSentry(playerLoc, player);
				projection.teleport(projPos);

				//Handling color display that indicates validity of current sentry location
				if (isValidProjection(projPos)) {
					Main.getPlayerInfo(player).getScoreboard().addMembers(GREEN_GLOWING_TEAM, projection);
				} else {
					Main.getPlayerInfo(player).getScoreboard().addMembers(RED_GLOWING_TEAM, projection);
				}
			}

			//Creating Wrangler "laser beam" and manipulating sentry direction
			if (PlayerUtils.isHolding(player, WRANGLER)) {
				wranglerProjection(player);
			}

			//Extra check to ensure wrangler is replaced if there are no active sentries
			toggleWranglerItem(player, false);
		}

		public void wranglerProjection(Player player) {
			Location playerTarget = findBlock(player.getEyeLocation(), 100);

			BuildingManager.getPlayerBuildings(player, Sentry.class).stream()
					//First remove sentries that are currently starting up
					.filter(sentry -> sentry.currState != Sentry.State.STARTUP)
					//Calculate projected path for each sentry and make them look at that spot
					.forEach(sentry -> {
						sentry.currState = Sentry.State.WRANGLED;
						sentry.forceTarget(playerTarget);
					});
		}

		public boolean isValidProjection(Location projPos) {
			Block baseBlock = projPos.clone().add(0, -0.1, 0).getBlock();
			return baseBlock.getRelative(0, 2, 0).isReplaceable() &&
					baseBlock.getRelative(0, 1, 0).isReplaceable() &&
					baseBlock.isSolid();
		}

		public void createProjection(Player player) {
			Location loc = projectSentry(player.getEyeLocation().clone().add(0, -.8, 0), player);
			TextColor teamColorText = Main.getPlayerInfo(player).team.getRGBTextColor();
			LivingEntity projection = player.getWorld().spawn(loc, Skeleton.class, entity -> {
				entity.setAI(false);
				entity.setCollidable(false);
				entity.setInvisible(true);
				entity.setRemoveWhenFarAway(false);
				entity.setInvulnerable(true);
				entity.setShouldBurnInDay(false);
				entity.getEquipment().clear();
				entity.setCanPickupItems(false);
				entity.setSilent(true);

				MetadataViewer metaViewer = Main.getPlayerInfo(player).getMetadataViewer();
				metaViewer.setViewedValue(MetaIndex.BASE_ENTITY_META,
						MetaIndex.GLOWING_METADATA, entity.getEntityId(), entity);

				activePlayerProjections.put(player, entity);
			});
		}

		public void destroyProjection(Player player) {
			LivingEntity projection = activePlayerProjections.get(player);
			projection.setInvulnerable(false);
			projection.remove();
			activePlayerProjections.remove(player);
			Main.getPlayerInfo(player).getMetadataViewer().removeViewedValues(projection);
		}

		//From entity's eyes, find the location in their line of sight that is within range
		public Location findBlock(Location loc, double range) {
			var world = loc.getWorld();
			RayTraceResult rayTraceResult = world.rayTraceBlocks(loc, loc.getDirection(), range, FluidCollisionMode.NEVER);
			if (rayTraceResult == null) {
				return loc.clone().add(loc.getDirection().multiply(range));
			} else {
				return rayTraceResult.getHitPosition().toLocation(world);
			}
		}

		public Location projectSentry(Location loc, Player player) {
			Location distance = findBlock(loc, SENTRY_PLACEMENT_RANGE);
			distance.setYaw(loc.getYaw());
			distance.setPitch(0);

			return distance;
		}

		//Cancel damage events where the attacker is an ally of the engineer
		public static void handleSentryAttemptDamage(DamageEvent event) {
			Skeleton skele = (Skeleton) event.getVictim();
			Sentry sentry = sentryEntityToSentryMap.get(skele);
			if (sentry != null) {
				if (event.getFinalAttacker() instanceof Player attacker &&
						!Main.getGame().canAttack(attacker, sentry.owner)) {
					event.setCancelled(true);
				}
			}
		}
	}
}
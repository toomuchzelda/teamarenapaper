package me.toomuchzelda.teamarenapaper.teamarena.kits.engineer;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.Inventories;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.building.Building;
import me.toomuchzelda.teamarenapaper.teamarena.building.BuildingInventory;
import me.toomuchzelda.teamarenapaper.teamarena.building.BuildingManager;
import me.toomuchzelda.teamarenapaper.teamarena.building.BuildingSelector;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.KitCategory;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.GlowUtils;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import me.toomuchzelda.teamarenapaper.utils.TextColors;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static me.toomuchzelda.teamarenapaper.teamarena.kits.engineer.KitEngineer.EngineerAbility.SENTRY_CD;

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

        Active sentry(s) can be mounted by the Engineer and
        manually aimed and fired with the Wrench with slightly faster attack speed

        Turret Further Details:
            -Visible Angle: 90 Degrees (Able to turn 360 degrees to track a locked-on enemy)
            -Health: 20 Hearts + Full Leather Armor
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
	private static final String PROJECTION_STATUS = "ProjectionStatus";

	public static final ItemStack WRENCH = ItemBuilder.of(Material.IRON_SHOVEL)
			.displayName(Component.text("Wrench"))
			.lore(TextUtils.wrapString("Right click to manually fire while mounted on your sentry! " +
					"(with slightly faster fire rate than Automatic fire)", Style.style(TextUtils.RIGHT_CLICK_TO)))
			.build();

	/*public static final ItemStack SENTRY = ItemBuilder.of(Material.CHEST_MINECART)
			.displayName(Component.text("Sentry Kit"))
			.lore(TextUtils.toLoreList("""
							When your Sentry Projection is Green,
							Right click to initialize the building process!
							After Construction, your Sentry will Automatically fire at enemies
							within a <pitch> degree cone of view and <range> block radius.
							It can also be mounted and fired manually using your wrench!
							<brown>Active Time / Cooldown: <cd> seconds""", TextColors.LIGHT_YELLOW,
					Placeholder.unparsed("pitch", String.valueOf(Sentry.SENTRY_PITCH_VIEW)),
					Placeholder.unparsed("range", String.valueOf(Sentry.SENTRY_SIGHT_RANGE)),
					Placeholder.unparsed("cd", String.valueOf(SENTRY_CD / 20)),
					TagResolver.resolver("brown", Tag.styling(TextColors.LIGHT_BROWN))
			))
			.build();*/

	public static final ItemStack SENTRY;
	static {
		List<Component> sentryLore = new ArrayList<>();
		sentryLore.addAll(TextUtils.wrapString("When your Sentry Projection is Green, Right click to initialize the building process!"
				, Style.style(TextUtils.RIGHT_CLICK_TO), 200));

		sentryLore.addAll(TextUtils.wrapString("After Construction, your Sentry will Automatically fire at enemies within a "
				+ Sentry.SENTRY_PITCH_VIEW + " degree cone of view and " + Sentry.SENTRY_SIGHT_RANGE + " block radius." +
				" It can also be mounted and fired manually using your wrench", Style.style(TextColors.LIGHT_YELLOW), 200));

		sentryLore.addAll(TextUtils.wrapString("Active Time / Cooldown: " + (SENTRY_CD / 20) + " seconds", Style.style(TextColors.LIGHT_BROWN), 200));

		SENTRY = ItemBuilder.of(Material.CHEST_MINECART)
				.displayName(Component.text("Sentry Kit"))
				.lore(sentryLore)
				.build();
	}

	public static final ItemStack TP_CREATOR = ItemBuilder.of(Material.QUARTZ)
			.displayName(Component.text("Create Teleporter"))
			.lore(TextUtils.wrapString("Right click the top of a block to create/destroy your teleporter!", Style.style(TextColors.LIGHT_YELLOW)))
			.build();

	public static final ItemStack DESTRUCTION_PDA = ItemBuilder.of(Material.BOOK)
			.displayName(Component.text("Destruction PDA"))
			.lore(Component.text("Right click to manage active buildings!", TextColors.LIGHT_YELLOW))
			.build();

	public KitEngineer() {
		super("Engineer", "A utility kit that uses its buildings to support its team. " +
				"Gun down enemies with your automatic sentry and set up teleporters to " +
				"transport your allies across the map!", Material.IRON_SHOVEL);

		//ItemStack pants = new ItemStack(Material.LEATHER_LEGGINGS);
		//LeatherArmorMeta pantsMeta = (LeatherArmorMeta) pants.getItemMeta();
		//pantsMeta.setColor(Color.WHITE);
		//pants.setItemMeta(pantsMeta);
		setArmor(new ItemStack(Material.GOLDEN_HELMET), new ItemStack(Material.IRON_CHESTPLATE),
				new ItemStack(Material.IRON_LEGGINGS), new ItemStack(Material.GOLDEN_BOOTS));

		setItems(WRENCH, SENTRY, TP_CREATOR, DESTRUCTION_PDA);
		setAbilities(new EngineerAbility());

		setCategory(KitCategory.SUPPORT);
	}

	public static class EngineerAbility extends Ability {

		private static final Map<Player, SentryProjection> activePlayerProjections = new HashMap<>();
		private final Map<Player, BuildingSelector> buildingSelectors = new HashMap<>();

		@Deprecated // temporary API
		private static final Map<Skeleton, Sentry> sentryEntityToSentryMap = new HashMap<>();
		//SENTRY_CD should be 300, it may be altered for testing purposes
		public static final int SENTRY_CD = 300;
		public static final int SENTRY_PLACEMENT_RANGE = 3;

		@Override
		public void registerAbility() {
			//Cleaning up is done in registerAbility so structures remain after game ends
			activePlayerProjections.clear();
			sentryEntityToSentryMap.clear();
		}

		@Override
		public void unregisterAbility() {
		}

		private static final Component SELECTOR_MESSAGE = Component.textOfChildren(
			Component.text("Left click: remove selected", TextUtils.LEFT_CLICK_TO),
			Component.text(" | ", NamedTextColor.GRAY),
			Component.text("Right click: manage buildings", TextUtils.RIGHT_CLICK_TO)
		);
		@Override
		protected void giveAbility(Player player) {
			buildingSelectors.put(player, new BuildingSelector(SELECTOR_MESSAGE, SENTRY, TP_CREATOR, DESTRUCTION_PDA));
		}

		public void removeAbility(Player player) {
			if (activePlayerProjections.containsKey(player)) {
				destroyProjection(player);
			}
			buildingSelectors.remove(player).cleanUp();
			Inventories.closeInventory(player, BuildingInventory.class);
			// remove all player buildings
			BuildingManager.getAllPlayerBuildings(player).forEach(BuildingManager::destroyBuilding);

			player.setCooldown(SENTRY.getType(), 0);
			player.setCooldown(WRENCH.getType(), 0);
		}

		//Modifying sentry fire to deal less KB
		@Override
		public void onAttemptedAttack(DamageEvent event) {
			if(event.getDamageType().is(DamageType.PROJECTILE) &&
					event.getAttacker() instanceof Arrow) {
				event.setDamageType(DamageType.ENGINEER_SENTRY);
			}
		}

		@Override
		public void onInteract(PlayerInteractEvent event) {
			if (event.useItemInHand() == Event.Result.DENY)
				return;

			boolean rightClick = event.getAction().isRightClick();

			Player player = event.getPlayer();
			Material mat = event.getMaterial();
			Block block = event.getClickedBlock();
			BlockFace blockFace = event.getBlockFace();

			// will be uncancelled later if not handled
			event.setUseItemInHand(Event.Result.DENY);
			event.setUseInteractedBlock(Event.Result.DENY);

			//If a player is mounted on a sentry, they can fire it manually w/ wrench
			if(mat == Material.IRON_SHOVEL &&
					!player.hasCooldown(Material.IRON_SHOVEL) &&
					player.getVehicle() instanceof Skeleton skeleton &&
					sentryEntityToSentryMap.get(skeleton) != null){
				Sentry sentry = sentryEntityToSentryMap.get(skeleton);
				sentry.forceFire();
				//a mounted sentry has slightly faster fire rate
				player.setCooldown(Material.IRON_SHOVEL, Sentry.SENTRY_FIRE_RATE * 3 / 4);
			}

			if (rightClick && mat == Material.QUARTZ) {
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
						message = Component.text("Another teleporter already exists as this spot.", TextColors.ERROR_RED);
					}
				} else {
					var playerTeleporters = BuildingManager.getPlayerBuildings(player, Teleporter.class);
					//Creating TP
					if (playerTeleporters.size() >= 2) {
						//Failure: 2 TPs already exist
						message = Component.text("Two teleporters are already active! Destroy one with your Destruction PDA!", TextColors.ERROR_RED);
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
			} else if (rightClick && mat == Material.CHEST_MINECART) {
				//Initializing Sentry Build
				if (activePlayerProjections.containsKey(player) &&
						isValidProjection(activePlayerProjections.get(player).getLocation()) &&
						!player.hasCooldown(Material.CHEST_MINECART)) {
					createSentry(player);
				}
			} else if (mat == Material.BOOK) {
				// Destruction PDA
				if (rightClick) {
					Inventories.openInventory(player, new BuildingInventory());
				} else {
					Building selected = buildingSelectors.get(player).getSelected();
					if (selected != null)
						BuildingManager.destroyBuilding(selected);
				}
			} else {
				// undo cancelling if not handled
				event.setUseItemInHand(Event.Result.DEFAULT);
				event.setUseInteractedBlock(Event.Result.DEFAULT);
			}

		}

		//Converts the Projection into a Sentry + Handles static hashmaps + Inventory
		public void createSentry(Player player) {
			SentryProjection projection = activePlayerProjections.remove(player);
			Skeleton skeleton = player.getWorld().spawn(projection.getLocation(), Skeleton.class);
			Sentry sentry = new Sentry(player, skeleton);
			projection.remove(); //destroy the old projection so it doesn't linger

			BuildingManager.placeBuilding(sentry);

			sentryEntityToSentryMap.put(skeleton, sentry);
			if (player.getGameMode() != GameMode.CREATIVE)
				player.setCooldown(Material.CHEST_MINECART, SENTRY_CD);
		}

		//Destroys sentry + Handles static hashmaps + Inventory
		public void destroySentry(Player player, Sentry sentry) {
			BuildingManager.destroyBuilding(sentry);
			sentryEntityToSentryMap.remove(sentry.sentry);
		}

		@Override
		public void onPlayerTick(Player player) {
			BuildingSelector selector = buildingSelectors.get(player);

			selector.buildingFilter = null;
			selector.message = SELECTOR_MESSAGE;
			if (PlayerUtils.isHolding(player, TP_CREATOR)) {
				selector.buildingFilter = building -> building instanceof Teleporter;
				selector.message = Component.text("Right click: place or remove teleporter", TextUtils.RIGHT_CLICK_TO);
			}


			//Initializing Sentry Projection
			if (PlayerUtils.isHolding(player, SENTRY)) {
				selector.buildingFilter = building -> building instanceof Sentry;
				selector.message = Component.text("Right click: place sentry", TextUtils.RIGHT_CLICK_TO);

				if (!activePlayerProjections.containsKey(player) && !player.hasCooldown(Material.CHEST_MINECART)) {
					createProjection(player);
				}
			}

			selector.tick(player);

			//Cancel Sentry Projection
			if ((!PlayerUtils.isHolding(player, SENTRY) ||
					player.hasCooldown(Material.CHEST_MINECART)) &&
					activePlayerProjections.containsKey(player)) {
				destroyProjection(player);
			}

			//Controlling position of Sentry Projection
			if (activePlayerProjections.containsKey(player) &&
					!player.hasCooldown(Material.CHEST_MINECART)) {
				SentryProjection projection = activePlayerProjections.get(player);
				//Y Coordinate is lowered so the projection doesn't obstruct the Engineer's view
				Location playerLoc = player.getEyeLocation().add(0, -0.8, 0);
				Location projPos = projectSentry(playerLoc);
				projection.move(projPos);

				//Handling color display that indicates validity of current sentry location
				GlowUtils.setPacketGlowing(List.of(player), List.of(projection.getUuid().toString()),
					isValidProjection(projPos) ? NamedTextColor.GREEN : NamedTextColor.RED);
			}

			//If player is riding skeleton (sentry), wrangle it
			if(player.getVehicle() instanceof Skeleton skeleton){
				Sentry sentry = sentryEntityToSentryMap.get(skeleton);
				if(sentry != null){
					sentry.currState = Sentry.State.WRANGLED;
				}
			}

		}

		//Allowing engineers to ride their own sentries and manually aim + fire
		@Override
		public void onInteractEntity(PlayerInteractEntityEvent event) {
			Player rider = event.getPlayer();
			//If the right-clicked mob is a skeleton
			//Check all sentries made by that player but also make sure it is not in STARTUP
			if(event.getRightClicked() instanceof Skeleton skeleton){
				Sentry sentry = sentryEntityToSentryMap.get(skeleton);
				if(sentry != null && sentry.owner.equals(rider) &&
						sentry.currState != Sentry.State.STARTUP){
					skeleton.addPassenger(rider);
				}
			}
		}


		public boolean isValidProjection(Location projLoc) {
			projLoc = projLoc.clone();
			//If the projection is levitating too far off ground, invalidate the current position
			if(projLoc.clone().subtract(0,0.1,0).getBlock().isPassable()){
				return false;
			}

			//base = block beneath projection, ignoring blocks that have partial height (slabs, carpet, etc.)
			//aboveOne = + 1, aboveTwo = +2
			Block baseBlock = projLoc.getBlock().getRelative(BlockFace.DOWN);
			Block aboveOne = baseBlock.getRelative(0, 1, 0);
			Block aboveTwo = baseBlock.getRelative(0, 2, 0);
			Block aboveThree = baseBlock.getRelative(0, 3, 0);

			//for testing only
			//if(TeamArena.getGameTick() % 20 == 0){
			//	Bukkit.broadcast(Component.text("BLOCKS: " +
			//			baseBlock.toString() + aboveOne.toString() + aboveTwo.toString()
			//	+ aboveThree.toString()));
			//}

			//entity is not standing on top of any block
			if(!aboveOne.isCollidable()){
				return baseBlock.isSolid() && !aboveTwo.isCollidable();
			}
			//Validating all blocks which will allow the skeleton to survive in a
			//2 block tall space if placed on
			else if(aboveOne.getBoundingBox().getHeight() <= 0.25){
				return !aboveTwo.isCollidable();
			}
			//On top of this block, the sentry will suffocate in a 2 block space,
			//so check aboveThree
			else if(aboveOne.getBoundingBox().getHeight() > 0.25){
				return !aboveTwo.isCollidable() && !aboveThree.isCollidable();
			}
			else{
				return false;
			}
		}

		public void createProjection(Player player) {
			Location loc = projectSentry(player.getEyeLocation().clone().add(0, -.8, 0));
			SentryProjection projection = new SentryProjection(loc, player);
			projection.respawn();
			activePlayerProjections.put(player, projection);
		}

		public void destroyProjection(Player player) {
			SentryProjection projection = activePlayerProjections.remove(player);
			projection.remove();
		}

		//From entity's eyes, find the location in their line of sight that is within range
		public static Location findBlock(Location loc, double range) {
			var world = loc.getWorld();
			RayTraceResult rayTraceResult = world.rayTraceBlocks(loc, loc.getDirection(), range,
					FluidCollisionMode.NEVER, true);
			if (rayTraceResult == null) {
				return loc.clone().add(loc.getDirection().multiply(range));
			} else {
				return rayTraceResult.getHitPosition().toLocation(world);
			}
		}

		public Location projectSentry(Location loc) {
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

		public static Player getOwnerBySkeleton(Skeleton skeleton) {
			Sentry sentry = sentryEntityToSentryMap.get(skeleton);
			if (sentry != null) {
				return sentry.owner;
			}

			return null;
		}
	}
}
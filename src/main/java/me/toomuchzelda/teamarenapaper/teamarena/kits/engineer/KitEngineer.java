package me.toomuchzelda.teamarenapaper.teamarena.kits.engineer;

import me.toomuchzelda.teamarenapaper.inventory.Inventories;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.building.*;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.KitCategory;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.ProjectileReflectEvent;
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
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;

import java.util.ArrayList;
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
		sentryLore.addAll(TextUtils.wrapString("When your Sentry Projection is Green, Right click to initialize the building process"
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

		//SENTRY_CD should be 300, it may be altered for testing purposes
		public static final int SENTRY_CD = 300;
		public static final int SENTRY_PLACEMENT_RANGE = 3;

		private static final Component RCLICK_PLACE_SENTRY = Component.text("Right click: place sentry", TextUtils.RIGHT_CLICK_TO);
		private static final Component RCLICK_PLACE_TELEPORTER = Component.text("Right click: place teleporter", TextUtils.RIGHT_CLICK_TO);
		private static final Component SELECTOR_MESSAGE = Component.textOfChildren(
			Component.text("Left click: remove selected", TextUtils.LEFT_CLICK_TO),
			Component.text(" | ", NamedTextColor.GRAY),
			Component.text("Right click: manage buildings", TextUtils.RIGHT_CLICK_TO)
		);

		private static final Map<ItemStack, BuildingSelector.Action> SELECTOR_ACTION = Map.of(
			DESTRUCTION_PDA, BuildingSelector.Action.selectBuilding(SELECTOR_MESSAGE),
			SENTRY, BuildingSelector.Action.showEntityPreview(RCLICK_PLACE_SENTRY, Sentry.class, Sentry::new,
				p -> !p.hasCooldown(SENTRY.getType())),
			TP_CREATOR, BuildingSelector.Action.showEntityPreview(RCLICK_PLACE_TELEPORTER, Teleporter.class, Teleporter::new,
				p -> BuildingManager.getPlayerBuildingCount(p, Teleporter.class) < 2)
		);

		@Override
		protected void giveAbility(Player player) {
			BuildingOutlineManager.registerSelector(player, BuildingSelector.fromAction(SELECTOR_ACTION));
		}

		public void removeAbility(Player player) {
			BuildingOutlineManager.unregisterSelector(player);
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
		public void onReflect(ProjectileReflectEvent event) {
			final Player shooter = (Player) event.projectile.getShooter(); // The sentry owner, not the sentry
			event.attackFunc = dEvent -> {
				dEvent.setDamageType(DamageType.ENGINEER_SENTRY_REFLECTED);
				dEvent.setDamageTypeCause(shooter);
			};
		}

		@Override
		public void onInteract(PlayerInteractEvent event) {
			if (event.useItemInHand() == Event.Result.DENY)
				return;

			boolean rightClick = event.getAction().isRightClick();

			final Player player = event.getPlayer();
			BuildingSelector selector = BuildingOutlineManager.getSelector(player);
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
					BuildingManager.getBuilding(skeleton) instanceof Sentry sentry) {
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
						message = Component.text("Another building already exists as this spot.", TextColors.ERROR_RED);
					}
				} else {
					//Creating TP
					if (BuildingManager.getPlayerBuildingCount(player, Teleporter.class) >= 2) {
						//Failure: 2 TPs already exist
						message = Component.text("Two teleporters are already active! Destroy one with your Destruction PDA", TextColors.ERROR_RED);
					} else {
						//Success: TP is created
						if (selector.placePreview(Teleporter.class) != null)
							message = Component.text("Successfully placed your teleporter.", NamedTextColor.GREEN);
						else
							message = Component.text("Cannot build here", TextColors.ERROR_RED);
					}
				}
				player.sendMessage(message);
			} else if (rightClick && mat == Material.CHEST_MINECART) {
				if (!player.hasCooldown(Material.CHEST_MINECART)) {
					Sentry sentry = selector.placePreview(Sentry.class);
					if (sentry != null) {
						// Band aid fix the faulty location used by the building
						Location pointedLoc = sentry.getLocation();
						pointedLoc.setYaw(player.getLocation().getYaw());
						sentry.setLocation(pointedLoc);
						if (player.getGameMode() != GameMode.CREATIVE)
							player.setCooldown(Material.CHEST_MINECART, SENTRY_CD);
					}
				}
			} else if (mat == Material.BOOK) {
				// Destruction PDA
				if (rightClick) {
					Inventories.openInventory(player, new BuildingInventory());
				} else {
					Building selected = selector.getSelected();
					if (selected != null)
						BuildingManager.destroyBuilding(selected);
				}
			} else {
				// undo cancelling if not handled
				event.setUseItemInHand(Event.Result.DEFAULT);
				event.setUseInteractedBlock(Event.Result.DEFAULT);
			}

		}

		@Override
		public void onPlayerTick(Player player) {
			//If player is riding skeleton (sentry), wrangle it
			// now wrangled when mounted

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

		public static Player getOwnerBySkeleton(Skeleton skeleton) {
			Building building = BuildingManager.getBuilding(skeleton);
			if (building instanceof Sentry sentry)
				return sentry.owner;

			return null;
		}
	}
}
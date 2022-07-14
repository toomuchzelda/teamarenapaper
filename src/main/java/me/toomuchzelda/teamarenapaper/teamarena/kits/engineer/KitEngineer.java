package me.toomuchzelda.teamarenapaper.teamarena.kits.engineer;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.Inventories;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.metadata.MetaIndex;
import me.toomuchzelda.teamarenapaper.metadata.MetadataViewer;
import me.toomuchzelda.teamarenapaper.scoreboard.PlayerScoreboard;
import me.toomuchzelda.teamarenapaper.teamarena.building.BuildingInventory;
import me.toomuchzelda.teamarenapaper.teamarena.building.BuildingManager;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.KitCategory;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import me.toomuchzelda.teamarenapaper.utils.TextColors;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
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
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.RayTraceResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static me.toomuchzelda.teamarenapaper.teamarena.kits.KitValkyrie.VALK_AXE_MAT;
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
	static final Team[] COLOUR_TEAMS;
	static final Team RED_GLOWING_TEAM;
	static final Team GREEN_GLOWING_TEAM;
	private static final String PROJECTION_STATUS = "ProjectionStatus";

	public static final ItemStack WRENCH = ItemBuilder.of(Material.IRON_SHOVEL)
			.displayName(Component.text("Wrench"))
			.lore(Component.text("Right click to manually fire while mounted on your sentry!", TextColors.LIGHT_YELLOW),
					Component.text("(with slightly faster fire rate than Automatic fire)", TextColors.LIGHT_BROWN))
			.build();

	public static final ItemStack SENTRY = ItemBuilder.of(Material.CHEST_MINECART)
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
			.build();

	public static final ItemStack TP_CREATOR = ItemBuilder.of(Material.QUARTZ)
			.displayName(Component.text("Create Teleporter"))
			.lore(Component.text("Right click the top of a block to create/destroy your teleporter!", TextColors.LIGHT_YELLOW))
			.build();

	public static final ItemStack DESTRUCTION_PDA = ItemBuilder.of(Material.BOOK)
			.displayName(Component.text("Destruction PDA"))
			.lore(Component.text("Right click to manage active buildings!", TextColors.LIGHT_YELLOW))
			.build();

	static {
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

		public static final Map<Player, SentryProjection> activePlayerProjections = new HashMap<>();
		@Deprecated // temporary API
		public static final Map<LivingEntity, Sentry> sentryEntityToSentryMap = new HashMap<>();
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
			//Cleaning up display of sentry projection color
			for (Team team : COLOUR_TEAMS) {
				PlayerScoreboard.removeEntriesAll(team, team.getEntries());
				team.removeEntries(team.getEntries());
			}
		}

		public void removeAbility(Player player) {
			if (activePlayerProjections.containsKey(player)) {
				destroyProjection(player);
			}
			Inventories.closeInventory(player, BuildingInventory.class);
			// remove all player buildings
			BuildingManager.getAllPlayerBuildings(player).forEach(BuildingManager::destroyBuilding);
		}

		//Modifying sentry fire to deal less KB
		@Override
		public void onAttemptedAttack(DamageEvent event) {
			if(event.getDamageType().is(DamageType.PROJECTILE) &&
					event.getKnockback() != null){
				event.setKnockback(event.getKnockback().multiply(0.5));
			}
		}

		@Override
		public void onInteract(PlayerInteractEvent event) {
			if (!event.getAction().isRightClick())
				return;
			if (event.useItemInHand() == Event.Result.DENY)
				return;

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
			} else if (mat == Material.CHEST_MINECART) {
				//Initializing Sentry Build
				if (activePlayerProjections.containsKey(player) &&
						isValidProjection(activePlayerProjections.get(player).getLocation()) &&
						!player.hasCooldown(Material.CHEST_MINECART)) {
					createSentry(player);
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
		public void createSentry(Player player) {
			SentryProjection projection = activePlayerProjections.remove(player);
			Skeleton skeleton = player.getWorld().spawn(projection.getLocation(), Skeleton.class);
			Sentry sentry = new Sentry(player, skeleton);
			projection.remove(); //destroy the old projection so it doesn't linger

			BuildingManager.placeBuilding(sentry);

			sentryEntityToSentryMap.put(skeleton, sentry);
			player.setCooldown(Material.CHEST_MINECART, SENTRY_CD);
		}

		//Destroys sentry + Handles static hashmaps + Inventory
		public void destroySentry(Player player, Sentry sentry) {
			BuildingManager.destroyBuilding(sentry);
			sentryEntityToSentryMap.remove(sentry.sentry);
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
				SentryProjection projection = activePlayerProjections.get(player);
				//Y Coordinate is lowered so the projection doesn't obstruct the Engineer's view
				Location playerLoc = player.getEyeLocation().add(0, -0.8, 0);
				Location projPos = projectSentry(playerLoc);
				projection.move(projPos);

				//Handling color display that indicates validity of current sentry location
				if (isValidProjection(projPos)) {
					Main.getPlayerInfo(player).getScoreboard().addMembers(GREEN_GLOWING_TEAM, projection.getUuid().toString());
				} else {
					Main.getPlayerInfo(player).getScoreboard().addMembers(RED_GLOWING_TEAM, projection.getUuid().toString());
				}
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
	}
}
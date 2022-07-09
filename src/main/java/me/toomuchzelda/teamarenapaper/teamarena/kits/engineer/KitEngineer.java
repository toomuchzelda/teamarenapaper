package me.toomuchzelda.teamarenapaper.teamarena.kits.engineer;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.Inventories;
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
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.RayTraceResult;

import java.util.*;

import static me.toomuchzelda.teamarenapaper.teamarena.kits.KitValkyrie.VALK_AXE_MAT;
import static me.toomuchzelda.teamarenapaper.teamarena.kits.engineer.KitEngineer.EngineerAbility.SENTRY_CD;
import static me.toomuchzelda.teamarenapaper.utils.TextUtils.LORE_BROWN;
import static me.toomuchzelda.teamarenapaper.utils.TextUtils.LORE_YELLOW;

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

	public static final Component WRENCH_DESC = ItemUtils.noItalics(
			Component.text("Right click to manually fire while mounted on your sentry!")
					.color(LORE_YELLOW));
	public static final Component WRENCH_DESC2 = ItemUtils.noItalics(
			Component.text("(with slightly faster fire rate than Automatic fire)")
					.color(LORE_BROWN));
	public static final List<Component> WRENCH_LORE_LIST;
	public static final ItemStack WRENCH;

	public static final Component SENTRY_DESC = ItemUtils.noItalics(
			Component.text("When your Sentry Projection is Green, ")
					.color(LORE_YELLOW));
	public static final Component SENTRY_DESC2 = ItemUtils.noItalics(
			Component.text("Right click to initialize the building process!")
					.color(LORE_YELLOW));
	public static final Component SENTRY_DESC3 = ItemUtils.noItalics(
			Component.text("After Construction, your Sentry will Automatically fire at enemies")
					.color(LORE_YELLOW));
	public static final Component SENTRY_DESC4 = ItemUtils.noItalics(
			Component.text("within a " + Sentry.SENTRY_PITCH_VIEW + " degree cone of view and " +
							Sentry.SENTRY_SIGHT_RANGE + " block radius.")
					.color(LORE_YELLOW));
	public static final Component SENTRY_DESC5 = ItemUtils.noItalics(
			Component.text("It can also be mounted and fired manually using your wrench!")
					.color(LORE_YELLOW));
	public static final Component SENTRY_DESC6 = ItemUtils.noItalics(
			Component.text("Active Time / Cooldown: " + SENTRY_CD/20 + " seconds")
					.color(LORE_BROWN));
	public static final List<Component> SENTRY_LORE_LIST;
	public static final ItemStack SENTRY;

	public static final Component TP_DESC = ItemUtils.noItalics(
			Component.text("Right click the top of a block to create/destroy your teleporter!")
					.color(LORE_YELLOW));
	public static final List<Component> TP_LORE_LIST;
	public static final ItemStack TP_CREATOR;

	public static final Component PDA_DESC = ItemUtils.noItalics(
			Component.text("Right click to manage active buildings!")
					.color(LORE_YELLOW));
	public static final List<Component> PDA_LORE_LIST;
	public static final ItemStack DESTRUCTION_PDA;

	static {
		WRENCH = new ItemStack(Material.IRON_SHOVEL);
		ItemMeta wrenchMeta = WRENCH.getItemMeta();
		wrenchMeta.displayName(ItemUtils.noItalics(Component.text("Wrench")));
		ArrayList<Component> wrenchLore = new ArrayList<>(2);
		wrenchLore.add(WRENCH_DESC);
		wrenchLore.add(WRENCH_DESC2);
		WRENCH_LORE_LIST = Collections.unmodifiableList(wrenchLore);
		wrenchMeta.lore(WRENCH_LORE_LIST);
		WRENCH.setItemMeta(wrenchMeta);

		SENTRY = new ItemStack(Material.CHEST_MINECART);
		ItemMeta sentryMeta = SENTRY.getItemMeta();
		sentryMeta.displayName(ItemUtils.noItalics(Component.text("Sentry Kit")));
		ArrayList<Component> sentryLore = new ArrayList<>(6);
		sentryLore.add(SENTRY_DESC);
		sentryLore.add(SENTRY_DESC2);
		sentryLore.add(SENTRY_DESC3);
		sentryLore.add(SENTRY_DESC4);
		sentryLore.add(SENTRY_DESC5);
		sentryLore.add(SENTRY_DESC6);
		SENTRY_LORE_LIST = Collections.unmodifiableList(sentryLore);
		sentryMeta.lore(SENTRY_LORE_LIST);
		SENTRY.setItemMeta(sentryMeta);

		TP_CREATOR = new ItemStack(Material.QUARTZ);
		ItemMeta teleMeta = TP_CREATOR.getItemMeta();
		teleMeta.displayName(ItemUtils.noItalics(Component.text("Create Teleporter")));
		ArrayList<Component> tpLore = new ArrayList<>(1);
		tpLore.add(TP_DESC);
		TP_LORE_LIST = Collections.unmodifiableList(tpLore);
		teleMeta.lore(TP_LORE_LIST);
		TP_CREATOR.setItemMeta(teleMeta);

		DESTRUCTION_PDA = new ItemStack(Material.BOOK);
		ItemMeta pdaMeta = DESTRUCTION_PDA.getItemMeta();
		pdaMeta.displayName(ItemUtils.noItalics(Component.text("Destruction PDA")));
		ArrayList<Component> pdaLore = new ArrayList<>(1);
		pdaLore.add(PDA_DESC);
		PDA_LORE_LIST = Collections.unmodifiableList(pdaLore);
		pdaMeta.lore(PDA_LORE_LIST);
		DESTRUCTION_PDA.setItemMeta(pdaMeta);

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

		public static final Map<Player, Mob> activePlayerProjections = new HashMap<>();
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
						isValidProjection(activePlayerProjections.get(player)) &&
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
			Mob projection = activePlayerProjections.get(player);
			Sentry sentry = new Sentry(player, projection);
			BuildingManager.placeBuilding(sentry);

			sentryEntityToSentryMap.put(projection, sentry);
			activePlayerProjections.remove(player);
			Main.getPlayerInfo(player).getMetadataViewer().removeViewedValues(projection);
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
				LivingEntity projection = activePlayerProjections.get(player);
				//Y Coordinate is lowered so the projection doesn't obstruct the Engineer's view
				Location playerLoc = player.getEyeLocation().clone().add(0, -0.8, 0);
				Location projPos = projectSentry(playerLoc);
				projection.teleport(projPos);

				//Handling color display that indicates validity of current sentry location
				if (isValidProjection(activePlayerProjections.get(player))) {
					Main.getPlayerInfo(player).getScoreboard().addMembers(GREEN_GLOWING_TEAM, projection);
				} else {
					Main.getPlayerInfo(player).getScoreboard().addMembers(RED_GLOWING_TEAM, projection);
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


		public boolean isValidProjection(Mob projection) {
			Location projLoc = projection.getLocation().clone();
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
			LivingEntity projection = player.getWorld().spawn(loc, Skeleton.class, entity -> {
				entity.setAI(false);
				entity.setCollidable(false);
				entity.setInvisible(true);
				entity.setRemoveWhenFarAway(false);
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
			projection.remove();
			activePlayerProjections.remove(player);
			Main.getPlayerInfo(player).getMetadataViewer().removeViewedValues(projection);
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

		//Cancels all incoming damage but also,
		// When projection blocks a melee hit,
		//manually calculate if nearby players should be hit "through" the projection or not
		public static void handleProjectionAttemptDamage(DamageEvent event) {
			//First, cancel all damage events that hit registered projections
			List<Map.Entry<Player, Mob>> projections = activePlayerProjections.entrySet().stream()
					.filter(entry -> {
						Mob projection = entry.getValue();
						return projection.equals(event.getVictim());
					}).toList();

			if(!projections.isEmpty()) {
				//Prevent the hit projections from actually taking damage
				//Bukkit.broadcast(Component.text("HIT PROJECTION"));
				event.setCancelled(true);
			}

			//If a projection was hit,
			//check if the projection is intercepting a melee attack
			//Ensure the attacker is a player that is alive
			if(!projections.isEmpty() &&
					event.getFinalAttacker() instanceof Player attacker &&
						event.getDamageType().is(DamageType.MELEE) &&
						Main.getPlayerInfo(attacker).activeKit != null){
				//Ignore cases where Valk Axe is used, since it will already find all enemies in range
				if(attacker.getInventory().getItemInMainHand().getType() == VALK_AXE_MAT &&
						Main.getPlayerInfo(attacker).activeKit.getName().equalsIgnoreCase("Valkyrie")){

				}
				else{
					projections.forEach(entry -> {
						Mob projection = entry.getValue();
						RayTraceResult trace = attacker.getWorld()
								.rayTraceEntities(attacker.getEyeLocation(),
										attacker.getEyeLocation().getDirection(),
										3,
										hitMob -> hitMob instanceof LivingEntity &&
												!hitMob.equals(projection) &&
												!hitMob.equals(attacker)
								);
						//Bukkit.broadcast(Component.text("trace complete"));
						if(trace != null){
							attacker.attack(trace.getHitEntity());
							//Bukkit.broadcast(Component.text("HIT: " + trace.getHitEntity()));
						}
					});
				}
			}
		}
	}
}
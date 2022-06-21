package me.toomuchzelda.teamarenapaper.teamarena.kits.engineer;

import java.util.*;

import me.toomuchzelda.teamarenapaper.inventory.ClickableItem;
import me.toomuchzelda.teamarenapaper.inventory.Inventories;
import me.toomuchzelda.teamarenapaper.inventory.InventoryProvider;
import me.toomuchzelda.teamarenapaper.inventory.PagedInventory;
import me.toomuchzelda.teamarenapaper.scoreboard.PlayerScoreboard;
import me.toomuchzelda.teamarenapaper.teamarena.SidebarManager;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.kits.KitSpy;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import org.bukkit.Location;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

//Kit Description:
/*
    Kit Goal: Utility/Ranged
    Ideally, Defensive and Offensive play should be equally viable

    Main Ability: Sentry
        Active Time / CD = 20 seconds
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
            -DMG: 2 DMG per shot
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
public class KitEngineer extends Kit{
    public static final ItemStack SENTRY;
    public static final ItemStack WRANGLER;
	static final Team[] COLOUR_TEAMS;
	static final Team RED_GLOWING_TEAM;
	static final Team GREEN_GLOWING_TEAM;
	private static final String PROJECTION_STATUS = "ProjectionStatus";

    static{
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

		NamedTextColor[] matchingColours = new NamedTextColor[] {NamedTextColor.RED, NamedTextColor.GREEN};

		for(int i = 0; i < 2; i++) {
			COLOUR_TEAMS[i] = SidebarManager.SCOREBOARD.registerNewTeam(PROJECTION_STATUS + matchingColours[i].value());
			COLOUR_TEAMS[i].color(matchingColours[i]);
			COLOUR_TEAMS[i].setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);

			PlayerScoreboard.addGlobalTeam(COLOUR_TEAMS[i]);
		}

		//Red = Invalid, Green = Valid
		RED_GLOWING_TEAM = COLOUR_TEAMS[0];
		GREEN_GLOWING_TEAM = COLOUR_TEAMS[1];
    }

    public KitEngineer(){
        super("Engineer", "Wrench and Turret and Teleporter", Material.IRON_SHOVEL);

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
    }

    public static class EngineerAbility extends Ability {

        public static final HashMap<Player, List<Building>> ACTIVE_BUILDINGS = new HashMap<>();
        public static final HashMap<Player, List<Teleporter>> ACTIVE_TELEPORTERS = new HashMap<>();
		public static final HashMap<Player, List<Sentry>> ACTIVE_SENTRY = new HashMap<>();
		public static final HashMap<Player, LivingEntity> ACTIVE_PROJECTION = new HashMap<>();
		static final HashMap<Integer, Player> PROJECTION_ID = new HashMap<>(20, 0.4f);

        public static final int TP_CD = 30;
		//SENTRY_CD should be 400, it is altered for testing purposes
		public static final int SENTRY_CD = 100;
		public static final int SENTRY_PLACEMENT_RANGE = 3;


		//Note: Currently designed so buildings persist even if engineer dies
		//Modifications will be made to accommodate for SnD, so buildings die when engineer dies
		@Override
		public void registerAbility() {
			//Cleaning up is done in registerAbility so structures remain after game ends
			ACTIVE_BUILDINGS.forEach((player, buildings) ->
			{
				buildings.forEach((Building::destroy));
			});
			ACTIVE_BUILDINGS.clear();
			ACTIVE_SENTRY.clear();
			ACTIVE_TELEPORTERS.clear();
			ACTIVE_PROJECTION.clear();
			PROJECTION_ID.clear();
		}

		@Override
		public void unregisterAbility(){
			//Cleaning up display of sentry projection color
			for(Team team : COLOUR_TEAMS) {
				PlayerScoreboard.removeEntriesAll(team, team.getEntries());
				team.removeEntries(team.getEntries());
			}
		}
        public void giveAbility(Player player) {
			//Initializes the lists that store active buildings
            if(!ACTIVE_TELEPORTERS.containsKey(player)){
                List<Teleporter> teleporters = new LinkedList<>();
                ACTIVE_TELEPORTERS.put(player, teleporters);
            }
			if(!ACTIVE_SENTRY.containsKey(player)){
				List<Sentry> sentries = new LinkedList<>();
				ACTIVE_SENTRY.put(player, sentries);
			}
            if(!ACTIVE_BUILDINGS.containsKey(player)){
                List<Building> buildings = new LinkedList<>();
                ACTIVE_BUILDINGS.put(player, buildings);
            }
        }

		public void removeAbility(Player player) {
			if(ACTIVE_PROJECTION.containsKey(player)){
				destroyProjection(player);
			}
			Inventories.closeInventory(player, EngineerInventory.class);
		}

        @Override
        public void onInteract(PlayerInteractEvent event) {
            Player player = event.getPlayer();
			PlayerInventory inv = event.getPlayer().getInventory();
			ItemStack item = event.getItem();
            Material mat = event.getMaterial();
            Block block = event.getClickedBlock();
            BlockFace blockFace = event.getBlockFace();

			//Preventing vanilla uses of items
			if(event.useItemInHand() != Event.Result.DENY && (mat == Material.CHEST_MINECART ||
																mat == Material.BOOK ||
																mat == Material.IRON_SHOVEL) &&
										event.getAction() == Action.RIGHT_CLICK_BLOCK) {
				event.setUseItemInHand(Event.Result.DENY);
			}

			//Creating / Destroying Teleporters
            if(mat == Material.QUARTZ &&
					block != null && block.isSolid() &&
            		blockFace == BlockFace.UP &&
					(block.getLocation().clone().add(0, 1, 0).getBlock().getType() == Material.AIR ||
							block.getLocation().clone().add(0, 1, 0).getBlock().isLiquid())){
						List<Teleporter> teleporters = ACTIVE_TELEPORTERS.get(player);
						List<Building> buildings = ACTIVE_BUILDINGS.get(player);
						Location tpLoc = block.getLocation().clone();
						//Breaking TP
						if(findDuplicateTele(teleporters, tpLoc) != null){
							Teleporter dupeTele = findDuplicateTele(teleporters, tpLoc);
							dupeTele.destroy();
							teleporters.remove(dupeTele);
							buildings.remove(dupeTele);
						}
						else{
						//Creating TP
							if(teleporters.size() >= 2){
								//Failure: 2 TPs already exist
								player.sendMessage(Component.text("Two teleporters are already active! Destroy one with your Destruction PDA!"));
							}
							else{
								//Ensuring that 2 TPs cannot exist on the same block
								if(findDuplicateAllTele(tpLoc) == null){
									//Success: TP is created
									Teleporter newTP = new Teleporter(player, tpLoc);
									teleporters.add(newTP);
									buildings.add(newTP);
									if(teleporters.size() == 2){
										//Syncing the Cooldowns for the newly created TP.
										int lastUsedTick = newTP.getLastUsedTick();
										teleporters.get(0).setLastUsedTick(lastUsedTick);
									}
								}
								else{
									//Failure: Another TP from an enemy engineer exists at that spot
									player.sendMessage(Component.text("Another teleporter already exists as this spot."));
								}

							}
						}
            }

			//Initializing Sentry Build
			if(mat == Material.CHEST_MINECART &&
					ACTIVE_PROJECTION.containsKey(player) &&
							isValidProjection(ACTIVE_PROJECTION.get(player).getLocation()) &&
								!player.hasCooldown(Material.CHEST_MINECART)) {
				createSentry(player, item);
			}

			//Manual Fire using Wrangler
			if(mat == Material.STICK && !player.hasCooldown(Material.STICK) &&
					ACTIVE_SENTRY.containsKey(player)){
					List<Sentry> sentries = ACTIVE_SENTRY.get(player);
					sentries.stream().filter(sentry -> sentry.currState != Sentry.State.STARTUP)
							.forEach(sentry -> {
								sentry.shoot(sentry.sentry.getLocation().getDirection());
							});
					player.setCooldown(Material.STICK, Sentry.SENTRY_FIRE_RATE);
			}

			//Destruction PDA
			if(mat == Material.BOOK){
				Inventories.openInventory(event.getPlayer(), new EngineerInventory());
			}
        }

		//Converts the Projection into a Sentry
		public void createSentry(Player player, ItemStack currItem){
			PlayerInventory inv = player.getInventory();
			boolean isMainHand = inv.getItemInMainHand().equals(currItem);
			LivingEntity projection = ACTIVE_PROJECTION.get(player);
			List<Building> buildings = ACTIVE_BUILDINGS.get(player);
			List<Sentry> sentries = ACTIVE_SENTRY.get(player);
			Sentry sentry = new Sentry(player, projection);
			sentries.add(sentry);
			buildings.add(sentry);

			ACTIVE_PROJECTION.remove(player);
			PROJECTION_ID.remove(projection.getEntityId());
			player.setCooldown(Material.CHEST_MINECART, SENTRY_CD);

			if(isMainHand){
				inv.setItemInMainHand(currItem.subtract());
				if(!inv.contains(WRANGLER)){
					inv.setItemInMainHand(WRANGLER);
				}
			}
			else{
				inv.setItemInOffHand(currItem.subtract());
				if(!inv.contains(WRANGLER)){
					inv.setItemInOffHand(WRANGLER);
				}
			}
		}

		//Buildings have a destroy method, but this method is necessary to manage static hashmaps
		//and player inventory/items
		public void destroySentry(Player player, Sentry sentry){
			PlayerInventory inv = player.getInventory();
			List<Building> buildings = ACTIVE_BUILDINGS.get(player);
			List<Sentry> sentries = ACTIVE_SENTRY.get(player);
			sentries.remove(sentry);
			buildings.remove(sentry);
			sentry.destroy();

			if(sentries.isEmpty()){
				//No more sentries are active, so wrangler can now be replaced
				if(inv.getItemInOffHand().equals(WRANGLER)){
					//Must make seperate case for when sentry is in offhand
					inv.setItemInOffHand(SENTRY);
				}
				else {
					int wranglerSlot = inv.first(WRANGLER);
					if (wranglerSlot != -1) {
						//wrangler found, so replace it with sentry
						inv.setItem(wranglerSlot, SENTRY);
					}
				}
			}
			else{
				//Active sentries still exist, so do not replace wrangler.
				inv.addItem(SENTRY);
			}
		}

        //Finding Duplicates for a given engineer
		//returns null if none are found
		public Teleporter findDuplicateTele(List<Teleporter> teleporters, Location loc){
            Teleporter dupeTele = null;
            for(Teleporter tele: teleporters){
                if(tele.getLoc().equals(loc)){
					dupeTele = tele;
                }
            }
            return dupeTele;
        }

		//Finding Duplicates for all currently existing engineers
		//returns null if none are found
		public Teleporter findDuplicateAllTele(Location loc){
			Teleporter dupeTele = null;
			for(Map.Entry<Player, List<Teleporter>> entry : ACTIVE_TELEPORTERS.entrySet()){
				List<Teleporter> activeTPs = entry.getValue();
				for(Teleporter tele : activeTPs){
					if(tele.getLoc().equals(loc)){
						dupeTele = tele;
					}
				}
			}
			return dupeTele;
		}

        @Override
        public void onPlayerTick(Player player) {
            List<Teleporter> teleporters = ACTIVE_TELEPORTERS.get(player);
            Component holoText;

			//Handling teleporting allies
            if(teleporters.size() == 2){
                for(Teleporter teleporter : teleporters){
					if(!teleporter.hasCD()){
						Location teleLoc = teleporter.getTPLoc();
						Collection<Player> players = teleLoc.getNearbyPlayers(0.30);
						players.stream()
								//Allies and spies disguised as allies can use
								//User must be sneaking and be on top of the teleporter block
								.filter((user ->
										(!Main.getGame().canAttack(player, user) ||
												PlayerUtils.isDisguisedAsAlly(player, user)) &&
														user.isSneaking() &&
														user.getLocation().getY() == teleLoc.getY()
									))
								.forEach(user -> {
											useTeleporter(player, user, teleLoc);
										}
								);
					}
				}
            }

            //Display of TP Hologram Text
                if(teleporters.size() == 1){
                    holoText = Component.text("Not Connected");
                    teleporters.get(0).setText(holoText);
                }
                else if(teleporters.size() == 2){
					//Since cooldowns are synced between teleporters, only 1 teleporter must be considered
					Teleporter tele = teleporters.get(0);
                    if(!tele.hasCD()){
                        holoText = Component.text("Teleport Ready");
                    }
                    else{
                        long percCD = Math.round(100 * (double)(tele.getRemainingCD()) / TP_CD);
                        holoText = Component.text("Recharging... " + percCD + "%");
                    }
                    teleporters.forEach((teleporter) -> teleporter.setText(holoText));
                }

			//Initializing Sentry Projection
			if(PlayerUtils.isHolding(player, SENTRY) &&
									!ACTIVE_PROJECTION.containsKey(player) &&
										!player.hasCooldown(Material.CHEST_MINECART)){
				createProjection(player);
			}

			//Cancel Sentry Projection
			if((!PlayerUtils.isHolding(player, SENTRY) ||
					player.hasCooldown(Material.CHEST_MINECART)) &&
						ACTIVE_PROJECTION.containsKey(player)){
				destroyProjection(player);
			}

			//Controlling position of Sentry Projection
			if(ACTIVE_PROJECTION.containsKey(player) &&
					!player.hasCooldown(Material.CHEST_MINECART)){
				LivingEntity projection = ACTIVE_PROJECTION.get(player);
				//Y Coordinate is lowered so the projection doesn't obstruct the Engineer's view
				Location playerLoc = player.getEyeLocation().clone().add(0,-.8,0);
				Location projPos = projectSentry(playerLoc, player);
				projection.teleport(projPos);

				//Handling color display that indicates validity of current sentry location
				if(isValidProjection(projPos)){
					Main.getPlayerInfo(player).getScoreboard().addMembers(GREEN_GLOWING_TEAM, projection);
				}
				else{
					Main.getPlayerInfo(player).getScoreboard().addMembers(RED_GLOWING_TEAM, projection);
				}
			}

			//Sentry behavior is handled in Sentry class
			if(ACTIVE_SENTRY.containsKey(player)){
				List<Sentry> activeSentries = ACTIVE_SENTRY.get(player);
				activeSentries.forEach(Sentry::tick);
				activeSentries.forEach(sentry -> {
					if(sentry.isExpired() || sentry.isDestroyed()){
						destroySentry(player, sentry);
					}
				});
			}

			//Creating Wrangler "laser beam" and manipulating sentry direction
			if(PlayerUtils.isHolding(player, WRANGLER)){
				wranglerProjection(player);
			}
        }

		public void wranglerProjection(Player player){
			List<Sentry> sentries = ACTIVE_SENTRY.get(player);
			Color teamColor = Main.getPlayerInfo(player).team.getColour();
			float currPitch = player.getLocation().getPitch();
			float currYaw = player.getLocation().getYaw();

			sentries.stream()
					//First remove sentries that are currently starting up
					.filter(sentry -> sentry.currState != Sentry.State.STARTUP)
					//Calculate projected path for each sentry and make them look at that spot
					.forEach(
					sentry -> {
						sentry.currState = Sentry.State.WRANGLED;

						//Making sentry look at the same block as player
						Location terminatingPoint = findBlock(player.getEyeLocation(), 100);
						Location initLoc = sentry.sentry.getEyeLocation().clone();
						//sentryLoc is for calculating the beam, sentryPosLoc is for altering pitch + yaw of sentry itself
						Location sentryLoc = sentry.sentry.getEyeLocation().clone();
						Location sentryPosLoc = sentry.sentry.getLocation().clone();
						Vector sentryToTermDir = terminatingPoint.subtract(sentryLoc).toVector().normalize();
						sentryPosLoc.setDirection(sentryToTermDir);
						sentry.sentry.teleport(sentryPosLoc);

						//Creating particle beam
						Vector inc = sentryToTermDir.multiply(0.5);
						Block currBlock = sentryLoc.getBlock();
						Material blockType = currBlock.getType();

						while((blockType == Material.AIR || currBlock.isLiquid() || !currBlock.isCollidable()) &&
								initLoc.distanceSquared(sentryLoc) < Math.pow(100, 2)){
							sentryLoc.add(inc);
							currBlock = sentryLoc.getBlock();
							blockType = currBlock.getType();
							player.getWorld().spawnParticle(Particle.REDSTONE,
									sentryLoc, 1, 0, 0, 0,
									new Particle.DustOptions(teamColor, 0.7f));
						}
					}
			);
		}

		public boolean isValidProjection(Location projPos){
			return !projPos.clone().add(0,2,0).getBlock().isCollidable() &&
					!projPos.clone().add(0,1,0).getBlock().isCollidable() &&
					projPos.clone().add(0,-0.10,0).getBlock().isCollidable();
		}

		public void createProjection(Player player){
			Location loc = projectSentry(player.getEyeLocation().clone().add(0,-.8,0), player);
			TextColor teamColorText = Main.getPlayerInfo(player).team.getRGBTextColor();
			LivingEntity projection = player.getWorld().spawn(loc, Skeleton.class, entity ->{
				entity.setAI(false);
				entity.setCollidable(false);
				entity.setInvisible(true);
				entity.setRemoveWhenFarAway(false);
				entity.setInvulnerable(true);
				entity.setShouldBurnInDay(false);
				entity.getEquipment().clear();
				entity.setCanPickupItems(false);
				entity.setSilent(true);

				PROJECTION_ID.put(entity.getEntityId(), player);
				ACTIVE_PROJECTION.put(player, entity);
			});
		}

		public void destroyProjection(Player player){
			LivingEntity projection = ACTIVE_PROJECTION.get(player);
			projection.setInvulnerable(false);
			projection.remove();
			ACTIVE_PROJECTION.remove(player);
			PROJECTION_ID.remove(projection.getEntityId());
		}

		public static Player getProjection(int id) {
			return PROJECTION_ID.get(id);
		}

		//From entity's eyes, find the location in their line of sight that is within range
		public Location findBlock(Location loc, int range){
			//loc is the eye location of the entity
			Location initLoc = loc.clone();
			Vector direction = loc.getDirection().clone();
			Location distance;
			Vector increment = direction.clone().multiply(0.05);
			Location projLoc = initLoc.clone();
			Block currBlock = loc.getBlock();
			Material blockType = currBlock.getType();
			while((blockType == Material.AIR || currBlock.isLiquid() || !currBlock.isCollidable()) &&
					initLoc.distanceSquared(projLoc) < Math.pow(range, 2)
					&& !isValidProjection(projLoc)){
				projLoc.add(increment);
				currBlock = projLoc.getBlock();
				blockType = currBlock.getType();
			}
			distance = projLoc.clone();
			return distance;
		}

		public Location projectSentry(Location loc, Player player){
			Location distance = findBlock(loc, SENTRY_PLACEMENT_RANGE);
			distance.setYaw(loc.getYaw());
			distance.setPitch(0);

			return distance;
		}

		public void onTick() {
			//Destroys an engineer's buildings if they respawn as a different kit
			Set<Player> currEngineers = ACTIVE_BUILDINGS.keySet();
			currEngineers.forEach((p) -> {
				if(Main.getPlayerInfo(p).activeKit != null &&
						!Main.getPlayerInfo(p).activeKit.getName().equalsIgnoreCase("Engineer")){
					List<Building> buildings = ACTIVE_BUILDINGS.get(p);
					buildings.forEach(Building::destroy);
					ACTIVE_BUILDINGS.remove(p);
					ACTIVE_SENTRY.remove(p);
					ACTIVE_TELEPORTERS.remove(p);
				}
			});
		}

        //Assume there are 2 active TPs
        public void useTeleporter(Player owner, Player teleporting, Location currLoc){
            List<Teleporter> teleporters = ACTIVE_TELEPORTERS.get(owner);
            Location destination = teleporters.get(0).getTPLoc();
            if(destination.equals(currLoc)){
                destination = teleporters.get(1).getTPLoc();
            }
			destination.setYaw(teleporting.getLocation().getYaw());
			destination.setPitch(teleporting.getLocation().getPitch());
			teleporting.teleport(destination);
            teleporters.forEach((teleporter) -> teleporter.setLastUsedTick(TeamArena.getGameTick()));
        }
		public class EngineerInventory extends PagedInventory {

			@Override
			public Component getTitle(Player player) {
				return Component.text("Click Building to Destroy");
			}

			@Override
			public int getRows() {
				return 1;
			}

			@Override
			public void init(Player player, InventoryAccessor inventory) {
				buildPDA(player, inventory);
			}

			@Override
			public void update(Player player, InventoryAccessor inventory) {
				if (TeamArena.getGameTick() % 10 == 0) {
					inventory.invalidate();
				}
			}

			public void buildPDA (Player player, InventoryAccessor inventory) {
				ArrayList<ClickableItem> items = new ArrayList<>();
				List<Building> buildings = ACTIVE_BUILDINGS.get(player);
				List<Teleporter> teleporters = ACTIVE_TELEPORTERS.get(player);
				List<Sentry> sentries = ACTIVE_SENTRY.get(player);

				for(Building building: buildings){
					ItemStack item = new ItemStack(Material.BARRIER);
					if(building.type == Building.BuildingType.SENTRY){
						item = new ItemStack(Material.BOW);
					}
					else if(building.type == Building.BuildingType.TELEPORTER){
						item = new ItemStack(Material.HONEYCOMB_BLOCK);
					}
					ItemMeta meta = item.getItemMeta();
					meta.displayName(ItemUtils.noItalics(Component.text(building.name)));
					Component buildingInfo = Component.text(
							String.format("%.2f", player.getLocation().distance(building.getLoc())) + " blocks away");
					meta.lore(Collections.singletonList(buildingInfo));
					item.setItemMeta(meta);

					items.add(ClickableItem.of(item, e -> {
						if(building.type == Building.BuildingType.SENTRY){
							destroySentry(player, (Sentry) building);
						}
						else if(building.type == Building.BuildingType.TELEPORTER){
							building.destroy();
							buildings.remove(building);
							teleporters.remove(building);
						}

						items.remove(e.getCurrentItem());
						Inventories.closeInventory(player);
							}));

						}
				setPageItems(items, inventory, 0, 9);
			}
		}
    }
}
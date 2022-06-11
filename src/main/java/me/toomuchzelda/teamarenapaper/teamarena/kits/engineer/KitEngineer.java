package me.toomuchzelda.teamarenapaper.teamarena.kits.engineer;

import java.util.*;

import me.toomuchzelda.teamarenapaper.scoreboard.PlayerScoreboard;
import me.toomuchzelda.teamarenapaper.teamarena.SidebarManager;
import me.toomuchzelda.teamarenapaper.teamarena.kits.demolitions.DemoMine;
import me.toomuchzelda.teamarenapaper.teamarena.kits.demolitions.KitDemolitions;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import it.unimi.dsi.fastutil.Hash;

import org.bukkit.Location;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import me.toomuchzelda.teamarenapaper.utils.RealHologram;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

//Kit Description:
/*
    Kit Goal: Utility/Ranged
    Defensive + Offensive play should be equally viable

    WRENCH

    Main Ability: Sentry
        Active Time / CD = 20 seconds
        Similar to TF2 Sentry, visible range is restricted to a cone of view
        Angle at which sentry is placed is determined during placement
        Sentry can be picked up and repositioned, however there is a set-up time.
        Turret will slowly rotate back and forth within its range.
        Turret will target the closest enemy within its sight

        Upon placement, the turret will self destruct after 20 seconds.

        Turret Stats:
            -Visible Angle: 90 Degrees (Able to turn 360 degrees to track a locked-on enemy)
            -Health: 20 Hearts + Full Leather Armor
            -Fire-Rate:
            -DMG:


    Sub Ability: Teleporter
        RWF Tele, but no more remote teleporter
        Add set-up time and cooldown to building TPs
*/
/**
 * @author onett425
 */
public class KitEngineer extends Kit{

    public static final ItemStack SENTRY;
    public static final ItemStack WRANGLER;
    public static final ItemStack ACTIVE_WRANGLER;
	public static final Set<Material> ABILITY_ITEMS = new HashSet<>();
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

        ACTIVE_WRANGLER = new ItemStack(Material.BLAZE_ROD);
        ItemMeta activeWranglerMeta = ACTIVE_WRANGLER.getItemMeta();
        activeWranglerMeta.displayName(ItemUtils.noItalics(Component.text("Manual Sentry Fire")));
        ACTIVE_WRANGLER.setItemMeta(activeWranglerMeta);

		ABILITY_ITEMS.add(SENTRY.getType());
		ABILITY_ITEMS.add(WRANGLER.getType());
		ABILITY_ITEMS.add(ACTIVE_WRANGLER.getType());
		ABILITY_ITEMS.add(Material.QUARTZ);
		ABILITY_ITEMS.add(Material.BOOK);

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
		public static final HashMap<Player, LivingEntity> PREPARING_SENTRY = new HashMap<>();
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
			ACTIVE_TELEPORTERS.clear();
			PREPARING_SENTRY.clear();
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
            if(!ACTIVE_TELEPORTERS.containsKey(player)){
                List<Teleporter> teleporters = new LinkedList<>();
                ACTIVE_TELEPORTERS.put(player, teleporters);
            }
            if(!ACTIVE_BUILDINGS.containsKey(player)){
                List<Building> buildings = new LinkedList<>();
                ACTIVE_BUILDINGS.put(player, buildings);
            }
        }

		public void removeAbility(Player player) {
			if(PREPARING_SENTRY.containsKey(player)){
				destroyProjection(player);
			}
		}

        @Override
        public void onInteract(PlayerInteractEvent event) {
            Player player = event.getPlayer();
			PlayerInventory inv = event.getPlayer().getInventory();
			ItemStack item = event.getItem();
            Material mat = event.getMaterial();
            Block block = event.getClickedBlock();
            BlockFace blockFace = event.getBlockFace();

			if(event.useItemInHand() != Event.Result.DENY && (mat == Material.CHEST_MINECART ||
																mat == Material.BOOK ||
																mat == Material.IRON_SHOVEL)) {
				event.setUseItemInHand(Event.Result.DENY);
			}

			//Creating / Destroying Teleporters
            if(mat == Material.QUARTZ && block != null && block.isSolid() &&
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
							//Failure: Another TP exists at that spot
							player.sendMessage(Component.text("Another teleporter already exists as this spot."));
						}

                    }
                }
            }

			//Initializing Sentry Build
			if(mat == Material.CHEST_MINECART &&
					PREPARING_SENTRY.containsKey(player) &&
							isValidProjection(PREPARING_SENTRY.get(player).getLocation(), player) &&
								!player.hasCooldown(Material.CHEST_MINECART)) {
				LivingEntity projection = PREPARING_SENTRY.get(player);
				Sentry sentry = new Sentry(player, projection);


				PREPARING_SENTRY.remove(player);
				PROJECTION_ID.remove(projection.getEntityId());
				player.setCooldown(Material.CHEST_MINECART, SENTRY_CD);

				inv.setItemInMainHand(item.subtract());
				if(!inv.contains(WRANGLER) || inv.contains(ACTIVE_WRANGLER)){
					inv.setItemInMainHand(WRANGLER);
				}
			}
        }

        //Finding Duplicates for a given engineer
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
            //2 Teleporters exist, so teleporter is active
            //Handling Teleporting teammates
            if(teleporters.size() == 2){
                for(Teleporter teleporter : teleporters){
                    Location teleLoc = teleporter.getTPLoc();
                    Collection<Player> players = teleLoc.getNearbyPlayers(0.30);

                    players.forEach(p -> {
                        if(!Main.getGame().canAttack(player, p) && p.isSneaking()
                        && p.getLocation().getY() == teleLoc.getY() && !teleporter.hasCD()){
                            useTeleporter(p, teleLoc);
                        }
                    }
                    );
                }
				if(ACTIVE_SENTRY.containsKey(player)){

				}
            }

            //Display of TP Hologram Text
                if(teleporters.size() == 1){
                    holoText = Component.text("Not Connected");
                    teleporters.get(0).setText(holoText);
                }
                else if(teleporters.size() == 2){
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
			if(player.getInventory().getItemInMainHand().getType() == Material.CHEST_MINECART &&
									!PREPARING_SENTRY.containsKey(player) &&
										!player.hasCooldown(Material.CHEST_MINECART)){
				createProjection(player);
			}

			//Cancel Sentry Projection bc...
			//1) No Sentry is being held in main hand
			//2) Sentry is on CD
			if(player.getInventory().getItemInMainHand().getType() != Material.CHEST_MINECART &&
							PREPARING_SENTRY.containsKey(player) &&
								!player.hasCooldown(Material.CHEST_MINECART)){
				destroyProjection(player);
			}

			//Controlling projection of Sentry
			if(PREPARING_SENTRY.containsKey(player) &&
					!player.hasCooldown(Material.CHEST_MINECART)){
				LivingEntity projection = PREPARING_SENTRY.get(player);
				//Y Coordinate is lowered so the projection doesn't obstruct the Engineer's view
				Location playerLoc = player.getEyeLocation().clone().add(0,-.8,0);
				Location projPos = projectSentry(playerLoc, player);
				projection.teleport(projPos);

				//Handling color display that indicates validity of current sentry location
				if(isValidProjection(projPos, player)){
					Main.getPlayerInfo(player).getScoreboard().addMembers(GREEN_GLOWING_TEAM, projection);
				}
				else{
					Main.getPlayerInfo(player).getScoreboard().addMembers(RED_GLOWING_TEAM, projection);
				}
			}
        }

		public boolean isValidProjection(Location projPos, Player player){
			Block projBlock = projPos.getBlock();
			return !projPos.clone().add(0,2,0).getBlock().isCollidable() &&
					!projPos.clone().add(0,1,0).getBlock().isCollidable() &&
					!projPos.clone().getBlock().isCollidable() &&
					projPos.clone().add(0,-0.10,0).getBlock().isCollidable();
		}

		public void createProjection(Player player){
			Location loc = projectSentry(player.getLocation().clone(), player);
			TextColor teamColorText = Main.getPlayerInfo(player).team.getRGBTextColor();
			LivingEntity sentry = player.getWorld().spawn(loc, Skeleton.class, entity ->{
				entity.setAI(false);
				entity.setCollidable(false);
				entity.setInvisible(true);
				entity.setRemoveWhenFarAway(false);
				entity.setInvulnerable(true);
				entity.setShouldBurnInDay(false);
				entity.getEquipment().clear();

				entity.customName(Component.text(player.getName() + "'s Sentry").color(teamColorText));
				PROJECTION_ID.put(entity.getEntityId(), player);
				PREPARING_SENTRY.put(player, entity);
			});
		}

		public void destroyProjection(Player player){
			LivingEntity sentry = PREPARING_SENTRY.get(player);
			sentry.setInvulnerable(false);
			sentry.remove();
			PREPARING_SENTRY.remove(player);
			PROJECTION_ID.remove(sentry.getEntityId());
		}

		public static Player getProjection(int id) {
			return PROJECTION_ID.get(id);
		}

		public Location projectSentry(Location loc, Player player){
			//loc is the eye location of the player
			Block aimedBlock = player.getTargetBlock(SENTRY_PLACEMENT_RANGE);
			Location initLoc = loc.clone();
			Vector direction = loc.getDirection().clone();
			Location distance;
			//No obstruction found
				Vector increment = direction.clone().multiply(0.05);
				Location projLoc = initLoc.clone();
				Block currBlock = loc.getBlock();
				Material blockType = currBlock.getType();
				while((blockType == Material.AIR || currBlock.isLiquid() || !currBlock.isCollidable()) &&
						initLoc.distanceSquared(projLoc) < Math.pow(SENTRY_PLACEMENT_RANGE, 2)
						&& !isValidProjection(projLoc, player)){
					projLoc.add(increment);
					currBlock = projLoc.getBlock();
					blockType = currBlock.getType();
				}
				distance = projLoc.clone();
				distance.setYaw(initLoc.getYaw());
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
					ACTIVE_TELEPORTERS.remove(p);
				}
			});
		}

        //Assume there are 2 active TPs
        public void useTeleporter(Player player, Location currLoc){
            List<Teleporter> teleporters = ACTIVE_TELEPORTERS.get(player);
            Location destination = teleporters.get(0).getTPLoc();
            if(destination.equals(currLoc)){
                destination = teleporters.get(1).getTPLoc();
            }
			destination.setYaw(player.getLocation().getYaw());
			destination.setPitch(player.getLocation().getPitch());
            player.teleport(destination);
            teleporters.forEach((teleporter) -> teleporter.setLastUsedTick(TeamArena.getGameTick()));
        }
    }
}
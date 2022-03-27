package me.toomuchzelda.teamarenapaper.teamarena.kits;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.BlockUtils;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class KitDemolitions extends Kit
{
	public KitDemolitions() {
		super("Demolitions", "mines", Material.STONE_PRESSURE_PLATE);
		
		ItemStack sword = new ItemStack(Material.IRON_SWORD);
		sword.addEnchantment(Enchantment.FIRE_ASPECT, 1);
		
		ItemStack minePlacer = new ItemStack(Material.STICK);
		
		setItems(sword, minePlacer);
		
		this.setAbilities(new DemolitionsAbility());
	}
	
	public static class DemolitionsAbility extends Ability
	{
		public static final HashMap<Player, List<DemoMine>> PLAYER_MINES = new HashMap<>();
		public static final HashMap<Axolotl, DemoMine> AXOLOTL_TO_DEMO_MINE = new HashMap<>();
		
		public static final DamageType DEMO_TNTMINE_BYSTANDER = new DamageType(DamageType.DEMO_TNTMINE,
				"%Killed% was blown up by %Killer%'s TNT Mine because %Cause% stepped on it. Thanks a lot!");
		
		public static final DamageType DEMO_TNTMINE_REMOTE = new DamageType(DamageType.DEMO_TNTMINE,
				"%Killed% was blown up %Killer%'s TNT Mine remotely");
		
		@Override
		public void unregisterAbility() {
			PLAYER_MINES.clear();
			AXOLOTL_TO_DEMO_MINE.clear();
		}
		
		@Override
		public void removeAbility(Player player) {
			List<DemoMine> list = PLAYER_MINES.remove(player);
			if(list != null) {
				for (DemoMine mine : list) {
					mine.removeEntites();
					AXOLOTL_TO_DEMO_MINE.remove(mine.axolotl);
				}
			}
		}
		
		@Override
		public void onInteract(PlayerInteractEvent event) {
			if(event.getMaterial() == Material.STICK) {
				Block block = event.getClickedBlock();
				if(block != null) {
					// create a mine
					DemoMine mine = new DemoMine(event.getPlayer(), block, MineType.TNTMINE);
					addMine(mine);
				}
			}
		}
		
		@Override
		public void onAttemptedAttack(DamageEvent event) {
			if(event.getDamageType().is(DamageType.EXPLOSION) && event.getAttacker() instanceof TNTPrimed dTnt) {
				Player demo = (Player) event.getFinalAttacker();
				Player victim = event.getPlayerVictim();
				DemoMine mine = getByTNT(demo, dTnt);
				if(mine != null) {
					if(mine.triggerer == demo) {
						event.setDamageType(DEMO_TNTMINE_REMOTE);
					}
					else if(mine.triggerer == victim) {
						event.setDamageType(DamageType.DEMO_TNTMINE);
					}
					else {
						event.setDamageType(DEMO_TNTMINE_BYSTANDER);
						event.setDamageTypeCause(mine.triggerer);
					}
				}
			}
		}
		
		public static DemoMine getByTNT(Player player, TNTPrimed tnt) {
			List<DemoMine> list = PLAYER_MINES.get(player);
			DemoMine lex_mine = null;
			if(list != null) {
				for(DemoMine mine : list) {
					if(mine.tnt == tnt) {
						lex_mine = mine;
						break;
					}
				}
			}
			return lex_mine;
		}
		
		@Override
		public void onTick() {
			TeamArena tma = Main.getGame();
			
			var axIter = AXOLOTL_TO_DEMO_MINE.entrySet().iterator();
			while(axIter.hasNext()) {
				Map.Entry<Axolotl, DemoMine> entry = axIter.next();
				DemoMine mine = entry.getValue();
				
				if(mine.removeNextTick) {
					Player player = mine.owner;
					List<DemoMine> list = PLAYER_MINES.get(player);
					list.remove(mine);
					if(list.size() == 0) {
						PLAYER_MINES.remove(player);
					}
					
					axIter.remove();
				}
				//determine if needs to be removed (next tick)
				// will prob need a different check for push mines
				else if(mine.type == MineType.TNTMINE && mine.tnt != null && !mine.tnt.isValid()) {
					mine.removeNextTick = true;
				}
				//if it hasn't been stepped on
				else if(mine.triggerer == null) {
					for (Player stepper : Main.getGame().getPlayers()) {
						if (!tma.canAttack(stepper, mine.owner))
							continue;
						
						Axolotl axolotl = entry.getKey();
						if (stepper.getBoundingBox().overlaps(axolotl.getBoundingBox())) {
							//they stepped on mine, trigger explosion
							World world = stepper.getWorld();
							world.playSound(axolotl.getLocation(), Sound.ENTITY_CREEPER_HURT, 1f, 0f);
							world.playSound(axolotl.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1f, 0f);
							
							mine.trigger(stepper);
							
							mine.removeEntites();
						}
					}
				}
			}
		}
		
		public static void handleAxolotlAttemptDamage(DamageEvent event) {
			Axolotl axolotl = (Axolotl) event.getVictim();
			DemoMine mine = AXOLOTL_TO_DEMO_MINE.get(axolotl);
			if(mine != null) {
				event.setCancelled(true);
				if(event.getDamageType().isMelee()) {
					if (event.getFinalAttacker() instanceof Player breaker) {
						if (Main.getGame().canAttack(breaker, mine.owner)) {
							event.setFinalDamage(0d);
							event.setCancelled(false);
						}
						// check MELEE only so not do sweep attacks
						// This may cause issues if other non-punch attacks use MELEE type (maybe something custom?)
						else if(event.getDamageType().is(DamageType.MELEE)){
							breaker.sendMessage(Component.text("This is ").color(NamedTextColor.AQUA).append(
									mine.owner.playerListName()).append(Component.text("'s " + mine.type.name)));
						}
					}
				}
			}
		}
		
		public static void handleAxolotlDamage(DamageEvent event) {
			Axolotl axolotl = (Axolotl) event.getVictim();
			DemoMine mine = AXOLOTL_TO_DEMO_MINE.get(axolotl);
			if (mine != null && event.getDamageType().isMelee()) {
				if(mine.hurt()) {
					if(event.getFinalAttacker() instanceof Player breaker) {
						Component message = Component.text("You've broken one of ").color(NamedTextColor.AQUA).append(
							mine.owner.playerListName()).append(Component.text("'s " + mine.type.name + "s!")
								.color(NamedTextColor.AQUA));
						
						breaker.sendMessage(message);
					}
					
					PLAYER_MINES.remove(mine.owner);
					AXOLOTL_TO_DEMO_MINE.remove(axolotl);
				}
			}
		}
		
		public static void addMine(@NotNull DemoMine mine) {
			Player player = mine.owner;
			List<DemoMine> fromPlayer = PLAYER_MINES.computeIfAbsent(player, demoMines -> {
				return new LinkedList<DemoMine>();
			});
			fromPlayer.add(mine);
			
			AXOLOTL_TO_DEMO_MINE.put(mine.axolotl, mine);
		}
		
		public static void removeMine(@NotNull DemoMine mine) {
			Player player = mine.owner;
			List<DemoMine> list = PLAYER_MINES.get(player);
			list.remove(mine);
			if(list.size() == 0) {
				PLAYER_MINES.remove(player);
			}
			
			AXOLOTL_TO_DEMO_MINE.remove(mine.axolotl);
		}
	}
	
	public static class DemoMine
	{
		public static final EulerAngle LEG_ANGLE = new EulerAngle(1.5708d, 0 ,0); //angle for legs so boots r horizontal
		public static final int MINE_DAMAGE_TO_DIE = 3;
		public static final int TNT_TIME_TO_DETONATE = 20;
		
		public final Player owner;
		private final ArmorStand[] stands;
		private final Axolotl axolotl; //the mine's interactable hitbox
		private TNTPrimed tnt; //the tnt
		private Player triggerer; //store the player that stepped on it for shaming OR the demo if remote detonate
		
		public int damage = 0; //amount of damage it has
		//whether to remove on next tick
		// whether it needs to be removed from hashmaps is checked every tick, and we can't remove it on the same tick
		// as the damage events are processed after the ability tick, so we need to 'schedule' it for removal next tick
		public boolean removeNextTick = false;
		
		public final MineType type;
		
		public DemoMine(Player demo, Block block, MineType type) {
			owner = demo;
			this.type = type;
			
			Color blockColor = BlockUtils.getBlockBukkitColor(block);
			ItemStack leatherBoots = new ItemStack(Material.LEATHER_BOOTS);
			ItemUtils.colourLeatherArmor(blockColor, leatherBoots);
			
			World world = block.getWorld();
			double topOfBlock = BlockUtils.getBlockHeight(block);
			//put downwards slightly so rotated legs lay flat on ground and boots partially in ground
			Location blockLoc = block.getLocation();
			Location baseLoc = blockLoc.add(0.5d, topOfBlock - 0.85d, 0.5d);
			
			Location spawnLoc1 = baseLoc.clone().add(0, 0, 0.5d);
			//put slightly lower to try prevent graphics plane fighting
			Location spawnLoc2 = baseLoc.clone().add(0, -0.005, -0.5d);
			spawnLoc2.setYaw(180f);
			
			stands = new ArmorStand[2];
			stands[0] = (ArmorStand) world.spawnEntity(spawnLoc1, EntityType.ARMOR_STAND);
			stands[1] = (ArmorStand) world.spawnEntity(spawnLoc2, EntityType.ARMOR_STAND);
			
			for (ArmorStand stand : stands) {
				stand.setSilent(true);
				stand.setMarker(true);
				stand.setCanMove(false);
				stand.setCanTick(false);
				stand.setInvulnerable(true);
				stand.setBasePlate(false);
				stand.setInvisible(true);
				stand.setLeftLegPose(LEG_ANGLE);
				stand.setRightLegPose(LEG_ANGLE);
				stand.getEquipment().setBoots(leatherBoots, true);
			}
			
			this.axolotl = (Axolotl) world.spawnEntity(baseLoc.clone().add(0, 0.65, 0), EntityType.AXOLOTL);
			axolotl.setAI(false);
			axolotl.setSilent(true);
			axolotl.setInvisible(true);
		}
		
		public void removeEntites() {
			for (ArmorStand stand : stands) {
				stand.remove();
			}
			axolotl.remove();
		}
		
		/**
		 * @return return true if mine extinguised/removed
		 */
		public boolean hurt() {
			this.damage++;
			World world = this.axolotl.getWorld();
			for(int i = 0; i < 3; i++) {
				world.playSound(axolotl.getLocation(), Sound.BLOCK_GRASS_HIT, 999, 0.5f);
				world.spawnParticle(Particle.CLOUD, axolotl.getLocation().add(0d, 0.2d, 0d), 1,
						0.2d, 0.2d, 0.2d, 0.02d);
			}
			
			if(this.damage >= MINE_DAMAGE_TO_DIE) {
				// game command: /particle minecraft:cloud ~3 ~0.2 ~ 0.2 0.2 0.2 0.02 3 normal
				world.spawnParticle(Particle.CLOUD, axolotl.getLocation().add(0d, 0.2d, 0d), 3,
						0.2d, 0.2d, 0.2d, 0.02d);
				world.playSound(axolotl.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1.5f, 1f);
				world.playSound(axolotl.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1.5f, 1.3f);
				world.playSound(axolotl.getLocation(), Sound.BLOCK_STONE_BREAK, 1.5f, 1f);
				this.removeEntites();
				return true;
			}
			return false;
		}
		
		public void trigger(Player triggerer) {
			this.triggerer = triggerer;
			World world = axolotl.getWorld();
			if(type == MineType.TNTMINE) {
				TNTPrimed tnt = (TNTPrimed) world.spawnEntity(axolotl.getLocation(), EntityType.PRIMED_TNT);
				tnt.setFuseTicks(TNT_TIME_TO_DETONATE);
				tnt.setSource(this.owner);
				tnt.setVelocity(new Vector(0, 0.45d, 0));
				this.tnt = tnt;
			}
			else {
			
			}
		}
	}
	
	public static enum MineType
	{
		TNTMINE("TNT Mine"),
		PUSHMINE("Push Mine");
		
		private final String name;
		
		private MineType(String name) {
			this.name = name;
		}
	}
}

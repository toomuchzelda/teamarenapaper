package me.toomuchzelda.teamarenapaper.teamarena.kits.demolitions;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftArmorStand;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Axolotl;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockVector;
import org.jetbrains.annotations.NotNull;

import java.util.*;


public class KitDemolitions extends Kit
{
	public KitDemolitions() {
		super("Demolitions", "mines", Material.STONE_PRESSURE_PLATE);
		
		ItemStack sword = new ItemStack(Material.IRON_SWORD);
		sword.addEnchantment(Enchantment.FIRE_ASPECT, 1);
		
		ItemStack tntMinePlacer = new ItemStack(Material.STICK);
		
		setItems(sword, tntMinePlacer, new ItemStack(Material.BLAZE_ROD));
		
		this.setAbilities(new DemolitionsAbility());
	}
	
	public static class DemolitionsAbility extends Ability
	{
		public static final HashMap<Player, List<DemoMine>> PLAYER_MINES = new HashMap<>();
		static final HashMap<Integer, DemoMine> ARMOR_STAND_ID_TO_DEMO_MINE = new HashMap<>(20, 0.4f);
		public static final HashMap<Axolotl, DemoMine> AXOLOTL_TO_DEMO_MINE = new HashMap<>();
		public static final HashSet<BlockVector> MINE_POSITIONS = new HashSet<>();
		
		
		public static final DamageType DEMO_TNTMINE_BYSTANDER = new DamageType(DamageType.DEMO_TNTMINE,
				"%Killed% was blown up by %Killer%'s TNT Mine because %Cause% stepped on it. Thanks a lot!");
		
		public static final DamageType DEMO_TNTMINE_REMOTE = new DamageType(DamageType.DEMO_TNTMINE,
				"%Killed% was blown up %Killer%'s TNT Mine remotely");
		
		
		@Override
		public void unregisterAbility() {
			PLAYER_MINES.clear();
			AXOLOTL_TO_DEMO_MINE.clear();
			ARMOR_STAND_ID_TO_DEMO_MINE.clear();
		}
		
		@Override
		public void removeAbility(Player player) {
			removeMines(player);
		}
		
		public static void addMine(@NotNull DemoMine mine) {
			Player player = mine.owner;
			List<DemoMine> fromPlayer = PLAYER_MINES.computeIfAbsent(player, demoMines -> {
				return new ArrayList<>(4);
			});
			fromPlayer.add(mine);
			
			//slightly hacky, but this is already done inside the DemoMine constructor.
			// it needs to be put into this map before the armor stands are spawned so the
			// Metadata packet listener for them will read from this map, and know that it's
			// a mine that needs the glowing effect applied.
			/*for(ArmorStand stand : mine.stands) {
				ARMOR_STAND_ID_TO_DEMO_MINE.put(stand.getEntityId(), mine);
			}*/
			
			AXOLOTL_TO_DEMO_MINE.put(mine.hitboxEntity, mine);
			
			MINE_POSITIONS.add(mine.getBlockVector());
		}
		
		public void removeMines(Player player) {
			List<DemoMine> list = PLAYER_MINES.remove(player);
			if(list != null) {
				var iter = list.iterator();
				while(iter.hasNext()) {
					DemoMine mine = iter.next();
					for(ArmorStand stand : mine.stands) {
						ARMOR_STAND_ID_TO_DEMO_MINE.remove(stand.getEntityId());
					}
					
					AXOLOTL_TO_DEMO_MINE.remove(mine.hitboxEntity);
					
					MINE_POSITIONS.remove(mine.getBlockVector());
					
					mine.removeEntities();
					iter.remove();
				}
			}
		}
		
		public void removeMine(DemoMine mine) {
			List<DemoMine> list = PLAYER_MINES.get(mine.owner);
			if(list != null) {
				list.remove(mine);
			}
			
			for(ArmorStand stand : mine.stands) {
				ARMOR_STAND_ID_TO_DEMO_MINE.remove(stand.getEntityId());
			}
			
			AXOLOTL_TO_DEMO_MINE.remove(mine.hitboxEntity);
			
			MINE_POSITIONS.remove(mine.getBlockVector());
			
			mine.removeEntities();
		}
		
		@Override
		public void onInteract(PlayerInteractEvent event) {
			Block block = event.getClickedBlock();
			Material mat = event.getMaterial();
			if (block != null && (mat == Material.STICK || mat == Material.BLAZE_ROD)) {
				if(!MINE_POSITIONS.contains(block.getLocation().toVector().toBlockVector())) {
					if (mat == Material.STICK) {
						// create a mine
						DemoMine mine = new TNTMine(event.getPlayer(), block);
						addMine(mine);
					}
					else if (mat == Material.BLAZE_ROD) {
						DemoMine mine = new PushMine(event.getPlayer(), block);
						addMine(mine);
					}
				}
				else {
					event.getPlayer().sendMessage(Component.text("A Mine has already been placed here",
							TextUtils.ERROR_RED));
				}
			}
		}
		
		@Override
		public void onAttemptedAttack(DamageEvent event) {
			if(event.getDamageType().is(DamageType.EXPLOSION) && event.getAttacker() instanceof TNTPrimed dTnt) {
				Player demo = (Player) event.getFinalAttacker();
				Player victim = event.getPlayerVictim();
				TNTMine mine = TNTMine.getByTNT(demo, dTnt);
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
					event.setFinalDamage(event.getFinalDamage() * 0.65);
				}
			}
		}
		
		@Override
		public void onTick() {
			int gameTick = TeamArena.getGameTick();
			
			//add mines to be removed to this list and remove afterwards to prevent concurrent modification
			List<DemoMine> toRemove = new LinkedList<>();
			var axIter = AXOLOTL_TO_DEMO_MINE.entrySet().iterator();
			while(axIter.hasNext()) {
				Map.Entry<Axolotl, DemoMine> entry = axIter.next();
				DemoMine mine = entry.getValue();
				
				mine.tick();
				
				if(mine.removeNextTick) {
					/*Player player = mine.owner;
					List<DemoMine> list = PLAYER_MINES.get(player);
					list.remove(mine);
					if(list.size() == 0) {
						PLAYER_MINES.remove(player);
					}
					
					axIter.remove();
					mine.removeEntities();
					MINE_POSITIONS.remove(mine.hitboxEntity.getLocation().toVector().toBlockVector());
					Bukkit.broadcastMessage(MINE_POSITIONS.toString());*/
					
					toRemove.add(mine);
				}
				//determine if needs to be removed (next tick)
				else if(mine.isDone()) {
					mine.removeNextTick = true;
				}
				//if it hasn't been armed yet
				else if(gameTick <= mine.creationTime + DemoMine.TIME_TO_ARM) {
					//indicate its armed
					if(gameTick == mine.creationTime + DemoMine.TIME_TO_ARM) {
						World world = mine.hitboxEntity.getWorld();
						world.playSound(mine.hitboxEntity.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_OFF, 1f, 1f);
						world.spawnParticle(Particle.CRIT, mine.hitboxEntity.getLocation().add(0, 0.4, 0), 2, 0, 0, 0,0);
						
						Component message = Component.text("Your " + mine.type.name + " is now armed").color(NamedTextColor.GREEN);
						PlayerUtils.sendKitMessage(mine.owner, message, message);
					}
					// else do nothing and don't enter the control statement below that checks for collision
				}
				//if it hasn't been stepped on already check if anyone's standing on it
				else if(!mine.isTriggered()) {
					for (Player stepper : Main.getGame().getPlayers()) {
						if (mine.team.getPlayerMembers().contains(stepper))
							continue;
						
						Axolotl axolotl = entry.getKey();
						if (stepper.getBoundingBox().overlaps(axolotl.getBoundingBox())) {
							//they stepped on mine, trigger explosion
							mine.trigger(stepper);
						}
					}
				}
			}
			
			for(DemoMine remove : toRemove) {
				removeMine(remove);
			}
		}
		
		public static void handleAxolotlAttemptDamage(DamageEvent event) {
			Axolotl axolotl = (Axolotl) event.getVictim();
			DemoMine mine = AXOLOTL_TO_DEMO_MINE.get(axolotl);
			if(mine != null) {
				event.setCancelled(true);
				if(event.getDamageType().is(DamageType.MELEE)) {
					if (event.getFinalAttacker() instanceof Player breaker) {
						if(breaker == mine.owner) {
							if(mine.damage == 0)
								breaker.sendMessage(Component.text("This is your mine. Keep punching to remove it")
										.color(NamedTextColor.AQUA));
							event.setFinalDamage(0d);
							event.setCancelled(false);
						}
						else if (!mine.team.getPlayerMembers().contains(breaker)) {
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
						Component message;
						if(breaker != mine.owner) {
							message = Component.text("You've broken one of ").color(NamedTextColor.AQUA).append(
								mine.owner.playerListName()).append(Component.text("'s " + mine.type.name + "s!")
									.color(NamedTextColor.AQUA));
							
							Component ownerMessage = Component.text("Someone broke one of your " + mine.type.name + "s!")
									.color(NamedTextColor.AQUA);
							PlayerUtils.sendKitMessage(mine.owner, ownerMessage, ownerMessage);
						}
						else {
							message = Component.text("Broke your " + mine.type.name).color(NamedTextColor.AQUA);
						}
						breaker.sendMessage(message);
					}
				}
			}
		}
	}
}

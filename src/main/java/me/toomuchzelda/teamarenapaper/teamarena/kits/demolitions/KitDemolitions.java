package me.toomuchzelda.teamarenapaper.teamarena.kits.demolitions;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.metadata.MetadataIndexes;
import me.toomuchzelda.teamarenapaper.metadata.MetadataViewer;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Axolotl;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.*;


public class KitDemolitions extends Kit
{
	public static final ItemStack REMOTE_DETONATOR_ITEM = new ItemStack(Material.FLINT_AND_STEEL);

	public static final int TNT_MINE_COUNT = 2;
	public static final int PUSH_MINE_COUNT = 1;
	public static final ItemStack TNT_MINE_ITEM = new ItemStack(Material.TNT);
	public static final ItemStack PUSH_MINE_ITEM = new ItemStack(Material.WHITE_WOOL);

	//valid blocks for mines to be placed on
	public static final EnumMap<Material, Boolean> VALID_MINE_BLOCKS;

	static {
		VALID_MINE_BLOCKS = new EnumMap<Material, Boolean>(Material.class);

		for(Material mat : Material.values()) {
			if(!mat.isBlock())
				continue;

			if(mat.isOccluding() || mat.name().endsWith("SLAB") || mat.name().endsWith("STAIRS")) {
				VALID_MINE_BLOCKS.put(mat, true);
			}
			else if(mat.name().endsWith("LEAVES")) {
				VALID_MINE_BLOCKS.put(mat, true);
			}
		}
	}


	public KitDemolitions() {
		super("Demolitions", "mines", Material.STONE_PRESSURE_PLATE);

		ItemStack sword = new ItemStack(Material.IRON_SWORD);
		sword.addEnchantment(Enchantment.FIRE_ASPECT, 1);

		//Mine item stack sizes set in giveAbility()
		setItems(sword, TNT_MINE_ITEM, PUSH_MINE_ITEM, REMOTE_DETONATOR_ITEM);

		this.setAbilities(new DemolitionsAbility());

		this.setArmor(new ItemStack(Material.CHAINMAIL_HELMET), new ItemStack(Material.CHAINMAIL_CHESTPLATE),
				new ItemStack(Material.CHAINMAIL_LEGGINGS), new ItemStack(Material.GOLDEN_BOOTS));
	}

	public static boolean isValidMineBlock(Block block) {
		return VALID_MINE_BLOCKS.containsKey(block.getType());
	}

	public record RegeneratingMine(MineType type, int removedTime) {}

	public static class DemolitionsAbility extends Ability
	{
		public static final Map<Player, List<DemoMine>> PLAYER_MINES = new HashMap<>();
		public static final Map<Player, List<RegeneratingMine>> REGENERATING_MINES = new LinkedHashMap<>();
		public static final Map<Player, DemoMine> TARGETTED_MINE = new HashMap<>();

		static final Map<Integer, DemoMine> ARMOR_STAND_ID_TO_DEMO_MINE = new HashMap<>(20, 0.4f);
		public static final Map<Axolotl, DemoMine> AXOLOTL_TO_DEMO_MINE = new LinkedHashMap<>();
		public static final Set<BlockVector> MINE_POSITIONS = new HashSet<>();

		public static final DamageType DEMO_TNTMINE_BYSTANDER = new DamageType(DamageType.DEMO_TNTMINE,
				"%Killed% was blown up by %Killer%'s TNT Mine because %Cause% stepped on it. Thanks a lot!");

		public static final DamageType DEMO_TNTMINE_REMOTE = new DamageType(DamageType.DEMO_TNTMINE,
				"%Killed% was blown up by %Killer%'s TNT Mine remotely");


		@Override
		public void unregisterAbility() {
			PLAYER_MINES.clear();
			REGENERATING_MINES.clear();
			AXOLOTL_TO_DEMO_MINE.clear();
			ARMOR_STAND_ID_TO_DEMO_MINE.clear();
			TARGETTED_MINE.clear();
			MINE_POSITIONS.clear();

			DemoMine.clearTeams();
		}

		@Override
		public void giveAbility(Player player) {
			PlayerInventory inventory = player.getInventory();

			ItemStack playersTNT = ItemUtils.getItemInInventory(TNT_MINE_ITEM, player.getInventory());
			ItemStack playersPush = ItemUtils.getItemInInventory(PUSH_MINE_ITEM, player.getInventory());
			//should not be null
			playersTNT.setAmount(TNT_MINE_COUNT);
			playersPush.setAmount(PUSH_MINE_COUNT);
		}
		@Override
		public void removeAbility(Player player) {
			removeMines(player);
		}

		public static void addMine(@NotNull DemoMine mine) {
			Player player = mine.owner;
			List<DemoMine> fromPlayer = PLAYER_MINES.computeIfAbsent(player, demoMines -> {
				return new ArrayList<>(TNT_MINE_COUNT + PUSH_MINE_COUNT);
			});
			fromPlayer.add(mine);

			//slightly hacky, but this is already done inside the DemoMine constructor.
			// it needs to be put into this map before the armor stands are spawned so the
			// Metadata packet listener for them will read from it and know it's
			// a mine that needs the glowing effect applied.
			/*for(Player viewer : player's teammates) {
				Main.getPlayerInfo(viewer).getMetadataViewer().setViewedValue(0, DemoMine.GLOWING_METADATA, armor stands);
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

			REGENERATING_MINES.remove(player);
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

			Material mat = event.getMaterial();
			int type = 0;
			if(TNT_MINE_ITEM.getType() == mat)
				type = 1;
			else if(PUSH_MINE_ITEM.getType() == mat)
				type = 2;

			Block block = event.getClickedBlock();
			if (type > 0 && block != null && event.getBlockFace() == BlockFace.UP) {
				event.setUseItemInHand(Event.Result.DENY);
				if(isValidMineBlock(block)) {
					if (!MINE_POSITIONS.contains(block.getLocation().toVector().toBlockVector())) {
						if (type == 1) { //tnt mine
							DemoMine mine = new TNTMine(event.getPlayer(), block);
							addMine(mine);
						}
						else { //push mine
							DemoMine mine = new PushMine(event.getPlayer(), block);
							addMine(mine);
						}

						event.getItem().subtract();
					}
					else {
						final Component message = Component.text("A Mine has already been placed here",
								TextUtils.ERROR_RED);
						PlayerUtils.sendKitMessage(event.getPlayer(), message, message);
					}
				}
				else {
					final Component message = Component.text("You can't place a Mine here", TextUtils.ERROR_RED);
					PlayerUtils.sendKitMessage(event.getPlayer(), message, message);
				}
			}
			else if(mat == REMOTE_DETONATOR_ITEM.getType()) {
				Player demo = event.getPlayer();
				event.setUseItemInHand(Event.Result.DENY);
				DemoMine mine = TARGETTED_MINE.get(demo);
				if(mine != null) {
					mine.trigger(demo);
					TARGETTED_MINE.remove(demo);
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
		public void onPlayerTick(Player demo) {
			if(!PlayerUtils.isHolding(demo, REMOTE_DETONATOR_ITEM)) {
				DemoMine mine = TARGETTED_MINE.remove(demo);
				if(mine != null)
					mine.unGlow();

			}
			else {
				//Credit jacky8399 for in-field-of-view algorithm
				Location demoLoc = demo.getEyeLocation();
				Vector demoLocVec = demoLoc.toVector();
				Vector direction = demoLoc.getDirection();

				//create a sort of method-local scope container class
				record targettedMinePair(DemoMine mine, double angle) {}

				List<DemoMine> mines = PLAYER_MINES.get(demo);
				List<targettedMinePair> targetCandidates;

				if (mines != null) {
					targetCandidates = new ArrayList<>(mines.size());
					for (DemoMine mine : mines) {
						if (!mine.isTriggered() && mine.isArmed() &&
								mine.getTargetLoc().distanceSquared(demoLocVec) <= DemoMine.REMOTE_ARMING_DISTANCE_SQRD) {
							Vector playerToPoint = mine.getTargetLoc().clone().subtract(demoLocVec).normalize();
							double angle = playerToPoint.angle(direction);

							if (angle <= DemoMine.TARGETTING_ANGLE) {
								targetCandidates.add(new targettedMinePair(mine, angle));
								/*demoLoc.getWorld().spawnParticle(Particle.CRIT,
										mine.getTargetLoc().toLocation(demoLoc.getWorld()), 1);*/
							}
						}
					}
				}
				else {
					targetCandidates = new ArrayList<>(0);
				}

				//get the one being closest pointed at
				double smallestAngle = 10000d;
				DemoMine targettedMine = null;
				for(targettedMinePair pair : targetCandidates) {
					if(pair.angle() < smallestAngle) {
						smallestAngle = pair.angle();
						targettedMine = pair.mine();
					}
				}

				if(targettedMine != null) {
					if(!targettedMine.glowing)
						targettedMine.glow();

					//unglow the previous mine if any and put the new one in
					final DemoMine finalTargettedMine = targettedMine;
					TARGETTED_MINE.compute(demo, (player, demoMine) -> {
						if(demoMine != null && demoMine != finalTargettedMine) {
							demoMine.unGlow();
						}

						return finalTargettedMine;
					});
				}
				else if(targetCandidates.size() == 0) {
					DemoMine mine = TARGETTED_MINE.remove(demo);
					if(mine != null)
						mine.unGlow();
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
					toRemove.add(mine);
					addRegeneratingMine(mine.owner, mine.type, gameTick);
				}
				//determine if needs to be removed (next tick)
				else if(mine.isDone()) {
					mine.removeNextTick = true;
				}
				//if it hasn't been armed yet
				else if(!mine.isArmed()) {
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

			//tick regenerating mines
			var regPlayersIter = REGENERATING_MINES.entrySet().iterator();
			while(regPlayersIter.hasNext()) {
				var entry = regPlayersIter.next();

				var regMinesIter = entry.getValue().iterator();
				while(regMinesIter.hasNext()) {
					RegeneratingMine regMine = regMinesIter.next();

					//check if time up and give back one mine here
					if(gameTick - regMine.removedTime() >= regMine.type().timeToRegen) {
						Player owner = entry.getKey();
						ItemStack mineItem;
						if(regMine.type() == MineType.TNTMINE) {
							mineItem = TNT_MINE_ITEM;
						}
						else {
							mineItem = PUSH_MINE_ITEM;
						}

						owner.getInventory().addItem(mineItem);

						regMinesIter.remove();
					}
				}

				if(entry.getValue().size() == 0)
					regPlayersIter.remove();
			}
		}

		/**
		 * Add the appropriate metadata for glowing mines for players leaving and joining the team.
		 */
		@Override
		public void onTeamSwitch(Player player, TeamArenaTeam oldTeam, TeamArenaTeam newTeam) {
			//iterate through all mines.
			// if it's old team's mines, make it un-glow.
			// if new team's mines, make it glow
			MetadataViewer metadataViewer = Main.getPlayerInfo(player).getMetadataViewer();
			for(Map.Entry<Axolotl, DemoMine> entry : AXOLOTL_TO_DEMO_MINE.entrySet()) {
				DemoMine mine = entry.getValue();

				if(mine.team == oldTeam) {
					metadataViewer.removeViewedValues(mine.stands);
				}
				else if(mine.team == newTeam) {
					metadataViewer.setViewedValues(MetadataIndexes.BASE_ENTITY_META_INDEX, DemoMine.GLOWING_METADATA,
							mine.stands);
				}

				metadataViewer.refreshViewer(mine.stands);
			}
		}

		public void addRegeneratingMine(Player player, MineType type, int startTime) {
			List<RegeneratingMine> regenningMines = REGENERATING_MINES.computeIfAbsent(player,
					player1 -> new ArrayList<>(TNT_MINE_COUNT + PUSH_MINE_COUNT));

			regenningMines.add(new RegeneratingMine(type, startTime));

			final Component message = Component.text("You'll get mine back in " + (type.timeToRegen / 20) + " seconds",
					NamedTextColor.AQUA);

			PlayerUtils.sendKitMessage(player, message, message);
		}

		public static void handleAxolotlAttemptDamage(DamageEvent event) {
			Axolotl axolotl = (Axolotl) event.getVictim();
			DemoMine mine = AXOLOTL_TO_DEMO_MINE.get(axolotl);
			if(mine != null) {
				event.setCancelled(true);
				if(event.getDamageType().is(DamageType.MELEE)) {
					if (event.getFinalAttacker() instanceof Player breaker) {
						if(breaker == mine.owner) {
							/*if(mine.damage == 0)
								breaker.sendMessage(Component.text("This is your mine. Keep punching to remove it")
										.color(NamedTextColor.AQUA));*/

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

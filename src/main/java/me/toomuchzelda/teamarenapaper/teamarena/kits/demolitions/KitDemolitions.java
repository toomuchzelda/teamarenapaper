package me.toomuchzelda.teamarenapaper.teamarena.kits.demolitions;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.metadata.MetaIndex;
import me.toomuchzelda.teamarenapaper.metadata.MetadataViewer;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.capturetheflag.CaptureTheFlag;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.KitCategory;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import me.toomuchzelda.teamarenapaper.utils.TextColors;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.*;


public class KitDemolitions extends Kit
{
	public static final int TNT_MINE_COUNT = 2;
	public static final int PUSH_MINE_COUNT = 1;
	public static final TextColor TNT_COLOR = TextColor.color(187, 60, 23);
	public static final ItemStack TNT_MINE_ITEM;
	public static final ItemStack PUSH_MINE_ITEM;
	public static final ItemStack REMOTE_DETONATOR_ITEM;

	//valid blocks for mines to be placed on
	private static final boolean[] VALID_MINE_BLOCKS;

	static {
		VALID_MINE_BLOCKS = new boolean[Material.values().length];

		for(Material mat : Material.values()) {
			if(!mat.isBlock())
				continue;

			if(mat.isOccluding() || mat.name().endsWith("SLAB") || mat.name().endsWith("STAIRS")) {
				setValidMineBlock(mat);
			}
			else if(mat.name().endsWith("LEAVES")) {
				setValidMineBlock(mat);
			}
		}

		Style style = Style.style(TextUtils.RIGHT_CLICK_TO).decoration(TextDecoration.ITALIC, false);
		String strUsage = "Right click the top of a block to place the trap down. " +
				"It will triggered by your remote detonator or when enemies step on it.";
		List<Component> usage = TextUtils.wrapString(strUsage, style, 200);

		TNT_MINE_ITEM = new ItemStack(Material.TNT, TNT_MINE_COUNT);
		ItemMeta meta = TNT_MINE_ITEM.getItemMeta();

		meta.displayName(ItemUtils.noItalics(Component.text("TNT Mine", TNT_COLOR)));

		List<Component> lore = new ArrayList<>();
		lore.addAll(TextUtils.wrapString("A TNT landmine trap that blows enemies to smithereens",
				Style.style(TNT_COLOR).decoration(TextDecoration.ITALIC, false), 200));
		lore.addAll(usage);
		meta.lore(lore);
		TNT_MINE_ITEM.setItemMeta(meta);

		PUSH_MINE_ITEM = new ItemStack(Material.WHITE_WOOL, PUSH_MINE_COUNT);
		meta = PUSH_MINE_ITEM.getItemMeta();
		meta.displayName(ItemUtils.noItalics(Component.text("Push Mine")));
		lore = new ArrayList<>();
		lore.addAll(TextUtils.wrapString("A trap that creates an explosive gust of air, pushing away all enemies near it"
				, Style.empty().decoration(TextDecoration.ITALIC, false), 200));
		lore.addAll(usage);
		meta.lore(lore);
		PUSH_MINE_ITEM.setItemMeta(meta);

		REMOTE_DETONATOR_ITEM = new ItemStack(Material.FLINT_AND_STEEL);
		meta = REMOTE_DETONATOR_ITEM.getItemMeta();
		meta.displayName(ItemUtils.noItalics(Component.text("Remote Trigger", NamedTextColor.BLUE)));
		lore = new ArrayList<>(TextUtils.wrapString("Point at any of your mines from any distance to select one. Once selected, it will turn blue. Right click and boom!", style, 200));
		meta.lore(lore);
		REMOTE_DETONATOR_ITEM.setItemMeta(meta);
	}

	private static void setValidMineBlock(Material mat) {
		VALID_MINE_BLOCKS[mat.ordinal()] = true;
	}

	public KitDemolitions() {
		super("Demolitions", "This kit comes with traps! Specifically, those that explode when people step on them. " +
				"They also blend in with the ground no matter the color!\n\n" +
				"It comes with a TNT trap that does lots of damage " +
				"and a Push Mine trap that sends enemies flying!\n\n" +
				"After placing them down, enemies can step on them to trigger them or they can be triggered remotely " +
				"by the kit user.",
				Material.STONE_PRESSURE_PLATE);

		ItemStack sword = new ItemStack(Material.IRON_SWORD);
		sword.addEnchantment(Enchantment.FIRE_ASPECT, 1);

		//Mine item stack sizes set in giveAbility()
		setItems(sword, TNT_MINE_ITEM, PUSH_MINE_ITEM, REMOTE_DETONATOR_ITEM);

		this.setAbilities(new DemolitionsAbility());

		this.setArmor(new ItemStack(Material.CHAINMAIL_HELMET), new ItemStack(Material.CHAINMAIL_CHESTPLATE),
				new ItemStack(Material.CHAINMAIL_LEGGINGS), new ItemStack(Material.GOLDEN_BOOTS));

		setCategory(KitCategory.SUPPORT);

		setFuseEnchantLevel(5);
	}

	public static boolean isValidMineBlock(Block block) {
		return VALID_MINE_BLOCKS[block.getType().ordinal()];
	}

	public record RegeneratingMine(MineType type, int removedTime) {}

	public static class DemolitionsAbility extends Ability
	{
		public static final Map<Player, List<DemoMine>> PLAYER_MINES = new HashMap<>();
		public static final Map<Player, List<RegeneratingMine>> REGENERATING_MINES = new LinkedHashMap<>();
		public static final Map<Player, DemoMine> TARGETTED_MINE = new HashMap<>();

		public static final Map<PacketMineHitbox, DemoMine> AXOLOTL_TO_DEMO_MINE = new LinkedHashMap<>();
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
			TARGETTED_MINE.clear();
			MINE_POSITIONS.clear();

			DemoMine.clearTeams();
		}

		@Override
		public void giveAbility(Player player) {
			/*PlayerInventory inventory = player.getInventory();

			ItemStack playersTNT = ItemUtils.getItemInInventory(TNT_MINE_ITEM, player.getInventory());
			ItemStack playersPush = ItemUtils.getItemInInventory(PUSH_MINE_ITEM, player.getInventory());
			//should not be null
			playersTNT.setAmount(TNT_MINE_COUNT);
			playersPush.setAmount(PUSH_MINE_COUNT);*/
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
					for(Player viewer : mine.team.getPlayerMembers()) {
						Main.getPlayerInfo(viewer).getMetadataViewer().removeViewedValues(mine.stands);
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

			for(Player viewer : mine.team.getPlayerMembers()) {
				Main.getPlayerInfo(viewer).getMetadataViewer().removeViewedValues(mine.stands);
			}

			AXOLOTL_TO_DEMO_MINE.remove(mine.hitboxEntity);

			MINE_POSITIONS.remove(mine.getBlockVector());

			mine.removeEntities();
		}

		@Override
		public void onInteract(PlayerInteractEvent event) {
			if(!event.getAction().isRightClick())
				return;

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
								TextColors.ERROR_RED);
						PlayerUtils.sendKitMessage(event.getPlayer(), message, message);
					}
				}
				else {
					final Component message = Component.text("You can't place a Mine here", TextColors.ERROR_RED);
					PlayerUtils.sendKitMessage(event.getPlayer(), message, message);
					event.getPlayer().updateInventory(); // Refresh the 'placed' block
				}
			}
			else if(mat == REMOTE_DETONATOR_ITEM.getType()) {
				Player demo = event.getPlayer();
				event.setUseItemInHand(Event.Result.DENY);
				event.setUseInteractedBlock(Event.Result.DENY); //prevent arming tnt
				DemoMine mine = TARGETTED_MINE.get(demo);
				if(mine != null) {
					mine.trigger(demo);
					TARGETTED_MINE.remove(demo);
				}
			}
		}

		@Override
		public void onAttemptedAttack(DamageEvent event) {
			if(event.getDamageType().is(DamageType.DEMO_TNTMINE) && event.getAttacker() instanceof TNTPrimed dTnt) {
				Player demo = (Player) event.getFinalAttacker();
				if(event.getVictim() instanceof Player victim) {
					TNTMine mine = TNTMine.getByTNT(demo, dTnt);
					if (mine != null) {
						if (mine.triggerer == demo) {
							event.setDamageType(DEMO_TNTMINE_REMOTE);
						}
						else if (mine.triggerer == victim) {
							event.setDamageType(DamageType.DEMO_TNTMINE);
						}
						else {
							event.setDamageType(DEMO_TNTMINE_BYSTANDER);
							event.setDamageTypeCause(mine.triggerer);
						}
						event.setFinalDamage(event.getFinalDamage() * 0.75);
					}
				}
			}
			// If the victim is the demo, then allow it for the sake of push mine-jumping.
			// However, don't allow it if the demo is carrying the flag in CTF
			else if(event.getDamageType().is(DamageType.DEMO_PUSHMINE) && event.getFinalAttacker() == event.getVictim()) {
				event.setCancelled(false);
				if(Main.getGame() instanceof CaptureTheFlag ctf && ctf.isFlagCarrier(event.getPlayerVictim())) {
					event.setCancelled(true);
					// Send them a message if they're probably mine jumping
					if(event.hasKnockback() && event.getKnockback().length() >= 1d) {
						event.getPlayerVictim().sendMessage(Component.text(
								"The flag is too heavy for you to mine-jump with!", TextColors.ERROR_RED));
					}
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
			final int gameTick = TeamArena.getGameTick();

			{
				//add mines to be removed to this list and remove afterwards to prevent concurrent modification
				List<DemoMine> toRemove = new LinkedList<>();
				for (Map.Entry<PacketMineHitbox, DemoMine> entry : AXOLOTL_TO_DEMO_MINE.entrySet()) {
					DemoMine mine = entry.getValue();

					mine.tick();

					if (mine.removeNextTick) {
						toRemove.add(mine);
						addRegeneratingMine(mine.owner, mine.type, gameTick);
					}
					//determine if needs to be removed (next tick)
					else if (mine.isDone()) {
						mine.removeNextTick = true;
					}
					//if it hasn't been armed yet
					else if (!mine.isArmed()) {
						//indicate its armed
						if (gameTick == mine.creationTime + DemoMine.TIME_TO_ARM) {
							World world = mine.hitboxEntity.getWorld();
							world.playSound(mine.hitboxEntity.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_OFF, 1f, 1f);
							world.spawnParticle(Particle.CRIT, mine.hitboxEntity.getLocation()
									.add(0, 0.4, 0), 2, 0, 0, 0, 0);

							Component message = Component.text("Your " + mine.type.name + " is now armed")
									.color(NamedTextColor.GREEN);
							PlayerUtils.sendKitMessage(mine.owner, message, message);
						}
						// else do nothing and don't enter the control statement below that checks for collision
					}
					//if it hasn't been stepped on already check if anyone's standing on it
					else if (!mine.isTriggered()) {
						for (Player stepper : Main.getGame().getPlayers()) {
							if (mine.team.getPlayerMembers().contains(stepper))
								continue;

							PacketMineHitbox axolotl = entry.getKey();
							if (stepper.getBoundingBox().overlaps(axolotl.getBoundingBox())) {
								//they stepped on mine, trigger explosion
								mine.trigger(stepper);
							}
						}
					}
				}

				for (DemoMine remove : toRemove) {
					removeMine(remove);
				}
			}

			//tick regenerating mines
			{
				var regPlayersIter = REGENERATING_MINES.entrySet().iterator();
				while (regPlayersIter.hasNext()) {
					var entry = regPlayersIter.next();

					var regMinesIter = entry.getValue().iterator();
					while (regMinesIter.hasNext()) {
						RegeneratingMine regMine = regMinesIter.next();

						//check if time up and give back one mine here
						if (gameTick - regMine.removedTime() >= regMine.type().timeToRegen) {
							Player owner = entry.getKey();
							ItemStack mineItem;
							if (regMine.type() == MineType.TNTMINE) {
								mineItem = TNT_MINE_ITEM;
							}
							else {
								mineItem = PUSH_MINE_ITEM;
							}

							owner.getInventory().addItem(mineItem.asOne());

							regMinesIter.remove();
						}
					}

					if (entry.getValue().size() == 0)
						regPlayersIter.remove();
				}
			}
		}

		/**
		 * Add the appropriate metadata for glowing mines for players leaving and joining the team.
		 */
		public static void teamSwitch(Player player, TeamArenaTeam oldTeam, TeamArenaTeam newTeam) {
			//iterate through all mines.
			// if it's old team's mines, make it un-glow.
			// if new team's mines, make it glow
			MetadataViewer metadataViewer = Main.getPlayerInfo(player).getMetadataViewer();
			for(Map.Entry<PacketMineHitbox, DemoMine> entry : AXOLOTL_TO_DEMO_MINE.entrySet()) {
				DemoMine mine = entry.getValue();

				if(mine.team == oldTeam) {
					metadataViewer.removeViewedValues(mine.stands);
				}
				else if(mine.team == newTeam) {
					metadataViewer.setViewedValues(MetaIndex.BASE_BITFIELD_IDX, MetaIndex.GLOWING_METADATA_VALUE,
							mine.stands);
				}

				metadataViewer.refreshViewer(mine.stands);
			}
		}

		public void addRegeneratingMine(Player player, MineType type, int startTime) {
			List<RegeneratingMine> regenningMines = REGENERATING_MINES.computeIfAbsent(player,
					player1 -> new ArrayList<>(TNT_MINE_COUNT + PUSH_MINE_COUNT));

			regenningMines.add(new RegeneratingMine(type, startTime));

			final Component message = Component.text("Your " + type.name + " exploded or was destroyed. You'll get it back in "
							+ (type.timeToRegen / 20) + " seconds",
					NamedTextColor.AQUA);

			PlayerUtils.sendKitMessage(player, message, message);
		}

		public static void handleHitboxPunch(PacketMineHitbox hitbox, Player puncher) {
			DemoMine mine = AXOLOTL_TO_DEMO_MINE.get(hitbox);

			//teammate punches it
			if (puncher != mine.owner && mine.team.getPlayerMembers().contains(puncher)) {
				puncher.sendMessage(Component.text("This is ", NamedTextColor.AQUA).append(
						mine.owner.playerListName()).append(Component.text("'s " + mine.type.name)));
			}
			else {
				final int currentTick = TeamArena.getGameTick();
				int diff = currentTick - hitbox.lastHurtTime;
				if(diff >= 10) {
					hitbox.lastHurtTime = currentTick;
					if(mine.hurt()) {
						Component message;
						if(puncher != mine.owner) {
							message = Component.text("You've broken one of ", NamedTextColor.AQUA).append(
									mine.owner.playerListName()).append(Component.text("'s " + mine.type.name + "s!",
									NamedTextColor.AQUA));

							Component ownerMessage = Component.text("Someone broke one of your " + mine.type.name + "s!",
									NamedTextColor.AQUA);

							PlayerUtils.sendKitMessage(mine.owner, ownerMessage, ownerMessage);
						}
						else {
							message = Component.text("Broke your " + mine.type.name).color(NamedTextColor.AQUA);
						}
						puncher.sendMessage(message);
					}
				}
			}
		}
	}
}

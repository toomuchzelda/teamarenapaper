package me.toomuchzelda.teamarenapaper.teamarena.kits.kitbeekeeper;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.MobGoals;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_19_R2.entity.CraftBee;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Bee;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Kit Beekeeper class.
 * <br>
 * The beekeeper has 3 bees that it can command.
 * <br>
 * Initially the bees surround & follow the user. They can be commanded to do 3 things:
 * <br>
 * Stay and defend a point, pursue and attack an enemy, or give health to a teammate (via a drinkable honey bottle)
 * <br>
 * They can be commanded to regroup with the user by clicking and individual bee or using the regroup item.
 *
 * @author toomuchzelda
 */
public class KitBeekeeper extends Kit
{
	private static final ItemStack BEE_WAND;
	private static final ItemStack REGROUP_ITEM;

	static {
		List<Component> lore = new ArrayList<>(5);
		TextColor color = TextColor.color(255, 210, 81);
		lore.add(Component.text("Use this to command one bee at a time", color));
		lore.add(Component.text("Right click a block to ", TextUtils.RIGHT_CLICK_TO).append(Component.text("defend", color)));
		lore.add(Component.text("Right click a teammate to ", TextUtils.RIGHT_CLICK_TO).append(Component.text("give honey", NamedTextColor.LIGHT_PURPLE)));
		lore.add(Component.text("Attack an enemy to ", TextUtils.LEFT_CLICK_TO).append(Component.text("pursue", NamedTextColor.RED)));
		lore.add(Component.text("Click any bee to return to following you"));

		BEE_WAND = ItemBuilder.of(Material.HONEYCOMB)
			.displayName(Component.text("Bee Commander", NamedTextColor.YELLOW))
			.lore(lore)
			.build();

		REGROUP_ITEM = ItemBuilder.of(Material.BEEHIVE)
			.displayName(Component.text("Return to hive (to yourself)", color))
			.lore(
				List.of(Component.text("Click to regroup all bees", TextUtils.RIGHT_CLICK_TO))
			)
			.build();
	}

	public KitBeekeeper() {
		super("Beekeeper", "Honey is sweet, but using bees to your military advantage is sweeter.", Material.BEE_NEST);

		ItemStack helmet = ItemUtils.colourLeatherArmor(Color.BLACK, new ItemStack(Material.LEATHER_HELMET));
		ItemStack boots = ItemUtils.colourLeatherArmor(Color.BLACK, new ItemStack(Material.LEATHER_BOOTS));
		helmet.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 1);
		boots.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 1);

		this.setArmor(helmet, new ItemStack(Material.GOLDEN_CHESTPLATE), new ItemStack(Material.GOLDEN_LEGGINGS), boots);
		// TODO find the the name of that honey thing
		ItemStack sword = ItemBuilder.of(Material.WOODEN_AXE).displayName(Component.text("Honey thing")).build();
		this.setItems(sword, BEE_WAND, REGROUP_ITEM);

		this.setAbilities(new BeekeeperAbility());
	}

	public static class BeekeeperAbility extends Ability {

		static final int MAX_BEES = 3;
		private static final float BEE_SPEED = 0.04f;
		private static final double ABSORPTION_AMOUNT = 2d;
		private static final double HEAL_AMOUNT = 2d;

		private static class BeekeeperBee {
			private Bee beeEntity;
			// instance of the task they're carrying out. Contains all the information needed for the task too.
			private BeeTask task;
			private final int beeNum; // index of this BeekeeperBee in the array it's in

			BeekeeperBee(Location loc, int beeNum) {
				this.beeNum = beeNum;
				// TODO metadataviewer
			}

			void spawnBee(Location loc) {
				if (this.beeEntity != null) {
					this.beeEntity.remove();
				}

				beeEntity = loc.getWorld().spawn(loc, Bee.class);
				((CraftBee) beeEntity).getHandle().flyingSpeed = BEE_SPEED; // TODO test bee speed
			}

			void setTask(BeeTask task) {
				// Remove previous BeeTask goals
				if (this.task != null) {
					for (Goal<Bee> previousGoal : this.task.getMobGoals()) {
						Bukkit.getMobGoals().removeGoal(this.beeEntity, previousGoal);
					}
				}
				// Add new
				for (Goal<Bee> goal : task.getMobGoals()) {
					Bukkit.getMobGoals().addGoal(beeEntity, 1, goal);
				}
				this.task = task;
			}

			private boolean isFollowingOwner() {
				return this.task instanceof FollowOwnerTask;
			}
		}

		private static class BeekeeperInfo {
			BeekeeperBee[] bees;
			final int spawnTime;
			/**
			 * Band-aid. When player interacts with bee, both PlayerInteractEntityEvent and PlayerInteractEvent are
			 * fired. EntityEvent is called first, which means when you click a bee to make it follow, the InteractEvent
			 * is called immediately after for the block behind it, and it begins defending that block again.
			 * So just check the last time any was clicked here and don't fire twice in one tick.
			 */
			private int lastInteractTime;

			BeekeeperInfo(BeekeeperBee[] bees) {
				this.bees = bees;
				final int currentTick = TeamArena.getGameTick();
				this.spawnTime = currentTick;
				this.lastInteractTime = currentTick;
			}

			private BeekeeperBee getNextAvailableBee() {
				for (BeekeeperBee beekeeperBee : this.bees) {
					if (beekeeperBee.isFollowingOwner()) {
						return beekeeperBee;
					}
				}

				return null;
			}
		}

		private record BeePlayerPair(Player owner, BeekeeperBee beekeeperBee) {}

		private final Map<Player, BeekeeperInfo> BEEKEEPERS = new LinkedHashMap<>();
		private final Map<Bee, BeePlayerPair> BEE_LOOKUP = new WeakHashMap<>();

		@Override
		public void giveAbility(Player player) {
			BeekeeperBee[] bees = new BeekeeperBee[MAX_BEES];
			final int currentTick = TeamArena.getGameTick();
			MobGoals manager = Bukkit.getMobGoals();
			for (int i = 0; i < bees.length; i++) {
				bees[i] = new BeekeeperBee(FollowOwnerTask.calculateBeeLocation(player, i, currentTick), i);

				manager.removeAllGoals(bees[i].beeEntity);
				// Need to use NMS to add vanilla goals
				net.minecraft.world.entity.animal.Bee nmsBee = ((CraftBee) bees[i].beeEntity).getHandle();
				nmsBee.goalSelector.addGoal(9, new FloatGoal(nmsBee));
				// Should only activate if the bee has a target via setTarget(LivingEntity)
				nmsBee.goalSelector.addGoal(8, new MeleeAttackGoal(nmsBee, 1.399999976158142d, true));

				bees[i].setTask(new FollowOwnerTask(bees[i].beeEntity, currentTick, i, player));

				BEE_LOOKUP.put(bees[i].beeEntity, new BeePlayerPair(player, bees[i]));
			}

			BeekeeperInfo info = new BeekeeperInfo(bees);
			BEEKEEPERS.put(player, info);
		}

		@Override
		public void removeAbility(Player player) {
			BeekeeperInfo beekeeperInfo = BEEKEEPERS.remove(player);
			if (beekeeperInfo == null) {
				Main.logger().severe("null BeekeeperInfo for player " + player.getName());
				Thread.dumpStack();
				return;
			}

			for (BeekeeperBee bee : beekeeperInfo.bees) {
				Location loc = bee.beeEntity.getLocation().add(0, bee.beeEntity.getHeight() / 2, 0);
				loc.getWorld().spawnParticle(Particle.CLOUD, loc, 1, 0.2d, 0.2d, 0.2d, 0.02d);
				loc.getWorld().playSound(loc, Sound.ENTITY_BEE_DEATH, 0.85f, 1f);
				bee.beeEntity.remove();

				BEE_LOOKUP.remove(bee.beeEntity);
			}
		}

		@Override
		public void onTick() {
			// Reset completed AI Goals to FollowOwner
			for (var entry : BEEKEEPERS.entrySet()) {
				for (BeekeeperBee beekeeperBee : entry.getValue().bees) {
					if (beekeeperBee.task.isDone()) {
						beekeeperBee.setTask(new FollowOwnerTask(beekeeperBee.beeEntity, TeamArena.getGameTick(),
							beekeeperBee.beeNum, entry.getKey()));
					}
				}
			}
		}

		/**
		 * Brain method to handle all interactions with the BEE_WAND.
		 * No differentiation between left and right clicks with this item.
		 * @param clickedBlock null if a block wasn't clicked
		 * @param clickedBlockFace null if a block wasn't clicked
		 * @param clickedEntity null if a LivingEntity wasn't clicked
		 */
		private void handleInteraction(Player beekeeper, Block clickedBlock, BlockFace clickedBlockFace,
									   LivingEntity clickedEntity) {

			if (clickedEntity != null) {
				if (clickedEntity instanceof Player clickedPlayer && Main.getPlayerInfo(clickedPlayer).team.hasMember(beekeeper)) {
					// Clicked teammate: give honey
					BeekeeperBee freeBee = BEEKEEPERS.get(beekeeper).getNextAvailableBee();
					if (freeBee != null) {
						freeBee.setTask(new DeliverHoneyTask(freeBee.beeEntity, clickedPlayer));
					}
				}
				else {
					BeePlayerPair pair = null;
					if (clickedEntity instanceof Bee clickedBee)
						pair = BEE_LOOKUP.get(clickedBee);

					if (pair != null && pair.owner() == beekeeper) { // Clicked a bee they own
						if (!pair.beekeeperBee.isFollowingOwner()) {
							Bee clickedBee = (Bee) clickedEntity;
							pair.beekeeperBee.setTask(new FollowOwnerTask(clickedBee, TeamArena.getGameTick() - clickedBee.getTicksLived(), pair.beekeeperBee.beeNum, beekeeper));

							beekeeper.sendMessage("following u");
						}
					}
					else {
						// Clicked enemy or other entity: target them
						BeekeeperBee freeBee = BEEKEEPERS.get(beekeeper).getNextAvailableBee();
						if (freeBee != null) {
							freeBee.setTask(new PursueEnemyTask(freeBee.beeEntity, clickedEntity));
						}
					}
				}
			}
			else if (clickedBlock != null) {
				BeekeeperBee bee = BEEKEEPERS.get(beekeeper).getNextAvailableBee();
				if (bee != null) {
					// The point to defend will be the centre of the block + the vector of the clicked block face.
					// So actually the block connected to the face of the clickedBlock
					Location locToDefend = clickedBlock.getLocation().add(0.5d, 0.5d, 0.5d).add(clickedBlockFace.getDirection());
					bee.setTask(new DefendPointTask(bee.beeEntity, beekeeper, locToDefend));
				}
			}
		}

		@Override
		public void onInteract(PlayerInteractEvent event) {
			if (event.getItem() != null && event.getItem().isSimilar(BEE_WAND)) {
				BeekeeperInfo binfo = BEEKEEPERS.get(event.getPlayer());
				final int currentTick = TeamArena.getGameTick();

				if (binfo.lastInteractTime != currentTick) {
					if (event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
						binfo.lastInteractTime = currentTick;
						handleInteraction(event.getPlayer(), event.getClickedBlock(), event.getBlockFace(), null);
					}
				}
			}
		}

		@Override
		public void onInteractEntity(PlayerInteractEntityEvent event) {
			ItemStack usedItem = event.getPlayer().getEquipment().getItem(event.getHand());
			BeekeeperInfo binfo = BEEKEEPERS.get(event.getPlayer());
			final int currentTick = TeamArena.getGameTick();

			if (binfo.lastInteractTime != currentTick) {
				if (usedItem != null && usedItem.isSimilar(BEE_WAND) && event.getRightClicked() instanceof LivingEntity livingEntity) {
					binfo.lastInteractTime = currentTick;
					handleInteraction(event.getPlayer(), null, null, livingEntity);
				}
			}
		}

		static void beeHeal(Bee bee, LivingEntity target) {
			if (target instanceof Player playerTarget) {
				PlayerUtils.heal(playerTarget, HEAL_AMOUNT, EntityRegainHealthEvent.RegainReason.CUSTOM);
			}
			else {
				double newHealth = target.getHealth() + HEAL_AMOUNT;
				newHealth = Math.min(newHealth, target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
				target.setHealth(newHealth);
			}

			target.setAbsorptionAmount(target.getAbsorptionAmount() + ABSORPTION_AMOUNT);

			// Play visual and sound effects
			bee.getWorld().spawnParticle(Particle.FALLING_HONEY, target.getLocation(), 3);
			bee.getWorld().playSound(bee, Sound.ENTITY_BEE_POLLINATE, 1f, 1.1f);
		}
	}
}

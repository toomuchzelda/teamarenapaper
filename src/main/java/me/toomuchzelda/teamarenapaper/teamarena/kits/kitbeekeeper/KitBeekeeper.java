package me.toomuchzelda.teamarenapaper.teamarena.kits.kitbeekeeper;

import com.comphenix.protocol.wrappers.AdventureComponentConverter;
import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.MobGoals;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.metadata.MetaIndex;
import me.toomuchzelda.teamarenapaper.metadata.MetadataViewer;
import me.toomuchzelda.teamarenapaper.metadata.SimpleMetadataValue;
import me.toomuchzelda.teamarenapaper.scoreboard.PlayerScoreboard;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import me.toomuchzelda.teamarenapaper.utils.TextColors;
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
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Team;

import java.util.*;

/**
 * Kit Beekeeper class.
 * <br>
 * The beekeeper has 3 bees that it can command.
 * <br>
 * Initially the bees surround & follow the user. They can be commanded to do 3 things:
 * <br>
 * Stay and defend a point, pursue and attack an enemy, or give health to a teammate
 * <br>
 * They can be commanded to regroup with the user by clicking and individual bee or using the regroup item.
 *
 * @author toomuchzelda
 */
public class KitBeekeeper extends Kit
{
	private static final ItemStack BEE_WAND;
	private static final ItemStack REGROUP_ITEM;

	private static final TextColor BEE_YELLOW = TextColor.color(255, 210, 81);

	static {
		List<Component> lore = new ArrayList<>(5);
		lore.add(Component.text("Use this to command one bee at a time", BEE_YELLOW));
		lore.add(Component.text("Right click a block to ", TextUtils.RIGHT_CLICK_TO).append(Component.text("defend", BEE_YELLOW)));
		lore.add(Component.text("Right click a teammate to ", TextUtils.RIGHT_CLICK_TO).append(Component.text("give honey", NamedTextColor.LIGHT_PURPLE)));
		lore.add(Component.text("Right click an enemy to ", TextUtils.RIGHT_CLICK_TO).append(Component.text("pursue", NamedTextColor.RED)));
		lore.add(Component.text("Click any bee to return to following you"));

		BEE_WAND = ItemBuilder.of(Material.HONEYCOMB)
			.displayName(Component.text("Bee Commander", NamedTextColor.YELLOW))
			.lore(lore)
			.build();

		REGROUP_ITEM = ItemBuilder.of(Material.BEEHIVE)
			.displayName(Component.text("Return to hive (to yourself)", BEE_YELLOW))
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
		ItemStack sword = ItemBuilder.of(Material.WOODEN_SWORD).displayName(Component.text("Honey dipper")).build();
		this.setItems(sword, BEE_WAND, REGROUP_ITEM);

		this.setAbilities(new BeekeeperAbility());
	}

	public static class BeekeeperAbility extends Ability {

		static final int MAX_BEES = 3;
		static final float BEE_SPEED = 0.12f;
		static final float BEE_VANILLA_SPEED = 0.02f;

		private static final double ABSORPTION_AMOUNT = 2d;
		private static final double HEAL_AMOUNT = 1d;
		private static final double BEE_ATTACK = 4.5d; // Bee raw damage power for PursueOwnerGoal
		private static final double BEE_HEALTH = 14d; // 7 hearts
		private static final int RESPAWN_TIME = 10 * 20;

		private static final List<Team> GLOWING_COLOUR_TEAMS = new ArrayList<>(MAX_BEES);
		private static final List<BeeName> BEE_NAMES = List.of(
			BeeName.create("Bee-lue", NamedTextColor.BLUE),
			BeeName.create("Greenbee", NamedTextColor.GREEN),
			BeeName.create("Richard", NamedTextColor.RED)
		);

		private static final Component ACTIONBAR_DEAD = Component.text("Dead", TextColors.ERROR_RED);

		private record BeeName(Component displayName, NamedTextColor color, Team team) {
			public static BeeName create(String name, NamedTextColor color) {
				Team team = PlayerScoreboard.SCOREBOARD.registerNewTeam("bkpr" + color.value());
				team.color(color);

				PlayerScoreboard.addGlobalTeam(team);
				GLOWING_COLOUR_TEAMS.add(team);

				return new BeeName(Component.text(name, color), color, team);
			}
		}

		private static class BeekeeperBee {
			private final Player owner;
			private final BeeName name;
			private Bee beeEntity;
			// instance of the task they're carrying out. Contains all the information needed for the task too.
			private BeeTask task;
			private final int beeNum; // index of this BeekeeperBee in the array it's in
			private int deathTime = -RESPAWN_TIME;

			BeekeeperBee(Location loc, int beeNum, BeeName nameAndColour, Player owner) {
				this.owner = owner;
				this.beeNum = beeNum;
				this.name = nameAndColour;
				this.spawnBee(loc);
			}

			void spawnBee(Location loc) {
				if (this.beeEntity != null) {
					this.beeEntity.remove();
				}

				beeEntity = loc.getWorld().spawn(loc, Bee.class);
				beeEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(BEE_HEALTH);
				((CraftBee) beeEntity).getHandle().flyingSpeed = BEE_SPEED;

				MobGoals manager = Bukkit.getMobGoals();
				manager.removeAllGoals(beeEntity);
				// Need to use NMS to add vanilla goals
				net.minecraft.world.entity.animal.Bee nmsBee = ((CraftBee) beeEntity).getHandle();
				nmsBee.goalSelector.addGoal(9, new FloatGoal(nmsBee));
				// Should only activate if the bee has a target via setTarget(LivingEntity)
				nmsBee.goalSelector.addGoal(8, new MeleeAttackGoal(nmsBee, 1.399999976158142d, true));

				//this.setTask(new FollowOwnerTask(beeEntity, TeamArena.getGameTick(), this.beeNum, this.owner));
				this.setFollowing();

				BEE_LOOKUP.put(this.beeEntity, new BeePlayerPair(this.owner, this));

				final PlayerInfo pinfo = Main.getPlayerInfo(this.owner);
				// Put them on the team for the glowing colour, for the owner only
				pinfo.getScoreboard().addMembers(GLOWING_COLOUR_TEAMS.get(this.beeNum), beeEntity);

				// Only the beekeeper sees their bees' name and glowing
				Optional<?> nameComponent = Optional.of(AdventureComponentConverter.fromComponent(
					this.name.displayName()).getHandle());

				MetadataViewer viewer = pinfo.getMetadataViewer();
				viewer.setViewedValue(MetaIndex.BASE_BITFIELD_IDX, MetaIndex.GLOWING_METADATA_VALUE, beeEntity);
				viewer.setViewedValue(MetaIndex.CUSTOM_NAME_VISIBLE_IDX, new SimpleMetadataValue<>(Boolean.TRUE), beeEntity);
				viewer.setViewedValue(MetaIndex.CUSTOM_NAME_IDX, new SimpleMetadataValue<Object>(nameComponent), beeEntity);
				viewer.refreshViewer(beeEntity);
			}

			void tick() {
				if (this.beeEntity != null) {
					if (this.task.isDone()) {
						//this.setTask(new FollowOwnerTask(this.beeEntity, TeamArena.getGameTick(), this.beeNum, this.owner));
						this.setFollowing();
					}
				}
				else {
					final int currentTick = TeamArena.getGameTick();
					if (currentTick - this.deathTime >= RESPAWN_TIME) {
						this.spawnBee(FollowOwnerTask.calculateBeeLocation(this.owner, this.beeNum, currentTick));
					}
				}
			}

			void setTask(BeeTask newTask) {
				if (this.isDead()) return;

				// Remove previous BeeTask goals
				if (this.task != null) {
					for (Goal<Bee> previousGoal : this.task.getMobGoals()) {
						Bukkit.getMobGoals().removeGoal(this.beeEntity, previousGoal);
					}
				}
				// Add new
				for (Goal<Bee> goal : newTask.getMobGoals()) {
					Bukkit.getMobGoals().addGoal(beeEntity, 1, goal);
				}
				this.task = newTask;
			}

			private Component getTaskActionBar() {
				var builder = Component.text();
				builder.append(this.name.displayName());
				builder.append(Component.space());
				if (this.isDead()) {
					builder.append(ACTIONBAR_DEAD);
				}
				else {
					builder.append(this.task.getActionBarPart());
				}

				return builder.build();
			}

			/** Convenience method to set task to FollowOwnerTask */
			void setFollowing() {
				if (!this.isDead())
					this.setTask(new FollowOwnerTask(this.beeEntity, TeamArena.getGameTick() - this.beeEntity.getTicksLived(), this.beeNum, this.owner));
			}

			private boolean isFollowingOwner() {
				return this.task instanceof FollowOwnerTask;
			}

			private boolean isDead() {
				return TeamArena.getGameTick() - deathTime < RESPAWN_TIME;
			}

			private void setDead(boolean killEntity) {
				if (killEntity)
					this.beeEntity.remove();

				this.deathTime = TeamArena.getGameTick();
				this.task = null;

				PlayerInfo pinfo = Main.getPlayerInfo(this.owner);
				pinfo.getMetadataViewer().removeViewedValues(this.beeEntity);
				pinfo.getScoreboard().removeMembers(GLOWING_COLOUR_TEAMS.get(this.beeNum), this.beeEntity);

				BEE_LOOKUP.remove(this.beeEntity);

				this.beeEntity = null;
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

			private static final Component SEPARATOR = Component.text(" | ");
			private Component getActionBar() {
				var builder = Component.text();
				for (int i = 0; i < this.bees.length; i++) {
					builder.append(this.bees[i].getTaskActionBar());

					if (i != this.bees.length - 1) {
						builder.append(SEPARATOR);
					}
				}

				return builder.build();
			}
		}

		private record BeePlayerPair(Player owner, BeekeeperBee beekeeperBee) {}

		private final Map<Player, BeekeeperInfo> BEEKEEPERS = new LinkedHashMap<>();
		private static final Map<Bee, BeePlayerPair> BEE_LOOKUP = new HashMap<>();

		@Override
		public void giveAbility(Player player) {
			BeekeeperBee[] bees = new BeekeeperBee[MAX_BEES];
			final int currentTick = TeamArena.getGameTick();
			for (int i = 0; i < bees.length; i++) {
				bees[i] = new BeekeeperBee(FollowOwnerTask.calculateBeeLocation(player, i, currentTick), i,
					BEE_NAMES.get(i), player);
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
				if (!bee.isDead()) {
					Location loc = bee.beeEntity.getLocation().add(0, bee.beeEntity.getHeight() / 2, 0);
					loc.getWorld().spawnParticle(Particle.CLOUD, loc, 1, 0.2d, 0.2d, 0.2d, 0.02d);
					loc.getWorld().playSound(loc, Sound.ENTITY_BEE_DEATH, 0.8f, 1f);

					bee.setDead(true);
				}
			}
		}

		@Override
		public void unregisterAbility() {
			var iter = BEE_LOOKUP.entrySet().iterator();
			while (iter.hasNext()) {
				var entry = iter.next();
				PlayerScoreboard.removeMembersAll(GLOWING_COLOUR_TEAMS.get(entry.getValue().beekeeperBee().beeNum), entry.getKey());
				entry.getKey().remove();
				iter.remove();
			}
		}

		@Override
		public void onTick() {
			final boolean tickActionBar = TeamArena.getGameTick() % 2 == 1;
			for (var entry : BEEKEEPERS.entrySet()) {
				for (BeekeeperBee beekeeperBee : entry.getValue().bees) {
					beekeeperBee.tick();
				}

				if (tickActionBar)
					entry.getKey().sendActionBar(entry.getValue().getActionBar());
			}
		}

		/** Bee attacks another entity */
		public static void handleBeeAttemptAttack(DamageEvent event) {
			final Bee bee = (Bee) event.getFinalAttacker(); // instanceof Bee check done in TeamArena.java
			BeePlayerPair pair = BEE_LOOKUP.get(bee);
			if (pair != null) {
				event.setRealAttacker(bee);
				event.setFinalAttacker(pair.owner());
				event.setDamageType(DamageType.BEEKEEPER_BEE);

				if (!pair.beekeeperBee().isFollowingOwner())
					event.setRawDamage(BEE_ATTACK);
			}
		}

		/** Damage is attempted on the Bee */
		public static void handleBeeAttemptDamage(DamageEvent event) {
			final Bee bee = (Bee) event.getVictim(); // instanceof Bee check done in TeamArena.java
			BeePlayerPair pair = BEE_LOOKUP.get(bee);
			if (pair != null) { // Is a beekeeper bee
				final Entity finalAttacker = event.getFinalAttacker();
				if (finalAttacker instanceof Player playerAttacker &&
					!Main.getGame().canAttack(pair.owner(), playerAttacker)) {

					event.setCancelled(true);
				}
			}
		}

		/** Bee takes damage */
		public static void handleBeeConfirmedDamage(DamageEvent event) {
			final Bee bee = (Bee) event.getVictim();
			BeePlayerPair pair = BEE_LOOKUP.get(bee);
			if (pair != null && event.getFinalDamage() >= bee.getHealth()) { // Is a beekeeper bee and this DamageEvent kills them
				event.setBroadcastDeathMessage(false);
				pair.beekeeperBee().setDead(false);
			}
		}

		private void handleRegroupItemUse(Player beekeeper, Bee clickedBee) {
			if (clickedBee != null) {
				// TODO: handle from-afar clicked bees

				BeePlayerPair pair = BEE_LOOKUP.get(clickedBee);
				if (pair != null) {
					pair.beekeeperBee().setFollowing();
				}
			}
			else {
				for (BeekeeperBee beekeeperBee : BEEKEEPERS.get(beekeeper).bees) {
					beekeeperBee.setFollowing();
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
		private void handleBeeCommanderUse(Player beekeeper, Block clickedBlock, BlockFace clickedBlockFace,
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
							pair.beekeeperBee().setFollowing();
							//beekeeper.sendMessage("following u");
						}
					}
					else {
						// Clicked enemy or other entity: target them
						BeekeeperBee freeBee = BEEKEEPERS.get(beekeeper).getNextAvailableBee();
						if (freeBee != null) {
							freeBee.setTask(PursueEnemyTask.newInstance(freeBee.beeEntity, clickedEntity));
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
			final BeekeeperInfo binfo = BEEKEEPERS.get(event.getPlayer());
			final int currentTick = TeamArena.getGameTick();

			if (binfo.lastInteractTime != currentTick) {
				if (BEE_WAND.isSimilar(event.getItem())) {
					if (event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
						binfo.lastInteractTime = currentTick;
						handleBeeCommanderUse(event.getPlayer(), event.getClickedBlock(), event.getBlockFace(), null);
					}
				}
				else if (REGROUP_ITEM.isSimilar(event.getItem())) {
					binfo.lastInteractTime = currentTick;
					handleRegroupItemUse(event.getPlayer(), null);
				}
			}
		}

		@Override
		public void onInteractEntity(PlayerInteractEntityEvent event) {
			ItemStack usedItem = event.getPlayer().getEquipment().getItem(event.getHand());
			BeekeeperInfo binfo = BEEKEEPERS.get(event.getPlayer());
			final int currentTick = TeamArena.getGameTick();

			if (binfo.lastInteractTime != currentTick) {
				if (BEE_WAND.isSimilar(usedItem)) {
					if (event.getRightClicked() instanceof LivingEntity livingEntity) {
						binfo.lastInteractTime = currentTick;
						handleBeeCommanderUse(event.getPlayer(), null, null, livingEntity);
					}
				}
				else if (REGROUP_ITEM.isSimilar(usedItem)) {
					if (event.getRightClicked() instanceof Bee bee) {
						binfo.lastInteractTime = currentTick;
						handleRegroupItemUse(event.getPlayer(), bee);
					}
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

			// Don't heal beyond 2 absorption hearts
			final double targetAbsorp = target.getAbsorptionAmount();
			if (targetAbsorp < 4d) {
				target.setAbsorptionAmount(Math.min(4d, targetAbsorp + ABSORPTION_AMOUNT));
			}

			// Play visual and sound effects
			bee.getWorld().spawnParticle(Particle.FALLING_HONEY, target.getLocation(), 3);
			bee.getWorld().playSound(bee, Sound.ENTITY_BEE_POLLINATE, 1.1f, 1.1f);
		}
	}
}

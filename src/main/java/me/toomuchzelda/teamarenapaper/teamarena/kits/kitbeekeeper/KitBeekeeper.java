package me.toomuchzelda.teamarenapaper.teamarena.kits.kitbeekeeper;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

	static {
		List<Component> lore = new ArrayList<>(5);
		TextColor color = TextColor.color(255, 210, 81);
		lore.add(Component.text("Use this to command one bee at a time", color));
		lore.add(Component.text("Right click a block to ", TextUtils.RIGHT_CLICK_TO).append(Component.text("defend", color)));
		lore.add(Component.text("Right click a teammate to ", TextUtils.RIGHT_CLICK_TO).append(Component.text(" give honey", NamedTextColor.LIGHT_PURPLE)));
		lore.add(Component.text("Attack an enemy to ", TextUtils.LEFT_CLICK_TO).append(Component.text(" pursue", NamedTextColor.RED)));
		lore.add(Component.text("Click any bee to return to following you"));

		BEE_WAND = ItemBuilder.of(Material.HONEYCOMB)
			.displayName(Component.text("Bee Commander", NamedTextColor.YELLOW))
			.lore(lore)
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
		this.setItems(sword, BEE_WAND);

		this.setAbilities(new BeekeeperAbility());
	}

	public static class BeekeeperAbility extends Ability {

		private static final int MAX_BEES = 3;

		private static class BeekeeperBee {
			Bee beeEntity;
			BeeTask task; // instance of the task they're carrying out. Contains all the information needed for the task too.

			BeekeeperBee(Location loc, Player owner) {
				beeEntity = loc.getWorld().spawn(loc, Bee.class);
				this.task = new BeeTask.FollowOwner(beeEntity, owner);
			}
		}

		private static class BeekeeperInfo {
			BeekeeperBee[] bees;
			final int spawnTime;

			BeekeeperInfo(BeekeeperBee[] bees) {
				this.bees = bees;
				this.spawnTime = TeamArena.getGameTick();
			}
		}

		private final Map<Player, BeekeeperInfo> BEEKEEPERS = new LinkedHashMap<>();

		@Override
		public void giveAbility(Player player) {
			BeekeeperBee[] bees = new BeekeeperBee[MAX_BEES];
			final int currentTick = TeamArena.getGameTick();
			for (int i = 0; i < bees.length; i++) {
				bees[i] = new BeekeeperBee(calculateBeeLocation(player, i, currentTick), player);
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
				Location loc = bee.beeEntity.getLocation();
				loc.getWorld().spawnParticle(Particle.CLOUD, loc, 1, 0.2d, 0.2d, 0.2d, 0.02d);
				loc.getWorld().playSound(loc, Sound.ENTITY_BEE_DEATH, 0.85f, 1f);
				bee.beeEntity.remove();
			}
		}

		// TODO handle item interactions here
		@Override
		public void onInteract(PlayerInteractEvent event) {}
		@Override
		public void onInteractEntity(PlayerInteractEntityEvent event) {}

		/**
		 * Method to calculate position of bee for initial spawn and during FOLLOW_OWNER task
		 * @param beeOwner Boundingbox to surround
		 * @param beeNum index of bee out of all the bees (0, 1, 2 etc)
		 * @return Position as Location
		 */
		private static Location calculateBeeLocation(Entity beeOwner, int beeNum, int spawnTime) {
			//yaw : -180 to 180
			float offset = (float) (TeamArena.getGameTick() - spawnTime);
			offset %= 360f;
			offset -= 180f; // get it between -180 and 180

			Location loc = beeOwner.getLocation().add(0, beeOwner.getHeight() / 2, 0);
			loc.setYaw(offset);
			Vector direction = loc.getDirection();
			direction.multiply(beeOwner.getWidth() * 1.41);

			loc.add(direction);

			return loc; // TODO improve positioning for visual effect
		}
	}
}

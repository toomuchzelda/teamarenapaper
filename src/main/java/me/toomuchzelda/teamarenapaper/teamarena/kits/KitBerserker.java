package me.toomuchzelda.teamarenapaper.teamarena.kits;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import me.toomuchzelda.teamarenapaper.utils.ParticleUtils;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.ListIterator;

public class KitBerserker extends Kit {
	private static final ItemStack FOOD_ITEM = ItemBuilder.of(Material.COOKED_BEEF).displayName(Component.text("Insta-Steak"))
		.build();

	private static final ItemStack TEMPLATE_AXE = createAxe(0);
	private static final Component AXE_LORE = Component.text("Feed me kills...", NamedTextColor.DARK_RED);

	private static ItemStack createAxe(int kills) {
		ItemStack item = new ItemStack(Material.DIAMOND_AXE);
		ItemMeta meta = item.getItemMeta();

		if (kills > 0) {
			meta.addEnchant(Enchantment.DAMAGE_ALL, kills, true);
		}
		else {
			// Bluff the enchanted visual effect for negative knockback
			meta.addEnchant(Enchantment.DURABILITY, 1, true);
			meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		}

		ArrayList<Component> lore = new ArrayList<>(2);
		double negativeKB = ((double) (kills * 2) + 1d) * -0.1d;
		negativeKB = MathUtils.round(negativeKB, 1);
		lore.add(Component.text("Knockback " + negativeKB, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
		lore.add(AXE_LORE);

		meta.lore(lore);
		item.setItemMeta(meta);

		return item;
	}

	public KitBerserker() {
		super("Berserker", "Blood hungry, with every kill it swings its axe with more daamge power, but less knockback!" +
				" It hates pants and likes its steak well-done.",
			TEMPLATE_AXE.getType());

		this.setArmor(new ItemStack(Material.IRON_HELMET), new ItemStack(Material.IRON_CHESTPLATE), null,
			new ItemStack(Material.IRON_BOOTS));

		this.setItems(TEMPLATE_AXE, FOOD_ITEM.asQuantity(2));

		this.setCategory(KitCategory.FIGHTER);

		this.setAbilities(new BerserkerAbility());
	}

	private static class BerserkerAbility extends Ability {
		private static final double HEAL_AMOUNT = 8d;
		private static final double KB_LOSS_STEPS = 6d;
		private static final double HEAL_LOSS_STEPS = 9d;

		@Override
		public void onInteract(PlayerInteractEvent event) {
			if (event.useItemInHand() != Event.Result.DENY && event.getAction().isRightClick()) {
				final ItemStack item = event.getItem();
				if (item != null && item.getType() == FOOD_ITEM.getType()) {
					final Player eater = event.getPlayer();
					if (eater.getHealth() + 0.5d < eater.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()) {
						item.subtract();
						PlayerUtils.heal(eater, HEAL_AMOUNT, EntityRegainHealthEvent.RegainReason.CUSTOM);
						eater.getWorld().playSound(eater, Sound.ENTITY_GENERIC_EAT, SoundCategory.PLAYERS, 1.4f, 0.0f);
					}
				}
			}
		}

		@Override
		public void onAttemptedAttack(DamageEvent event) {
			if (event.getDamageType().isMelee() && event.getMeleeWeapon().getType() == TEMPLATE_AXE.getType()) {
				double kills = Main.getPlayerInfo((Player) event.getFinalAttacker()).kills;
				kills = Math.floor(kills);
				if (kills == 0d) return;
				if (kills >= KB_LOSS_STEPS) {
					event.setNoKnockback();
					return;
				}

				kills /= KB_LOSS_STEPS;
				kills = 1d - kills;

				if (event.hasKnockback())
					event.setKnockback(event.getKnockback().multiply(kills));
			}
		}

		@Override
		public void onHeal(EntityRegainHealthEvent event) {
			double kills = Main.getPlayerInfo((Player) event.getEntity()).kills;
			kills = Math.floor(kills);

			if (kills == 0d) return;
			if (kills >= HEAL_LOSS_STEPS - 1d) {
				kills = HEAL_LOSS_STEPS - 1d;
			}

			kills = 1d - (kills / HEAL_LOSS_STEPS);
			event.setAmount(event.getAmount() * kills);
		}

		@Override
		public void onAssist(Player berserker, double amount, Player victim) {
			double killsNow = Main.getPlayerInfo(berserker).kills;
			double killsBefore = killsNow - amount;

			// If the killcount crossed a whole number boundary i.e 1.9 -> 2.1 (crosses 2.0)
			if ((int) killsBefore != (int) killsNow) {
				ListIterator<ItemStack> iter = berserker.getInventory().iterator();
				while (iter.hasNext()) {
					final ItemStack axeCandidate = iter.next();
					if (axeCandidate == null) continue;

					if (axeCandidate.getType() == TEMPLATE_AXE.getType()) {
						iter.set(createAxe((int) killsNow));
					}
				}
			}

			// Blood visual effect
			if (amount == 1.0f) {
				final Location location = victim.getLocation();
				final Location up1 = location.clone().add(0d, 1d, 0d);
				Bukkit.getOnlinePlayers().forEach(player -> {
					ParticleUtils.blockBreakEffect(player, Material.REDSTONE_BLOCK, location);
					ParticleUtils.blockBreakEffect(player, Material.REDSTONE_BLOCK, up1);
				});
			}
		}

		@Override
		public void onPlayerTick(Player player) {
			if (player.isSneaking()) return;

			final double kills = Main.getPlayerInfo(player).kills;
			final int iKills = (int) kills;
			if (iKills == 0) return;
			final int particleCount = MathUtils.randomRange(Math.max(0, iKills - 1), iKills);
			if (particleCount == 0) return; // Don't send packet of 0 particles

			final double killsStep = kills / 25d;
			Location loc = player.getLocation().add(0d, 1d, 0d);
			ParticleUtils.colouredRedstone(loc, particleCount,
				0.4d + killsStep, 0.7d + killsStep, 0.4d + killsStep,
				Color.MAROON, 1d, 0.7f);
		}
	}
}

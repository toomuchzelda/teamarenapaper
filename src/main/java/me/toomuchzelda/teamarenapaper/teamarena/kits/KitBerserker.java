package me.toomuchzelda.teamarenapaper.teamarena.kits;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import me.toomuchzelda.teamarenapaper.utils.ParticleUtils;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
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
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.Map;

public class KitBerserker extends Kit {
	private static final ItemStack FOOD_ITEM = ItemBuilder.of(Material.COOKED_BEEF).displayName(Component.text("Insta-Steak"))
		.build();

	private static final Component AXE_LORE = Component.text("Feed me kills...", NamedTextColor.DARK_RED);
	private static final ItemStack TEMPLATE_AXE = createAxe(0);

	private static ItemStack createAxe(int kills) {
		ItemStack item = new ItemStack(Material.DIAMOND_AXE);
		ItemMeta meta = item.getItemMeta();

		if (kills > 0) {
			meta.addEnchant(Enchantment.SHARPNESS, kills, true);
		}
		else {
			// Bluff the enchanted visual effect for negative knockback
			meta.addEnchant(Enchantment.UNBREAKING, 1, true);
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
		private static final double KB_LOSS_STEPS = 12d;
		private static final double HEAL_LOSS_STEPS = 12d;

		private static final Component BOSSBAR_TITLE = Component.text("Rage!!!", NamedTextColor.DARK_RED);
		private final Map<Player, BossBar> bossBars = new HashMap<>(Bukkit.getMaxPlayers());

		@Override
		public void giveAbility(Player player) {
			BossBar bar = BossBar.bossBar(BOSSBAR_TITLE.append(getHealRateComp(100)), 0f, BossBar.Color.RED, BossBar.Overlay.NOTCHED_6);
			player.showBossBar(bar);
			bossBars.put(player, bar);
		}

		@Override
		public void removeAbility(Player player) {
			player.hideBossBar(bossBars.remove(player));
		}

		@Override
		public void onInteract(PlayerInteractEvent event) {
			if (event.useItemInHand() != Event.Result.DENY && event.getAction().isRightClick()) {
				final ItemStack item = event.getItem();
				if (item != null && item.getType() == FOOD_ITEM.getType()) {
					final Player eater = event.getPlayer();
					if (KitVenom.VenomAbility.isVenomBlockingEating(eater)) {
						Component msg = Component.text("You're too sick to eat", NamedTextColor.LIGHT_PURPLE);
						PlayerUtils.sendKitMessage(eater, msg, msg);
						return;
					}
					if (eater.getHealth() + 0.5d < eater.getAttribute(Attribute.MAX_HEALTH).getValue()) {
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

			if (Double.isNaN(kills)) {
				Main.logger().warning("NaN in berserker heal");
				Thread.dumpStack();
			}
			event.setAmount(event.getAmount() * kills);
		}

		@Override
		public void onAssist(Player berserker, double amount, Player victim) {
			final double killsNow = Main.getPlayerInfo(berserker).kills;

			// Update bossbar
			final double floor = Math.floor(killsNow);
			float remainder = MathUtils.clamp(0f, 1f, (float) (killsNow - floor));
			final BossBar bossBar = bossBars.get(berserker);
			bossBar.progress(remainder);

			final double killsBefore = killsNow - amount;
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

				// Update heal rate on bossbar
				final double healRate = 100d - ((Math.min(floor, HEAL_LOSS_STEPS - 1d) / HEAL_LOSS_STEPS) * 100d);
				Component title = BOSSBAR_TITLE.append(getHealRateComp(healRate));
				bossBar.name(title);
			}

			// Blood visual effect
			// For full kills only
			if (amount == 1.0f) {
				ParticleUtils.bloodEffect(victim);
				berserker.getInventory().addItem(FOOD_ITEM);
			}
		}

		@NotNull
		private static TextComponent getHealRateComp(double percent) {
			return Component.text(" | " + MathUtils.round(percent, 1) + "% healing rate", NamedTextColor.WHITE);
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

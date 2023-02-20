package me.toomuchzelda.teamarenapaper.teamarena.kits;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import org.bukkit.Bukkit;
import org.bukkit.EntityEffect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class KitJuggernaut extends Kit {

	private static final ItemStack BATON = ItemBuilder.of(Material.STICK)
		.displayName(Component.text("Extremely Fragile Baton")).enchant(Enchantment.KNOCKBACK, 4)
		.lore(TextUtils.wrapString(
			"Use this to knock away troublesome enemies. It breaks instantly, but somehow heals itself quickly",
			Style.style(TextUtils.LEFT_CLICK_TO))
		).build();

	private static final int BATON_COOLDOWN = 5 * 20;

    public KitJuggernaut() {
        super("Juggernaut", "Have you ever just wanted full netherite armour? Well, now you get it. But it's really heavy. " +
				"So heavy in fact you can't sprint with it. Still worth it...!\n\n" +
				"At least the sword has Sweeping Edge III.\n" +
				"And you get a Knockback III stick.", Material.NETHERITE_CHESTPLATE);
        setArmor(new ItemStack(Material.NETHERITE_HELMET),
                new ItemStack(Material.NETHERITE_CHESTPLATE),
                new ItemStack(Material.NETHERITE_LEGGINGS),
                new ItemStack(Material.NETHERITE_BOOTS));

        ItemStack sword = new ItemStack(Material.STONE_SWORD);
        ItemMeta swordMeta = sword.getItemMeta();
        swordMeta.addEnchant(Enchantment.SWEEPING_EDGE, 3, true);
		swordMeta.addEnchant(Enchantment.KNOCKBACK, 1, true);
        sword.setItemMeta(swordMeta);

        setItems(sword, BATON);
        setAbilities(new JuggernautAbility());

		setCategory(KitCategory.FIGHTER);
    }

    private static class JuggernautAbility extends Ability {

        @Override
        public void giveAbility(Player player) {
            player.setFoodLevel(6);
        }

        @Override
        public void removeAbility(Player player) {
            player.setFoodLevel(20);
			player.setCooldown(BATON.getType(), 0);
        }

		@Override
		public void onAttemptedAttack(DamageEvent event) {
			if (isBatonAttack(event)) {
				final Player jugg = (Player) event.getFinalAttacker();
				if (jugg.getCooldown(BATON.getType()) > 0) {
					event.setCancelled(true); // Will prevent the below event handler from being called
					jugg.playSound(jugg, Sound.ENTITY_PLAYER_ATTACK_WEAK, 1f, 1.1f);
				}
			}
		}

		@Override
		public void onDealtAttack(DamageEvent event) {
			// When an attack with the baton is successfully landed set the cooldown
			if (isBatonAttack(event)) {
				Player jugg = (Player) event.getFinalAttacker();
				jugg.setCooldown(BATON.getType(), BATON_COOLDOWN);

				jugg.playEffect(EntityEffect.BREAK_EQUIPMENT_MAIN_HAND);
				Bukkit.getScheduler().runTaskLater(Main.getPlugin(),
					() -> jugg.playEffect(EntityEffect.BREAK_EQUIPMENT_MAIN_HAND), 1);
			}
		}

		private static boolean isBatonAttack(DamageEvent event) {
			if (event.getDamageType().is(DamageType.MELEE)) {
				return event.getFinalAttacker() instanceof Player jugg && jugg.getEquipment().getItemInMainHand()
					.isSimilar(BATON);
			}

			return false;
		}

        @Override
        public void onPlayerTick(Player player) {
            player.setFoodLevel(6);
        }
    }
}

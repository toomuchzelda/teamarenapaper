package me.toomuchzelda.teamarenapaper.teamarena.kits;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.fakehitboxes.FakeHitboxManager;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.abilities.centurion.CenturionAbility;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;

public class KitJuggernaut extends Kit {

	private static final ItemStack BATON = ItemBuilder.of(Material.STICK)
		.displayName(Component.text("Extremely Fragile Baton")).enchant(Enchantment.KNOCKBACK, 4)
		.lore(TextUtils.wrapString(
			"Use this to knock away troublesome enemies. It breaks instantly, but somehow heals itself quickly",
			Style.style(TextUtils.LEFT_CLICK_TO))
		).build();

	private static final int BATON_COOLDOWN = 5 * 20;

	private static final Style STYLE = DESC_STYLE;
    public KitJuggernaut() {
        super("juggernaut", "Juggernaut",
			List.of(
				text("Have you ever just wanted full netherite armor?", STYLE),
				text("Well, now you get it. But it's REALLY heavy.", STYLE),
				text("So heavy in fact you can't sprint with it.", STYLE),
				text("Hopefully it's still worth it...!", STYLE),
				empty(),
				text("At least the sword has Sweeping Edge III.", STYLE),
				text("And you get a Knockback III baton.", STYLE),
				text("And also a cool shield to protect your allies.", STYLE)
			), new ItemStack(Material.NETHERITE_CHESTPLATE));
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
        setAbilities(new JuggernautAbility() /*, new CenturionAbility()*/);

		setCategory(KitCategory.FIGHTER);
    }

    private static class JuggernautAbility extends Ability {
		private static final AttributeModifier BIG_MODIFIER = new AttributeModifier(
			new NamespacedKey(Main.getPlugin(), "jugg_big_scale"), 0.3333d, AttributeModifier.Operation.MULTIPLY_SCALAR_1
		);
		private static final AttributeModifier SNEAK_SPEED_MODIFIER = new AttributeModifier(
			new NamespacedKey(Main.getPlugin(), "jugg_sneak_speed"), 0.3, AttributeModifier.Operation.ADD_NUMBER
		);
		private static final AttributeModifier REACH_MODIFIER = new AttributeModifier(
			new NamespacedKey(Main.getPlugin(), "jugg_reach"), 0.019803903d, AttributeModifier.Operation.MULTIPLY_SCALAR_1
		);

        @Override
        public void giveAbility(Player player) {
            player.setFoodLevel(6);
			player.getAttribute(Attribute.SCALE).addModifier(BIG_MODIFIER);
			player.getAttribute(Attribute.SNEAKING_SPEED).addModifier(SNEAK_SPEED_MODIFIER);
			player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE).addModifier(REACH_MODIFIER);
			FakeHitboxManager.setHidden(player, true);
        }

        @Override
        public void removeAbility(Player player) {
            player.setFoodLevel(20);
			player.getAttribute(Attribute.SCALE).removeModifier(BIG_MODIFIER);
			player.getAttribute(Attribute.SNEAKING_SPEED).removeModifier(SNEAK_SPEED_MODIFIER);
			player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE).removeModifier(REACH_MODIFIER);
			FakeHitboxManager.setHidden(player, false);
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

		@Override
		public void onReceiveDamage(DamageEvent event) {
			if (event.getDamageType().is(DamageType.SNIPER_HEADSHOT)) {
				// forced damage mitigation
				event.setFinalDamage(Math.min(19, event.getFinalDamage()));
			}
		}

		private static boolean isBatonAttack(DamageEvent event) {
			if (event.getDamageType().is(DamageType.MELEE)) {
				//return event.getFinalAttacker() instanceof Player jugg && jugg.getEquipment().getItemInMainHand()
				//	.isSimilar(BATON);
				return event.getFinalAttacker() instanceof Player && event.getMeleeWeapon().isSimilar(BATON);
			}

			return false;
		}

        @Override
        public void onPlayerTick(Player player) {
            player.setFoodLevel(6);
        }
    }
}

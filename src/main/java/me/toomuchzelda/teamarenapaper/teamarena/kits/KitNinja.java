package me.toomuchzelda.teamarenapaper.teamarena.kits;

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import io.papermc.paper.event.player.PlayerItemCooldownEvent;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.capturetheflag.CaptureTheFlag;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageTimes;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class KitNinja extends Kit
{
	public static final AttributeModifier NINJA_SPEED_MODIFIER = new AttributeModifier("Ninja Speed", 0.4, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
	public static final Component NO_SPEED_WITH_FLAG = Component.text( "The weight of the flag bears down on you. You're no longer fast!", NamedTextColor.LIGHT_PURPLE);

	public KitNinja() {
		super("Ninja", "A kit that's a fast runner and a faster swinger. Every sword strike it does is weak, but " +
				"it can hit enemies twice as fast, allowing for some brain melting combos.\n\nIt also has ender pearls."
				, Material.ENDER_PEARL);

		ItemStack boots = new ItemStack(Material.CHAINMAIL_BOOTS);
		ItemMeta bootsMeta = boots.getItemMeta();
		bootsMeta.addEnchant(Enchantment.PROTECTION_FALL, 2, true);
		boots.setItemMeta(bootsMeta);

		this.setArmor(new ItemStack(Material.IRON_HELMET), new ItemStack(Material.IRON_CHESTPLATE),
				new ItemStack(Material.CHAINMAIL_LEGGINGS), boots);

		ItemStack sword = new ItemStack(Material.IRON_SWORD);
		ItemMeta swordMeta = sword.getItemMeta();
		swordMeta.displayName(Component.text("Fast Dagger"));
		sword.setItemMeta(swordMeta);

		setItems(sword, new ItemStack(Material.ENDER_PEARL));

		setAbilities(new NinjaAbility());

		setCategory(KitCategory.STEALTH);
	}

	public static class NinjaAbility extends Ability
	{
		@Override
		public void giveAbility(Player player) {
			player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addModifier(NINJA_SPEED_MODIFIER);
		}

		@Override
		public void removeAbility(Player player) {
			player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(NINJA_SPEED_MODIFIER);
		}

		//infinite enderpearls on a cooldown
		@Override
		public void onLaunchProjectile(PlayerLaunchProjectileEvent event) {
			if(event.getProjectile() instanceof EnderPearl) {
				event.setShouldConsume(false);
			}
		}

		@Override
		public void onItemCooldown(PlayerItemCooldownEvent event) {
			if(event.getType() == Material.ENDER_PEARL) {
				event.setCooldown(6 * 20);
			}
		}

		/**
		 * Add/remove their speed when carrying/not carrying the flag.
		 */
		@Override
		public void onPlayerTick(Player player) {
			TeamArena game = Main.getGame();
			if(game instanceof CaptureTheFlag ctf) {
				AttributeInstance speedAttr = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
				if(ctf.isFlagCarrier(player)) {
					if (speedAttr.getModifiers().contains(NINJA_SPEED_MODIFIER)) {
						speedAttr.removeModifier(NINJA_SPEED_MODIFIER);
						player.sendMessage(NO_SPEED_WITH_FLAG);
					}
				}
				else {
					if(!speedAttr.getModifiers().contains(NINJA_SPEED_MODIFIER)) {
						speedAttr.addModifier(NINJA_SPEED_MODIFIER);
					}
				}
			}
		}

		@Override
		public void onAttemptedAttack(DamageEvent event) {
			if(event.getDamageType().isMelee() && event.getVictim() instanceof LivingEntity living) {
				if(event.hasKnockback())
					event.getKnockback().multiply(0.65);

				event.setFinalDamage(event.getFinalDamage() / 2);

				DamageTimes.DamageTime dTimes = DamageTimes.getDamageTime(living, DamageTimes.TrackedDamageTypes.ATTACK);
				int ndt = TeamArena.getGameTick() - dTimes.getLastTimeDamaged();
				if(ndt >= living.getMaximumNoDamageTicks() / 4) {
					event.setIgnoreInvulnerability(true);
				}
			}
		}
	}
}

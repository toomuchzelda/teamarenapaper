package me.toomuchzelda.teamarenapaper.teamarena.kits;

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import io.papermc.paper.event.player.PlayerItemCooldownEvent;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
//import me.toomuchzelda.teamarenapaper.potioneffects.PotionEffectManager;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.capturetheflag.CaptureTheFlag;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageTimes;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.teamarena.kits.filter.KitOptions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.LinkedHashMap;
import java.util.Map;

public class KitNinja extends Kit
{
	private static final AttributeModifier NINJA_SPEED_MODIFIER = new AttributeModifier(new NamespacedKey(Main.getPlugin(), "ninja_speed"),
		0.4, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
	private static final AttributeModifier NINJA_GRAVITY_MODIFIER = new AttributeModifier(new NamespacedKey(Main.getPlugin(), "ninja_gravity"),
		-0.5, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
	private static final Component NO_SPEED_WITH_FLAG = Component.text( "The weight of the flag bears down on you. You're no longer fast!", NamedTextColor.LIGHT_PURPLE);

	private static final ItemStack PEARL = ItemBuilder.of(Material.ENDER_PEARL)
		.displayName(Component.text("Really heavy ender pearl")).build();

	public KitNinja(TeamArena game) {
		super("Ninja", "A kit that's a fast runner " +
				(KitOptions.ninjaFastAttack ? "and a faster swinger. Every sword strike it does is weak, but it can hit enemies twice as fast, allowing for some brain melting combos."
				: " and falls as gracefully as a cat."
				) +
				"\n\nIt also has really heavy ender pearls."
				, Material.ENDER_PEARL);

		ItemStack boots = new ItemStack(Material.CHAINMAIL_BOOTS);
		ItemMeta bootsMeta = boots.getItemMeta();
		bootsMeta.addEnchant(Enchantment.FEATHER_FALLING, 4, true);
		boots.setItemMeta(bootsMeta);

		this.setArmor(new ItemStack(Material.IRON_HELMET), new ItemStack(Material.IRON_CHESTPLATE),
				new ItemStack(Material.CHAINMAIL_LEGGINGS), boots);

		ItemStack sword = new ItemStack(Material.IRON_SWORD);
		if (KitOptions.ninjaFastAttack) {
			ItemMeta swordMeta = sword.getItemMeta();
			swordMeta.displayName(Component.text("Fast Dagger"));
			sword.setItemMeta(swordMeta);
		}

		setItems(sword, PEARL);

		setAbilities(new NinjaAbility(game));

		setCategory(KitCategory.STEALTH);
	}

	public static class NinjaAbility extends Ability
	{
		private static final String EFFECT_KEY = "ninjanightvision";
		private static final PotionEffect NIGHT_VISION_EFFECT = new PotionEffect(PotionEffectType.NIGHT_VISION, PotionEffect.INFINITE_DURATION, 1);

		private static final Vector EXTRA_GRAVITY_DELTA = new Vector(0d, -0.1d, 0d);
		private final Map<Player, EnderPearl> THROWN_PEARLS = new LinkedHashMap<>();

		private final TeamArena game;

		private NinjaAbility(TeamArena game) { this.game = game; }

		@Override
		public void giveAbility(Player player) {
			player.getAttribute(Attribute.MOVEMENT_SPEED).addModifier(NINJA_SPEED_MODIFIER);
			if (KitOptions.ninjaSlowFall)
				player.getAttribute(Attribute.GRAVITY).addModifier(NINJA_GRAVITY_MODIFIER);
			player.addPotionEffect(NIGHT_VISION_EFFECT);
		}

		@Override
		public void removeAbility(Player player) {
			player.getAttribute(Attribute.MOVEMENT_SPEED).removeModifier(NINJA_SPEED_MODIFIER);
			player.getAttribute(Attribute.GRAVITY).removeModifier(NINJA_GRAVITY_MODIFIER);
			player.removePotionEffect(NIGHT_VISION_EFFECT.getType());
			THROWN_PEARLS.remove(player);
		}

		//infinite enderpearls on a cooldown
		@Override
		public void onLaunchProjectile(PlayerLaunchProjectileEvent event) {
			if(event.getProjectile() instanceof EnderPearl pearl) {
				event.setShouldConsume(false);
				THROWN_PEARLS.put(event.getPlayer(), pearl);
			}
		}

		@Override
		public void onItemCooldown(PlayerItemCooldownEvent event) {
			if(event.getType() == Material.ENDER_PEARL) {
				event.setCooldown(6 * 20);
			}
		}

		// Apply extra gravity to ninja's ender pearls
		@Override
		public void onTick() {
			var iter = THROWN_PEARLS.entrySet().iterator();
			while (iter.hasNext()) {
				var entry = iter.next();
				EnderPearl pearl = entry.getValue();

				if (!pearl.isValid()) {
					iter.remove();
				}
				else {
					pearl.setVelocity(pearl.getVelocity().add(EXTRA_GRAVITY_DELTA));
				}
			}
		}

		/**
		 * Add/remove their speed when carrying/not carrying the flag.
		 */
		@Override
		public void onPlayerTick(Player player) {
			if(this.game instanceof CaptureTheFlag ctf) {
				final AttributeInstance speedAttr = player.getAttribute(Attribute.MOVEMENT_SPEED);
				final AttributeInstance gravityAttr = player.getAttribute(Attribute.GRAVITY);
				if(ctf.isFlagCarrier(player)) {
					if (speedAttr.getModifiers().contains(NINJA_SPEED_MODIFIER)) {
						speedAttr.removeModifier(NINJA_SPEED_MODIFIER);
						gravityAttr.removeModifier(NINJA_GRAVITY_MODIFIER);
						player.sendMessage(NO_SPEED_WITH_FLAG);
					}
				}
				else {
					if(!speedAttr.getModifiers().contains(NINJA_SPEED_MODIFIER)) {
						speedAttr.addModifier(NINJA_SPEED_MODIFIER);
						if (KitOptions.ninjaSlowFall)
							gravityAttr.addModifier(NINJA_GRAVITY_MODIFIER);
					}
				}
			}
		}

		@Override
		public void onAttemptedAttack(DamageEvent event) {
			if (!KitOptions.ninjaFastAttack) // shaking and crying rn
				return;

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

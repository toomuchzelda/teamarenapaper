package me.toomuchzelda.teamarenapaper.teamarena.kits;

import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.abilities.GasterBlasterAbility;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketEntity;
import me.toomuchzelda.teamarenapaper.utils.packetentities.SpeechBubbleHologram;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class KitSans extends Kit {

	public KitSans() {
		super("sans", "Sans Undertale", "The easiest enemy. Can only deal 1 damage.", new ItemStack(Material.SKELETON_SKULL));

		this.setArmor(
			ItemUtils.createPlayerHead("f7416e3a14ad48c467276f374d5581ea72290c2fece85f6385e66d4dedb281f9"),
			ItemBuilder.of(Material.LEATHER_CHESTPLATE).color(Color.BLUE).build(),
			ItemBuilder.of(Material.LEATHER_LEGGINGS).color(Color.BLACK).build(),
			ItemBuilder.of(Material.LEATHER_BOOTS).color(Color.FUCHSIA).build()
		);

		this.setItems(GasterBlasterAbility.ITEM);

		this.setAbilities(new SansAbility());
	}

	private static class SansAbility extends Ability {
		public static final Vector VERTICAL = new Vector(0d, 0.4d, 0d);

		@Override
		public void onReceiveDamage(DamageEvent event) {
			if (!event.getDamageType().isInstantDeath())
				event.setFinalDamage(0d);

			if (event.hasKnockback())
				event.setKnockback(event.getKnockback().crossProduct(VERTICAL).add(VERTICAL));

			assert event.getVictim() instanceof Player;
			final Player sans = (Player) event.getVictim();
			final Location sansLoc = sans.getEyeLocation();
			Location spawnLoc;
			if (event.getFinalAttacker() instanceof Entity finalAttacker) {
				if (finalAttacker instanceof LivingEntity livingAttacker)
					spawnLoc = livingAttacker.getEyeLocation().subtract(sansLoc);
				else
					spawnLoc = finalAttacker.getLocation().subtract(sansLoc);

				if (!spawnLoc.isFinite()) {
					spawnLoc = sansLoc;
				}
				else {
					spawnLoc = sansLoc.add(spawnLoc.toVector().normalize());
				}
			}
			else
				spawnLoc = sansLoc.add(sansLoc.getDirection());

			final SpeechBubbleHologram hologram = new SpeechBubbleHologram(
				spawnLoc, null, PacketEntity.VISIBLE_TO_ALL,
				Component.text("MISS", NamedTextColor.WHITE),
				new SpeechBubbleHologram.DamageIndicatorMovementFunc(0.15d, 0.7d)
			);
			hologram.respawn();
		}
	}
}

package me.toomuchzelda.teamarenapaper.teamarena.kits;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.abilities.GasterBlasterAbility;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageTimes;
import me.toomuchzelda.teamarenapaper.teamarena.killstreak.PayloadTestKillstreak;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.teamarena.kits.rewind.KitRewind;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketEntity;
import me.toomuchzelda.teamarenapaper.utils.packetentities.SpeechBubbleHologram;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class KitSans extends Kit {
	public KitSans(KitRewind.RewindAbility rewindAbility) {
		super("sans", "Sans Undertale", "The easiest enemy. Can only deal 1 damage.", new ItemStack(Material.SKELETON_SKULL));

		this.setArmor(
			ItemUtils.createPlayerHead("f7416e3a14ad48c467276f374d5581ea72290c2fece85f6385e66d4dedb281f9"),
			ItemBuilder.of(Material.LEATHER_CHESTPLATE).color(Color.BLUE).build(),
			ItemBuilder.of(Material.LEATHER_LEGGINGS).color(Color.BLACK).build(),
			ItemBuilder.of(Material.LEATHER_BOOTS).color(Color.FUCHSIA).build()
		);

		this.setItems(GasterBlasterAbility.ITEM, KitRewind.TIME_MACHINE);

		this.setAbilities(new SansAbility(), rewindAbility);
	}

	private static class SansAbility extends Ability {
		private static final AttributeModifier HEALTH_MODIFIER = new AttributeModifier(
			new NamespacedKey(Main.getPlugin(), "sanshealth"),
			-0.95d,
			AttributeModifier.Operation.ADD_SCALAR
		);
		private static final Vector VERTICAL = new Vector(0d, 0.4d, 0d);

		private static class SansInfo {
			private int dodges = 24;
			private Component lastMessage = null;
		}
		private final Map<Player, SansInfo> sanses = new HashMap<>();

		@Override
		protected void giveAbility(Player player) {
			this.sanses.put(player, new SansInfo());
			player.getAttribute(Attribute.MAX_HEALTH).addModifier(HEALTH_MODIFIER);
		}

		@Override
		protected void removeAbility(Player player) {
			this.sanses.remove(player);
			player.getAttribute(Attribute.MAX_HEALTH).removeModifier(HEALTH_MODIFIER);
		}

		private static final Component ONE = Component.text("Your movements grow a little wearier.", NamedTextColor.WHITE);
		private static final Component TWO = Component.text("Your movements seem to be slower.", NamedTextColor.WHITE);
		private static final Component THREE = Component.text("You're starting to look really tired.", NamedTextColor.WHITE);
		private Component getWeariness(int dodges) {
			assert dodges >= 0 && dodges <= 24;
			if (dodges > 24 - 4) {
				return null;
			}
			else if (dodges > 24 - 9) {
				return ONE;
			}
			else if (dodges > 24 - 20) {
				return TWO;
			}
			else {
				return THREE;
			}
		}

		@Override
		public void onTick() {
			if (TeamArena.getGameTick() % 3 == 0) {
				for (var entry : this.sanses.entrySet()) {
					final Player sans = entry.getKey();
					final SansInfo sinfo = entry.getValue();

					final Component actionBar = this.getWeariness(sinfo.dodges);
					if (actionBar != null)
						PlayerUtils.sendKitMessage(sans, null, actionBar);
				}
			}
		}

		@Override
		public void onAttemptedAttack(DamageEvent event) {
			if (event.getDamageType().isMelee())
				event.setFinalDamage(1d);
		}

		@Override
		public void onDealtAttack(DamageEvent event) {
			// melee attacks give poison
			if (event.getDamageType().isMelee()) {
				if (event.getVictim() instanceof LivingEntity livingVictim) {
					int duration = 24;
					if (livingVictim instanceof Player playerVictim)
						duration += (int) Main.getPlayerInfo(playerVictim).getKills() * 24;
					boolean hadPoison = livingVictim.hasPotionEffect(PotionEffectType.POISON);
					livingVictim.addPotionEffect(new PotionEffect(PotionEffectType.POISON, duration, 1));
					if (livingVictim.hasPotionEffect(PotionEffectType.POISON)) {
						DamageTimes.DamageTime poisonTime = DamageTimes.getDamageTime(livingVictim, DamageTimes.TrackedDamageTypes.POISON);
						int timeGiven;
						if (hadPoison)
							timeGiven = poisonTime.getTimeGiven();
						else
							timeGiven = TeamArena.getGameTick();

						poisonTime.update(event.getFinalAttacker(), timeGiven);
					}
				}
			}
		}

		@Override
		public void onReceiveDamage(DamageEvent event) {
			if (event.getDamageType().isInstantDeath()) return;

			assert event.getVictim() instanceof Player;
			final Player sans = (Player) event.getVictim();
			final SansInfo sinfo = this.sanses.get(sans);
			sinfo.dodges--;
			if (sinfo.dodges < 0) {
				event.setIgnoreInvulnerability(true);
				event.setFinalDamage(sans.getHealth());
				return;
			}

			final Component msg = this.getWeariness(sinfo.dodges);
			if (msg != null && msg != sinfo.lastMessage) {
				sinfo.lastMessage = msg;
				PlayerUtils.sendKitMessage(sans, msg, msg);
			}

			event.setFinalDamage(0d);
			if (event.hasKnockback()) {
				event.setKnockback(event.getKnockback().crossProduct(VERTICAL).add(VERTICAL));
			}

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

	public static void script(final TeamArena game) {
		final String[] lines = new String[] {
			"heya.",
			"you've been busy, huh?",
			"...",
			"so, i've got a question for ya.",
			"do you think even the worst person can change...?",
			"that everybody can be a good person, if they just try?",
			"heh heh heh heh...",
			"all right.",
			"well, here's a better question.",
			"do you wanna have a bad time?",
			"'cause if you take another step forward...",
			"you are REALLY not going to like what happens next.",
			"welp.",
			"sorry, old lady.",
			"this is why i never make promises."
		};

		final File songFile = new File(new File("songs"), "megalovania.nbs");
		try {
			PayloadTestKillstreak.NbsSong song = PayloadTestKillstreak.loadSong(new FileInputStream(songFile));
			new PayloadTestKillstreak.NbsSongPlayer(song).schedule();
			Bukkit.broadcast(Component.textOfChildren(
				Component.text("Now playing " + song.name(), NamedTextColor.GOLD),
				Component.newline(),
				Component.text("By " + song.author(), NamedTextColor.GOLD),
				Component.newline(),
				Component.text("Original by " + song.originalAuthor(), NamedTextColor.GOLD)
			));
		} catch (IOException e) {
			Main.logger().log(Level.WARNING, "Couldn't load " + songFile, e);
		}

		final String worldName = game.getWorld().getName(); // use world name so the tasks don't hold a reference to a stale TeamArena
		long start = 1;
		for (String line : lines) {
			Bukkit.getScheduler().runTaskLater(Main.getPlugin(), () -> {
				if (!Main.getGame().getWorld().getName().equals(worldName)) // don't persist into the next game
					return;
				Bukkit.broadcast(Component.text(line, NamedTextColor.WHITE));
			}, start);
			start += (line.length() * 2) + 10 + (line.charAt(line.length() - 1) == '?' ? 40 : 0);
		}
	}
}

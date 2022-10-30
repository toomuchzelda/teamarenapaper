package me.toomuchzelda.teamarenapaper.teamarena.kits;

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import io.papermc.paper.event.player.PlayerItemCooldownEvent;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.PacketListeners;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.damage.ArrowImpaleStatus;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import me.toomuchzelda.teamarenapaper.utils.ParticleUtils;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.ArrowBodyCountChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.LinkedHashMap;
import java.util.Map;

public class KitGhost extends Kit
{
	//for the Sound listener in PacketListener.java, register the name as a constant here
	public static final String GHOST_NAME = "Ghost";

	public KitGhost() {
		super(GHOST_NAME, "Invisible, sneaky, has ender pearls, and sus O_O! Although it's not very strong, and can't" +
				" push enemies very far", Material.GHAST_TEAR);

		//armor is already set to AIR in Kit.java

		ItemStack sword = new ItemStack(Material.GOLDEN_SWORD);
		ItemMeta swordMeta = sword.getItemMeta();
		swordMeta.addEnchant(Enchantment.DAMAGE_ALL, 1, true);
		sword.setItemMeta(swordMeta);

		ItemStack pearls = new ItemStack(Material.ENDER_PEARL);
		setItems(sword, new ItemStack(Material.AIR), pearls);

		setAbilities(new GhostAbility());

		setCategory(KitCategory.STEALTH);

		//for silencing ghost footstep sounds.
		PacketListeners.ghostInstance = this;
	}

	@Override
	public boolean isInvisKit() {
		return true;
	}

	public static class GhostAbility extends Ability
	{
		private static class ShieldInfo {
			int lastHit;
			BossBar bar;

			private ShieldInfo() {
				lastHit = 0;
				bar = BossBar.bossBar(BOSSBAR_NAME, 1f, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);
			}
		}

		private static final Component BOSSBAR_NAME = Component.text("Aetherial shield", NamedTextColor.AQUA);
		private static final Component BOSSBAR_RED_NAME = Component.text("Aetherial shield" , NamedTextColor.DARK_RED);
		private static final int SHIELD_COOLDOWN_TIME = 5 * 20; //8 secs
		private static final float SHIELD_REGEN_PER_TICK = 1f / 15f / 20f; //15 seconds until full
		private static final Map<Player, ShieldInfo> SHIELDS = new LinkedHashMap<>();

		@Override
		public void giveAbility(Player player) {
			PlayerUtils.setInvisible(player, true);
			ShieldInfo sinfo = new ShieldInfo();
			player.showBossBar(sinfo.bar);
			SHIELDS.put(player, sinfo);
		}

		@Override
		public void removeAbility(Player player) {
			PlayerUtils.setInvisible(player, false);
			player.setExp(0f);

			ShieldInfo sinfo = SHIELDS.remove(player);
			player.hideBossBar(sinfo.bar);
		}

		@Override
		public void unregisterAbility() {
			var iter = SHIELDS.entrySet().iterator();
			while(iter.hasNext()) {
				var entry = iter.next();
				entry.getKey().hideBossBar(entry.getValue().bar);
				iter.remove();
			}
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

		@Override
		public void onReceiveDamage(DamageEvent event) {
			final Player ghost = event.getPlayerVictim();
			boolean shielded = false;

			//shield damage from indirect sources if there is any shield left
			ShieldInfo sinfo = SHIELDS.get(ghost);
			if(event.getDamageType().isExplosion() || event.getDamageType().isProjectile()) {
				sinfo.lastHit = TeamArena.getGameTick();
				//change bossbar name colour to red to indicate not charging
				if(!sinfo.bar.name().equals(BOSSBAR_RED_NAME))
					sinfo.bar.name(BOSSBAR_RED_NAME);

				if(sinfo.bar.progress() > 0) {
					//reduce shield count
					//the shield effectively is 5 hearts.
					double shieldHearts = sinfo.bar.progress() * 5d;
					shieldHearts -= event.getFinalDamage();

					final Location loc = ghost.getLocation();
					if(shieldHearts < 0) {
						// half of the remaining damage after shield or 4 hearts, whichever is smaller
						event.setFinalDamage(Math.min(Math.abs(shieldHearts / 2d), 8d));

						//also play a "shield broke" sound
						ghost.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.8f);
						ghost.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 1.5f, 1.1f);
					}
					else {
						event.setFinalDamage(0d);
					}

					float newProgress = ((float) shieldHearts) / 5f;
					newProgress = MathUtils.clamp(0f, 1f, newProgress);
					sinfo.bar.progress(newProgress);

					ghost.getWorld().playSound(loc, Sound.ENTITY_SKELETON_DEATH, SoundCategory.PLAYERS, 1f, 2f);


					shielded = true;
				}
			}

			if(event.getDamageType().is(DamageType.PROJECTILE) && event.getAttacker() instanceof AbstractArrow aa) {
				//+1 for the one just added (yet to be added)
				final int arrowsInBody = ghost.getArrowsInBody() + 1;
				Component obf = Component.text("as").decorate(TextDecoration.OBFUSCATED);

				PlayerInfo pinfo = Main.getPlayerInfo(ghost);
				if(pinfo.getPreference(Preferences.KIT_CHAT_MESSAGES)) {
					Component chatText = obf.append(Component.text(" You've been slammed by an arrow! You have "
							+ arrowsInBody + " arrows sticking out of you! ")
							.decoration(TextDecoration.OBFUSCATED, TextDecoration.State.FALSE)
							.append(obf)).color(NamedTextColor.AQUA);
					ghost.sendMessage(chatText);
				}
				if(pinfo.getPreference(Preferences.KIT_ACTION_BAR)) {
					Component actionBarText = obf.append(Component.text("Hit by an arrow! " + arrowsInBody +
							" arrows in you now", NamedTextColor.AQUA).decoration(TextDecoration.OBFUSCATED, TextDecoration.State.FALSE)).append(obf);
					ghost.sendActionBar(actionBarText);
				}

				if (aa.getPierceLevel() > 0 && ArrowImpaleStatus.isImpaling(aa)) {
					//make arrows stick in the Ghost if it's a piercing projectile (normally doesn't)
					ghost.setArrowsInBody(event.getPlayerVictim().getArrowsInBody() + 1);
				}
			}

			if(!shielded) {
				//spawn le reveal particles
				Location baseLoc = event.getVictim().getLocation();
				double x = baseLoc.getX();
				double y = baseLoc.getY();
				double z = baseLoc.getZ();

				TeamArenaTeam team = Main.getPlayerInfo(ghost).team;

				for (int i = 0; i < 20; i++) {
					baseLoc.setX(x + MathUtils.randomRange(-0.3, 0.3));
					baseLoc.setY(y + MathUtils.randomRange(0, 1.4));
					baseLoc.setZ(z + MathUtils.randomRange(-0.3, 0.3));

					ParticleUtils.colouredRedstone(baseLoc, team.getColour(), 2, 1);
				}
			}
		}

		@Override
		public void onDealtAttack(DamageEvent event) {
			if(event.hasKnockback() && event.getDamageType().isMelee()) {
				event.getKnockback().multiply(0.8);
			}
		}

		//slowly regen the shields
		@Override
		public void onTick() {
			final int currentTick = TeamArena.getGameTick();

			for(Map.Entry<Player, ShieldInfo> entry : SHIELDS.entrySet()) {
				ShieldInfo sinfo = entry.getValue();

				//if it increased back from 0 reset the colour back to blue
				if(sinfo.bar.progress() > 0f && sinfo.bar.color() == BossBar.Color.RED) {
					sinfo.bar.color(BossBar.Color.BLUE);
				}

				if(currentTick - sinfo.lastHit >= SHIELD_COOLDOWN_TIME) {
					//if it was not charging and now is charging change it back to blue
					if(sinfo.bar.name().equals(BOSSBAR_RED_NAME)) {
						sinfo.bar.name(BOSSBAR_NAME);
					}
					float newProgress = sinfo.bar.progress();
					newProgress = Math.min(1f, newProgress + SHIELD_REGEN_PER_TICK);
					sinfo.bar.progress(newProgress);
				}
			}
		}

		//@Not an override
		public void arrowCountDecrease(ArrowBodyCountChangeEvent event) {
			int amt = event.getNewAmount();

			Player player = (Player) event.getEntity();
			PlayerInfo pinfo = Main.getPlayerInfo(player);

			if(pinfo.getPreference(Preferences.KIT_CHAT_MESSAGES)) {
				Component chat = Component.text("An arrow has disappeared... " + amt + " arrows stuck in you.").color(NamedTextColor.AQUA);
				player.sendMessage(chat);
			}
			if(pinfo.getPreference(Preferences.KIT_ACTION_BAR)) {
				Component actionBarText = Component.text(amt + " arrows left inside you").color(NamedTextColor.AQUA);
				player.sendActionBar(actionBarText);
			}
		}
	}
}

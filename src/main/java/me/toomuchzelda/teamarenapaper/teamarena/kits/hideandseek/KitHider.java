package me.toomuchzelda.teamarenapaper.teamarena.kits.hideandseek;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.hideandseek.HideAndSeek;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class KitHider extends Kit {

	public static final String NAME = "Hider";

	private static final int TRANSFORM_CD = 30;
	private static final Component TRANSFORM_WAIT_MSG = Component.text("Transform is cooling down", TextColors.ERROR_RED);
	private static final ItemStack TRANSFORM_WAND = ItemBuilder.of(Material.WOODEN_SHOVEL)
		.displayName(Component.text("Transformer", TextColor.color(119, 168, 50)))
		.lore(List.of(
			Component.text("Use on a block or animal to take that form", TextUtils.RIGHT_CLICK_TO),
			Component.text("Blocks and animals you can become will sparkle green", TextUtils.RIGHT_CLICK_TO),
			Component.text("Cooldown: " + TRANSFORM_CD / 20 + " secs", NamedTextColor.LIGHT_PURPLE)
		))
		.build();

	public KitHider(TeamArena game) {
		super(NAME, "Take the form of blocks and animals to hide from seekers", Material.HAY_BLOCK);

		this.setItems(
			TRANSFORM_WAND,
			ItemBuilder.of(Material.BOW).enchant(Enchantment.INFINITY, 1)
				.enchant(Enchantment.POWER, 1).build(),
			new ItemStack(Material.ARROW)
		);
		this.setAbilities(new HiderAbility(game));
	}

	private static class HiderAbility extends Ability {

		public static final double BLOCK_PARTICLE_DISTANCE = 15d;
		public static final double BLOCK_PARTICLE_DISTANCE_SQR = BLOCK_PARTICLE_DISTANCE * BLOCK_PARTICLE_DISTANCE;
		private final TeamArena game;
		private final Map<Player, HiderDisguise> disguiseMap = new HashMap<>(Bukkit.getMaxPlayers());

		private HiderAbility(TeamArena game) {
			this.game = game;
		}

		@Override
		public void giveAbility(Player player) {
			disguiseMap.put(player, new HiderDisguise(player, this.game));
		}

		@Override
		public void removeAbility(Player player) {
			disguiseMap.remove(player).remove();

			HACK_MAP.remove(player);
		}

		@Override
		public void unregisterAbility() {
			this.disguiseMap.forEach((player, hiderDisguise) -> {
				PlayerUtils.setInvisible(player, false);
				hiderDisguise.remove();
			});
			this.disguiseMap.clear();

			HACK_MAP.clear();
		}

		@Override
		public void onTick() {
			final int currentTick = TeamArena.getGameTick();
			PacketSender.Cached particleBundler = new PacketSender.Cached(Bukkit.getOnlinePlayers().size() * 2,
				512);

			// Lazy allocate
			List<LivingEntity> livents = this.game instanceof HideAndSeek ?
				null :
				Collections.emptyList();

			for (Map.Entry<Player, HiderDisguise> entry : this.disguiseMap.entrySet()) {
				final Player player = entry.getKey();
				final HiderDisguise hiderInfo = entry.getValue();
				hiderInfo.tick();

				if (this.game instanceof HideAndSeek hns &&
					(currentTick - hiderInfo.timerSeed) % 5 == 0 &&
					player.getInventory().getItemInMainHand().isSimilar(TRANSFORM_WAND) &&
					player.getCooldown(TRANSFORM_WAND.getType()) == 0) {

					final Location loc = player.getLocation();
					for (BlockCoords coords : hns.getAllowedBlockCoords()) {
						if (coords.distanceSqr(loc) <= BLOCK_PARTICLE_DISTANCE_SQR) {
							playSparkleParticles(player, coords.toLocation(loc.getWorld()).add(0.5, 0.85d, 0.5),
								particleBundler);
						}
					}

					if (livents == null) livents = this.game.getWorld().getLivingEntities();

					for (LivingEntity living : livents) {
						final Location livingLoc = living.getLocation();
						if (hns.isAllowedEntityType(living.getType()) &&
							livingLoc.distanceSquared(loc) <= BLOCK_PARTICLE_DISTANCE_SQR) {

							playSparkleParticles(player, livingLoc.add(0, (living.getHeight() / 2d), 0),
								particleBundler);
						}
					}

					// TODO play for disguised players too
				}

				if (player.getArrowsInBody() > 0) {
					player.setArrowsInBody(0, false);
				}
			}

			particleBundler.flush();
			particleBundler.clear();
		}

		private static void playSparkleParticles(Player player, Location loc,
												 PacketSender particleBundler) {
			ParticleUtils.batchParticles(player, particleBundler,
				Particle.HAPPY_VILLAGER, null,
				loc,
				BLOCK_PARTICLE_DISTANCE,
				1,
				0.25f, 0.15f, 0.25f,
				10, false
			);
		}

		// Hack to stop cancelled entity interactions causing an interaction on the block
		// behind them
		private final HashMap<Player, Integer> HACK_MAP = new HashMap<>();
		@Override
		public void onInteract(PlayerInteractEvent event) {
			final Player clicker = event.getPlayer();
			if (event.getAction().isRightClick() && TRANSFORM_WAND.isSimilar(event.getItem())) {
				Integer clickTick = HACK_MAP.remove(clicker);
				if (clickTick != null && clickTick == TeamArena.getGameTick()) return;

				if (clicker.getCooldown(TRANSFORM_WAND.getType()) == 0) {
					final Block clickedBlock = event.getClickedBlock();
					if (clickedBlock != null) {
						if (!(this.game instanceof HideAndSeek hns) || hns.isAllowedBlockType(clickedBlock.getType())) {
							this.disguiseMap.get(clicker).disguise(clickedBlock);
							clicker.setCooldown(TRANSFORM_WAND.getType(), TRANSFORM_CD);
							clicker.playSound(clicker, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.BLOCKS, 1f, 1f);
							event.setUseInteractedBlock(Event.Result.DENY);
						}
						else if (Main.getPlayerInfo(clicker).messageHasCooldowned("kithiderbadblock" + clickedBlock.getType(), 10)) {
							clicker.playSound(clicker, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.BLOCKS, 1f, 0.5f);
							clicker.spawnParticle(Particle.ANGRY_VILLAGER, event.getInteractionPoint(), 1);
						}
					}
				}
				else {
					transformCooldownMsg(clicker);
				}
			}
		}

		@Override
		public void onInteractEntity(PlayerInteractEntityEvent event) {
			final Player clicker = event.getPlayer();
			if (event.getRightClicked() instanceof LivingEntity livent &&
				TRANSFORM_WAND.isSimilar(event.getPlayer().getInventory().getItem(event.getHand()))) {
				if (clicker.getCooldown(TRANSFORM_WAND.getType()) == 0) {
					if (!(this.game instanceof HideAndSeek hns) || hns.isAllowedEntityType(livent.getType())) {
						this.disguiseMap.get(clicker).disguise(livent);
						clicker.setCooldown(TRANSFORM_WAND.getType(), TRANSFORM_CD);
						clicker.playSound(clicker, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.BLOCKS, 1f, 1f);
						event.setCancelled(true);
						HACK_MAP.put(clicker, TeamArena.getGameTick());
					}
					else if (Main.getPlayerInfo(clicker).messageHasCooldowned("kithiderbadblock" + livent.getType(), 10)) {
						clicker.playSound(clicker, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.BLOCKS, 1f, 0.5f);
						clicker.spawnParticle(Particle.ANGRY_VILLAGER, livent.getEyeLocation(), 1);
					}
				}
				else {
					transformCooldownMsg(clicker);
				}
			}
		}

		private static void transformCooldownMsg(Player clicker) {
			if (Main.getPlayerInfo(clicker).messageHasCooldowned("kithiderinteracttransform", 20)){
				clicker.sendMessage(TRANSFORM_WAIT_MSG);
			}
		}

		@Override
		public void onAttemptedAttack(DamageEvent event) {
			if (this.game instanceof HideAndSeek hns && hns.isAllowedEntityType(event.getVictim().getType())) {
				final Player finalAttacker = (Player) event.getFinalAttacker();
				if (Main.getPlayerInfo(finalAttacker).messageHasCooldowned("hiderouch", 50)) {
					Component msg = Component.textOfChildren(
						Component.text("[" + event.getVictim().getName() + " -> me]", NamedTextColor.GRAY),
						Component.text(" Ouch! I'm your ally ;(", NamedTextColor.WHITE)
					);
					finalAttacker.sendMessage(msg);
				}
				event.setCancelled(true);
			}
		}
	}
}

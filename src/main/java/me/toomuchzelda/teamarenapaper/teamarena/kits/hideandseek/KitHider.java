package me.toomuchzelda.teamarenapaper.teamarena.kits.hideandseek;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.hideandseek.HideAndSeek;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KitHider extends Kit {

	public static final String NAME = "Hider";

	private static final int TRANSFORM_CD = 2 * 20;
	private static final Component TRANSFORM_WAIT_MSG = Component.text("You can't do this yet", TextColors.ERROR_RED);
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

		this.setItems(TRANSFORM_WAND);
		this.setAbilities(new HiderAbility(game));
	}

	private static class HiderAbility extends Ability {

		public static final double BLOCK_PARTICLE_DISTANCE = 15d;
		public static final double BLOCK_PARTICLE_DISTANCE_SQR = BLOCK_PARTICLE_DISTANCE * BLOCK_PARTICLE_DISTANCE;
		private final TeamArena game;
		private final Map<Player, HiderInfo> disguiseMap = new HashMap<>(Bukkit.getMaxPlayers());

		private HiderAbility(TeamArena game) {
			this.game = game;
		}

		@Override
		public void giveAbility(Player player) {
			disguiseMap.put(player, new HiderInfo(player));
		}

		@Override
		public void removeAbility(Player player) {
			disguiseMap.remove(player).remove();
		}

		@Override
		public void unregisterAbility() {
			this.disguiseMap.forEach((player, hiderInfo) -> hiderInfo.remove());
			this.disguiseMap.clear();
		}

		@Override
		public void onTick() {
			final int currentTick = TeamArena.getGameTick();
			PlayerUtils.PacketCache particleBundler = new PlayerUtils.PacketCache();

			// Get now to avoid allocating in the loop
			List<LivingEntity> livents = this.game instanceof HideAndSeek ?
				this.game.getWorld().getLivingEntities() :
				Collections.emptyList();

			this.disguiseMap.forEach((player, hiderInfo) -> {
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
			});

			particleBundler.flush();
			particleBundler.clear();
		}

		private static void playSparkleParticles(Player player, Location loc,
												 PlayerUtils.PacketCache particleBundler) {
			ParticleUtils.batchParticles(player, particleBundler,
				Particle.VILLAGER_HAPPY, null,
				loc,
				BLOCK_PARTICLE_DISTANCE,
				1,
				0.25f, 0.15f, 0.25f,
				1, false
			);
		}

		@Override
		public void onInteract(PlayerInteractEvent event) {
			final Player clicker = event.getPlayer();
			if (event.getAction().isRightClick() && TRANSFORM_WAND.isSimilar(event.getItem())) {
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
							clicker.playSound(clicker, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.BLOCKS, 0.5f, 1f);
							clicker.spawnParticle(Particle.VILLAGER_ANGRY, event.getInteractionPoint(), 1);
						}
					}
				}
				else if (Main.getPlayerInfo(clicker).messageHasCooldowned("kithiderinteracttransform", 20)){
					clicker.sendMessage(TRANSFORM_WAIT_MSG);
				}
			}
		}
	}
}

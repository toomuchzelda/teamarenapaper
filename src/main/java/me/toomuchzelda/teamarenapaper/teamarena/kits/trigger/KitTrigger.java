package me.toomuchzelda.teamarenapaper.teamarena.kits.trigger;

import io.papermc.paper.adventure.PaperAdventure;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.metadata.MetaIndex;
import me.toomuchzelda.teamarenapaper.metadata.MetadataViewer;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaExplosion;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author toomuchzelda
 */
public class KitTrigger extends Kit
{
	public static final Material DETONATOR_MAT = Material.GUNPOWDER;
	public static final Component DETONATOR_NAME = ItemUtils.noItalics(Component.text("BOOM!", TextColors.ERROR_RED));
	public static final ItemStack DETONATOR_ITEM;

	static {
		List<Component> lore = List.of(
				Component.text("Hold this and click to blow up!", TextUtils.RIGHT_CLICK_TO),
				Component.text("While detonating your stability does not affect you!", NamedTextColor.GREEN)
				);
		DETONATOR_ITEM = new ItemStack(DETONATOR_MAT);
		ItemMeta meta = DETONATOR_ITEM.getItemMeta();
		meta.displayName(DETONATOR_NAME);
		meta.lore(lore);
		DETONATOR_ITEM.setItemMeta(meta);
	}

	public KitTrigger() {
		super("Trigger", "There was once a Creeper who was very emotionally unstable. They would swing from extremely " +
						"depressed, to blood-boiling angry over a push. To let off steam, they would find houses " +
						"of noobs, blow them to smithereens, and then watch the noob tears flow. " +
						"Eventually, the cries and rage-quits did not satisfy them anymore. Wanting more " +
						"thrill and action, they picked up a sword (don't ask how), strapped on an explosive vest " +
						"that was as unstable as their feelings and joined Team Arena!"
				, Material.CREEPER_HEAD);

		ItemStack helmet = new ItemStack(Material.NETHERITE_HELMET);
		ItemMeta meta = helmet.getItemMeta();
		meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 8, true);
		helmet.setItemMeta(meta);

		ItemStack chestplate = new ItemStack(Material.LEATHER_CHESTPLATE);
		ItemUtils.colourLeatherArmor(Color.RED, chestplate);
		chestplate.addEnchantment(Enchantment.BINDING_CURSE, 1);

		this.setArmor(helmet, chestplate, null, null);
		ItemStack sword = new ItemStack(Material.IRON_SWORD);
		sword.addEnchantment(Enchantment.DAMAGE_ALL, 3);
		this.setItems(sword, DETONATOR_ITEM);

		this.setAbilities(new TriggerAbility());
	}

	public static class TriggerAbility extends Ability
	{
		//chance to start a stall per tick
		public static final double CHANCE_TO_STALL = 0.02d;
		public static final int MAX_STALL_TIME = 2 * 20;
		public static final int BOOM_TIME = 1 * 20;
		public static final int COUNTDOWN_TIME = 5 * 20;
		public static final float LOWER_BEEPING_BOUND = 0.15f;
		public static final float UPPER_BEEPING_BOUND = 0.85f;

		private enum TriggerMode {
			NORMAL, COUNTDOWN, STALLING, BOOMING
		}

		public static class TriggerInfo {
			public static final int NULL_ALERT_TIME = -1;

			TriggerMode currentMode;
			int timestamp;
			//PacketEntities are the entites that are viewed by trigger's teammates and enemies respectively
			final TriggerCreeper enemyEntity;
			final TriggerCreeper teamEntity;

			int alertTime; //time alert sound was played and creeper ignite effect

			public TriggerInfo(Player trigger, TeamArenaTeam playersTeam) {
				this.currentMode = TriggerMode.COUNTDOWN;
				this.timestamp = TeamArena.getGameTick();
				this.alertTime = NULL_ALERT_TIME;

				this.enemyEntity = new TriggerCreeper(trigger,  player -> !playersTeam.getPlayerMembers().contains(player));
				enemyEntity.setMetadata(MetaIndex.CUSTOM_NAME_OBJ, Optional.of(PaperAdventure.asVanilla(trigger.playerListName())));
				enemyEntity.setMetadata(MetaIndex.CUSTOM_NAME_VISIBLE_OBJ, true);
				enemyEntity.refreshViewerMetadata();
				enemyEntity.respawn();

				this.teamEntity = new TriggerCreeper(trigger,  player -> player != trigger &&
						playersTeam.getPlayerMembers().contains(player));

				teamEntity.setMetadata(MetaIndex.BASE_BITFIELD_OBJ,MetaIndex.BASE_BITFIELD_INVIS_MASK);
				teamEntity.refreshViewerMetadata();
				teamEntity.respawn();
			}

			public TriggerCreeper getViewedCreeper(Player viewer) {
				if(enemyEntity.matchesViewerRule(viewer))
					return enemyEntity;
				else
					return teamEntity;
			}
		}

		private static final Map<Player, TriggerInfo> TRIGGER_INFOS = new HashMap<>();

		public static TriggerInfo getTriggerInfo(Player player) {
			return TRIGGER_INFOS.get(player);
		}

		@Override
		public void unregisterAbility() {
			TRIGGER_INFOS.forEach((player, triggerInfo) -> {
				triggerInfo.teamEntity.remove();
				triggerInfo.enemyEntity.remove();
			});
			TRIGGER_INFOS.clear();
		}

		@Override
		public void giveAbility(Player player) {
			player.setExp(0.5f);
			TeamArenaTeam team = Main.getPlayerInfo(player).team;
			TriggerInfo tinfo = new TriggerInfo(player, team);
			TRIGGER_INFOS.put(player, tinfo);

			//make the player invisible to enemies only
			var iter = Main.getPlayersIter();
			while (iter.hasNext()) {
				var entry = iter.next();
				Player p = entry.getKey();
				if(!team.getPlayerMembers().contains(p)) {
					PlayerInfo pinfo = entry.getValue();
					MetadataViewer viewer = pinfo.getMetadataViewer();
					viewer.updateBitfieldValue(player,  MetaIndex.BASE_BITFIELD_IDX, MetaIndex.BASE_BITFIELD_INVIS_IDX, true);
					viewer.refreshViewer(player);
				}
			}
		}

		@Override
		public void removeAbility(Player player) {
			TriggerInfo tinfo = TRIGGER_INFOS.remove(player);
			tinfo.enemyEntity.remove();
			tinfo.teamEntity.remove();

			for(PlayerInfo pinfo : Main.getPlayerInfos()) {
				pinfo.getMetadataViewer().removeBitfieldValue(player, MetaIndex.BASE_BITFIELD_IDX, MetaIndex.BASE_BITFIELD_INVIS_IDX);
			}
		}

		@Override
		public void onInteract(PlayerInteractEvent event) {
			if(event.getMaterial() == DETONATOR_MAT && (event.getAction().isRightClick() || event.getAction().isLeftClick())) {
				Player trigger = event.getPlayer();
				TriggerInfo info = TRIGGER_INFOS.get(trigger);
				if(info.currentMode == TriggerMode.BOOMING)
					return;

				final int currentTick = TeamArena.getGameTick();

				World world = trigger.getWorld();
				Location loc = trigger.getLocation();
				world.strikeLightningEffect(loc);

				if(info.currentMode == TriggerMode.COUNTDOWN) {
					trigger.setLevel(0);
				}
				info.currentMode = TriggerMode.BOOMING;
				info.timestamp = currentTick;
				//charge the creeper and fuse it
				info.enemyEntity.setMetadata(MetaIndex.CREEPER_STATE_OBJ, 1);
				info.enemyEntity.setMetadata(MetaIndex.CREEPER_CHARGED_OBJ, true);
				info.enemyEntity.refreshViewerMetadata();

				info.teamEntity.setMetadata(MetaIndex.CREEPER_CHARGED_OBJ, true);
				info.teamEntity.setMetadata(MetaIndex.CREEPER_STATE_OBJ, 1);
				info.teamEntity.refreshViewerMetadata();
			}
		}

		@Override
		public void onPlayerTick(Player player) {
			//do exp first
			float expToGain = -(0.1f / 20f);
			float newExp = MathUtils.clamp(0f, 1f, player.getExp() + expToGain);
			final int currentTick = TeamArena.getGameTick();
			TriggerInfo info = TRIGGER_INFOS.get(player);
			final int diff = currentTick - info.timestamp;

			final TriggerMode mode = info.currentMode;
			if(mode == TriggerMode.COUNTDOWN) {
				if(diff >= COUNTDOWN_TIME) {
					info.currentMode = TriggerMode.NORMAL;
					info.timestamp = currentTick;
					player.setLevel(0);
				}
				else {
					int secondsLeft = ((COUNTDOWN_TIME - diff) / 20) + 1;
					player.setLevel(secondsLeft);
				}
			}
			else if(mode == TriggerMode.STALLING) {
				//amount of time to stall for is decided at dice-roll time, so timestamp actually stores the time the
				// stall wears off
				if(currentTick >= info.timestamp) {
					info.currentMode = TriggerMode.NORMAL;
					info.timestamp = currentTick;
				}
			}
			else if(mode == TriggerMode.NORMAL) {
				stabilityTick(player, newExp);

				//only chance to stall if balance not urgently high or low
				if (newExp >= LOWER_BEEPING_BOUND && newExp <= UPPER_BEEPING_BOUND) {
					//decide if stall
					boolean stall = MathUtils.random.nextDouble() <= CHANCE_TO_STALL;
					if (stall) {
						int stallTime = currentTick + MathUtils.randomMax(MAX_STALL_TIME);
						info.currentMode = TriggerMode.STALLING;
						info.timestamp = stallTime;
					}
				}
			}
			else { //booming
				boomTick(player, info.timestamp, currentTick);
			}

			//play alert sound to trigger and update creepers igniting
			if(mode == TriggerMode.NORMAL || mode == TriggerMode.STALLING) {
				if(newExp <= LOWER_BEEPING_BOUND || newExp >= UPPER_BEEPING_BOUND) {
					if(info.alertTime == TriggerInfo.NULL_ALERT_TIME) {
						info.alertTime = currentTick;
						info.enemyEntity.setMetadata(MetaIndex.CREEPER_STATE_OBJ, 1);
						info.enemyEntity.refreshViewerMetadata();
					}

					if((currentTick - info.alertTime) % 10 == 0) {
						player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.AMBIENT, 2f, 1.4f);
					}
				}
				else if(info.alertTime != TriggerInfo.NULL_ALERT_TIME) {
					info.alertTime = TriggerInfo.NULL_ALERT_TIME;
					info.enemyEntity.setMetadata(MetaIndex.CREEPER_STATE_OBJ, -1);
					info.enemyEntity.refreshViewerMetadata();
				}
			}
		}

		private void boomTick(Player player, int timeStarted, int currentTick) {
			final int diff = currentTick - timeStarted;
			float expProgress = (float) diff / (float) BOOM_TIME;
			expProgress = MathUtils.clamp(0f, 1f, expProgress);
			player.setExp(expProgress);

			//esplode
			if(diff >= BOOM_TIME) {
				TeamArenaExplosion boom = new TeamArenaExplosion(null, 10d, 2d, 40d, 2d, 0.7d, DamageType.TRIGGER_BOOM, player);
				boom.explode();

				DamageEvent selfKill = DamageEvent.newDamageEvent(player, 999999d, DamageType.TRIGGER_BOOM_SELF, null, false);
				Main.getGame().queueDamage(selfKill);
			}
			else {
				if(diff % 10 == 0) {
					player.getWorld().playSound(player.getLocation(), Sound.ENTITY_CREEPER_PRIMED, SoundCategory.PLAYERS, 4.5f, 1.2f);
				}
			}
		}

		private void stabilityTick(Player player, float newExp) {
			player.setExp(newExp);

			//check and colour the chestplate
			ItemStack chestplate = player.getEquipment().getChestplate();
			//they should never be able to remove the chestplate
			if(chestplate == null || chestplate.getType() != Material.LEATHER_CHESTPLATE) {
				Main.logger().warning(player.getName() + " somehow is not wearing Creeper chestplate!");
				DamageEvent kill = DamageEvent.newDamageEvent(player, 99999999999d, DamageType.SUICIDE, null, false);
				Main.getGame().queueDamage(kill);
				return;
			}

			//get their exp progress from the middle to either end, not from left -> right
			float exp = newExp;
			exp -= 0.5f; //from -0.5 to 0.5
			exp *= 2f; // from -1 to 1
			exp = Math.abs(exp);

			int redPart = (int) (exp * 255f);
			int greenPart = (int) ((1- exp) * 255f);
			Color newColor = Color.fromRGB(redPart, greenPart, 0);
			ItemUtils.colourLeatherArmor(newColor, chestplate);

			if(newExp <= 0f || newExp >= 0.99f) {
				double damage = player.getHealth() * 1.5d;
				TeamArenaExplosion explosion = new TeamArenaExplosion(null, 7f, 1f, damage, 1d, 0.4d,
						DamageType.TRIGGER_UNSTABLE_EXPLODE, player);
				explosion.explode();

				DamageEvent selfKill = DamageEvent.newDamageEvent(player, 99999d, DamageType.TRIGGER_UNSTABLE_SELF_KILL, null, false);
				Main.getGame().queueDamage(selfKill);
			}
		}

		@Override
		public void onMove(PlayerMoveEvent event) {
			if(event instanceof PlayerTeleportEvent) {
				return;
			}

			//don't do anything if stalling or booming
			Player trigger = event.getPlayer();

			TriggerInfo info = TRIGGER_INFOS.get(trigger);
			//ignore stall if sprinting
			if(info.currentMode == TriggerMode.STALLING)
				if(!trigger.isSprinting())
					return;

			if(info.currentMode != TriggerMode.NORMAL)
				return;

			Vector from = event.getFrom().toVector();
			Vector to = event.getTo().toVector();
			float lengthSquared = (float) from.distanceSquared(to);
			if(lengthSquared > 0) {
				float dist = (float) Math.sqrt(lengthSquared);
				dist *= 0.04f;
				float exp = trigger.getExp() + dist;
				exp = MathUtils.clamp(0f, 1f, exp);
				trigger.setExp(exp);
			}
		}

		@Override
		public void onReceiveDamage(DamageEvent event) {
			TriggerInfo tinfo = TRIGGER_INFOS.get((Player) event.getVictim());
			EntityUtils.playEffect(tinfo.enemyEntity, ClientboundAnimatePacket.HURT);
		}
	}
}
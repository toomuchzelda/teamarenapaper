package me.toomuchzelda.teamarenapaper.teamarena.kits;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaExplosion;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import me.toomuchzelda.teamarenapaper.utils.TextColors;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
		helmet.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 4);

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
		//if stall, then timestamp is the time that the stall period will end
		// else, it's the time that the current BOOM was triggered
		private record TriggerInfo(boolean stall, int timestamp) {};

		//chance to start a stall per tick
		public static final double CHANCE_TO_STALL = 0.02d;
		public static final int MAX_STALL_TIME = 2 * 20;
		public static final int BOOM_TIME = 50;

		private final Map<Player, TriggerInfo> TRIGGER_ACTIONS = new HashMap<>();

		@Override
		public void unregisterAbility() {
			TRIGGER_ACTIONS.clear();
		}

		@Override
		public void giveAbility(Player player) {
			player.setExp(0.5f);
		}

		@Override
		public void removeAbility(Player player) {
			TRIGGER_ACTIONS.remove(player);
		}

		@Override
		public void onInteract(PlayerInteractEvent event) {
			if(event.getMaterial() == DETONATOR_MAT && event.getAction().isRightClick()) {
				Player trigger = event.getPlayer();
				final int currentTick = TeamArena.getGameTick();

				World world = trigger.getWorld();
				Location loc = trigger.getLocation();
				world.strikeLightningEffect(loc);

				TriggerInfo info = new TriggerInfo(false, currentTick);
				//override anything that was already there
				TRIGGER_ACTIONS.put(trigger, info);
			}
		}

		@Override
		public void onPlayerTick(Player player) {
			//do exp first
			float expToGain = -(0.1f / 20f);
			float newExp;
			final int currentTick = TeamArena.getGameTick();

			int timeStarted = 0;

			TriggerInfo info = TRIGGER_ACTIONS.get(player);
			boolean stabilityTick = true;
			if(info != null) {
				//stall time ran out
				if(info.stall()) {
					if(currentTick >= info.timestamp()) {
						TRIGGER_ACTIONS.remove(player);
					}
					//still stalling
					else {
						expToGain = 0f;
					}
				}
				else {
					stabilityTick = false;
					timeStarted = info.timestamp();
				}
			}
			//random chance to start a stall
			else {
				double rand = MathUtils.random.nextDouble();
				if(rand <= CHANCE_TO_STALL) {
					int length = MathUtils.randomMax(MAX_STALL_TIME - 1) + currentTick;
					info = new TriggerInfo(true, length);
					TRIGGER_ACTIONS.put(player, info);
					expToGain = 0f;
				}
			}

			if(stabilityTick) {
				newExp = MathUtils.clamp(0f, 1f, player.getExp() + expToGain);
				stabilityTick(player, newExp);
			}
			else {
				boomTick(player, timeStarted, currentTick);
			}
		}

		private void boomTick(Player player, int timeStarted, int currentTick) {
			int diff = currentTick - timeStarted;
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
					player.getWorld().playSound(player.getLocation(), Sound.ENTITY_CREEPER_PRIMED, SoundCategory.PLAYERS, 3f, 1.2f);
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
			TriggerInfo info = TRIGGER_ACTIONS.get(event.getPlayer());
			if(info != null) {
				return;
			}

			Vector from = event.getFrom().toVector();
			Vector to = event.getTo().toVector();
			float lengthSquared = (float) from.distanceSquared(to);
			if(lengthSquared > 0) {
				float dist = (float) Math.sqrt(lengthSquared);
				dist *= 0.05f;
				float exp = event.getPlayer().getExp() + dist;
				exp = MathUtils.clamp(0f, 1f, exp);
				event.getPlayer().setExp(exp);
			}
		}
	}
}
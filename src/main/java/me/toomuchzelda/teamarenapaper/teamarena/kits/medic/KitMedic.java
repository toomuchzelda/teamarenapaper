package me.toomuchzelda.teamarenapaper.teamarena.kits.medic;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.metadata.MetaIndex;
import me.toomuchzelda.teamarenapaper.metadata.MetadataViewer;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Kit medic Kit and Ability class
 *
 * @author toomuchzelda
 */
public class KitMedic extends Kit
{
	private static final ItemStack WAND;
	private static final ItemStack POTION;

	static {
		{ // Init wand
			WAND = new ItemStack(Material.FISHING_ROD);
			ItemMeta wandMeta = WAND.getItemMeta();;
			wandMeta.displayName(ItemUtils.noItalics(Component.text("Magical healing fishing rod", TextColor.color(245, 66, 149))));
			Component right = Component.text("Right Click: Bind to a teammate and heal them. Click again or hold different item to release", TextUtils.RIGHT_CLICK_TO);
			wandMeta.lore(List.of(right));
			WAND.setItemMeta(wandMeta);
		}

		{ // Init potion
			//Create our Regeneration potion effect first
			PotionEffect effect = new PotionEffect(PotionEffectType.REGENERATION, 5 * 20, 0, true, true, true);

			POTION = new ItemStack(Material.POTION);
			PotionMeta meta = (PotionMeta) POTION.getItemMeta();
			meta.clearCustomEffects();
			meta.addCustomEffect(effect, true); // add our regen effect
			POTION.setItemMeta(meta);
		}
	}

	public KitMedic() {
		super("Medic", "Heal your teammates with wand. Heal 1 teammate at a time. All your base are belong to us." +
						" Can see the health of teammates and enemies!"
				, POTION);

		this.setArmor(new ItemStack(Material.GOLDEN_HELMET),
				new ItemStack(Material.GOLDEN_CHESTPLATE),
				new ItemStack(Material.GOLDEN_LEGGINGS),
				new ItemStack(Material.GOLDEN_BOOTS));

		this.setItems(new ItemStack(Material.IRON_SWORD), WAND, POTION);

		this.setAbilities(new MedicAbility());
	}

	/**
	 * Gapples don't give absorption hearts
	 * Wand binding and healing
	 * See other player's health.
	 */
	private static class MedicAbility extends Ability {

		private static final Component HOLD_IN_MAIN_HAND = Component.text(
				"You're not ambidextrous! Use the fishing rod in your main hand", TextColors.ERROR_RED);
		private static final Component STOPPED_HEALING = Component.text("Stopped healing", NamedTextColor.GOLD);
		private static final double HEAL_PER_TICK = 1.5d / 20d; // 0.75 hearts per second

		private record HealInfo(LivingEntity healed, int startTime) {}
		private final Map<Player, HealInfo> currentHeals = new LinkedHashMap<>();

		/** Prevent losing the potion when drinking it */
		@Override
		public void onConsumeItem(PlayerItemConsumeEvent event) {
			if(event.getItem().isSimilar(POTION)) {
				event.setReplacement(POTION); // Setting the replacement should effectively prevent losing the item
			}
		}

		/** Prevent casting the fishing rod
		 *  If the medic is healing someone then cast the fishing rod and attach it to the healed entity
		 *  for visual effect.
		 */
		@Override
		public void onFish(PlayerFishEvent event) {
			final Player medic = event.getPlayer();
			if(event.getState() == PlayerFishEvent.State.FISHING &&
					medic.getEquipment().getItem(event.getHand()).isSimilar(WAND)) {

				if(event.getHand() == EquipmentSlot.HAND) {
					// Speed up the fishing hook to make aiming easier
					final Vector vel = event.getHook().getVelocity().multiply(2d);
					event.getHook().setVelocity(vel);

					HealInfo hinfo = currentHeals.get(medic);
					if(hinfo != null) { // If a heal target was right-clicked directly in the PlayerInteractEntityEvent
						event.getHook().setHookedEntity(hinfo.healed());
					}
				}
				else {
					event.setCancelled(true);
					if(Main.getPlayerInfo(medic).messageHasCooldowned("medicMH", 3 * 20)) {
						medic.sendMessage(HOLD_IN_MAIN_HAND);
					}
				}
			}
			else if(event.getState() != PlayerFishEvent.State.FISHING && currentHeals.containsKey(medic)) {
				HealInfo hinfo = currentHeals.remove(medic);
				if(hinfo != null)
					stopGlowing(medic, hinfo);
			}
		}

		/** Initiate healing a player when right click with wand
		 *  Can only start healing if wand is in mainhand.
		 */
		@Override
		public void onInteractEntity(PlayerInteractEntityEvent event) {
			final Player medic = event.getPlayer();
			if(medic.getEquipment().getItem(event.getHand()).isSimilar(WAND)) {
				if(event.getHand() == EquipmentSlot.HAND &&
						event.getRightClicked() instanceof LivingEntity healed) {
					startHealing(medic, healed);
				}
			}
		}

		private void startHealing(Player medic, LivingEntity healed) {
			final PlayerInfo pinfo = Main.getPlayerInfo(medic);
			// If healed is a player, don't heal them if they're not on the medic's team
			if(healed instanceof Player healedP && !pinfo.team.getPlayerMembers().contains(healedP))
				return;

			HealInfo hinfo = currentHeals.put(medic, new HealInfo(healed, TeamArena.getGameTick()));
			if(hinfo != null) { // Stop healing the previous if there was any.
				stopGlowing(medic, hinfo);
			}

			medic.sendMessage(getHealingMessage(healed));

			// Set the healed to glow for the medic only.
			MetadataViewer viewer = pinfo.getMetadataViewer();
			viewer.setViewedValue(MetaIndex.BASE_BITFIELD_IDX, MetaIndex.GLOWING_METADATA_VALUE, healed);
			viewer.refreshViewer(healed);
		}

		private void stopGlowing(Player medic, HealInfo hinfo) {
			PlayerUtils.sendKitMessage(medic, null, STOPPED_HEALING);
			// Remove the medic-only glowing effect.
			MetadataViewer viewer = Main.getPlayerInfo(medic).getMetadataViewer();
			viewer.removeBitfieldValue(hinfo.healed(), MetaIndex.BASE_BITFIELD_IDX, MetaIndex.BASE_BITFIELD_GLOWING_IDX);
			viewer.refreshViewer(hinfo.healed());
		}

		/** Process players that have cast their fishing rod (not healing) */
		@Override
		public void onPlayerTick(final Player medic) {
			final FishHook fishHook = medic.getFishHook();
			if(fishHook == null) // Fishing rod not cast
				return;

			final Entity hookedEntity = fishHook.getHookedEntity();
			if(!(hookedEntity instanceof LivingEntity hookedLiving)) // Hooked onto a non-living entity
				return;

			// If they aren't already healing this living entity, and they can heal this living entity,
			// then start healing them.

			HealInfo healInfo = currentHeals.get(medic);
			if(healInfo != null && healInfo.healed() == hookedLiving)
				return;

			if(!(hookedLiving instanceof Player hookedPlayer) ||
					Main.getPlayerInfo(medic).team.getPlayerMembers().contains(hookedPlayer)) {
				startHealing(medic, hookedLiving);
			}
		}

		/** Process all the current healings */
		@Override
		public void onTick() {
			for (Iterator<Map.Entry<Player, HealInfo>> iter = currentHeals.entrySet().iterator();
				 iter.hasNext(); ) {

				Map.Entry<Player, HealInfo> entry = iter.next();
				final Player medic = entry.getKey();
				final HealInfo hinfo = entry.getValue();

				// The PlayerFishEvent is not called when the player stops holding the rod by switching items
				// or something. So check the item every tick here and stop healing apporpriately
				if (!medic.getEquipment().getItemInMainHand().isSimilar(WAND)) {
					iter.remove();
					stopGlowing(medic, hinfo);
					continue;
				}

				final LivingEntity healed = hinfo.healed();

				if (healed instanceof Player healedPlayer) {
					PlayerUtils.heal(healedPlayer, HEAL_PER_TICK, EntityRegainHealthEvent.RegainReason.MAGIC_REGEN);
				}
				else {
					final double maxHealth = healed.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
					healed.setHealth(Math.min(healed.getHealth() + HEAL_PER_TICK, maxHealth));
				}

				final int mod = (TeamArena.getGameTick() - hinfo.startTime()) % 20;
				final Location healedLoc = healed.getLocation();
				if (mod == 0 || mod == 10) {
					PlayerUtils.sendKitMessage(medic, null, getHealingMessage(healed));

					if(mod == 0)
						healed.getWorld().spawnParticle(Particle.HEART, healedLoc.clone().add(0, healed.getHeight(), 0), 1);
				}

				if (mod == 10) {
					// TODO: healing line particles?
				}
			}
		}


		private static Component getHealingMessage(LivingEntity healed) {
			double healthPercent =
					(healed.getHealth() / healed.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()) * 100d;
			healthPercent = MathUtils.round(healthPercent, 2);

			Component healingName = Component.text()
					.append(Component.text("Healing "))
					.append(EntityUtils.getComponent(healed))
					.color(NamedTextColor.LIGHT_PURPLE)
					.append(Component.text().append(Component.text(" " + healthPercent + "%")).append(TextColors.HEART).build())
					.build();

			return healingName;
		}
	}
}

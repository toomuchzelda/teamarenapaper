package me.toomuchzelda.teamarenapaper.teamarena.kits.medic;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.metadata.MetaIndex;
import me.toomuchzelda.teamarenapaper.metadata.MetadataViewer;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.KitCategory;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
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
import org.bukkit.util.RayTraceResult;
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
	private static final TextColor ITEM_NAME_COLOUR = TextColor.color(245, 66, 149);

	static {
		{ // Init wand
			WAND = new ItemStack(Material.FISHING_ROD);
			ItemMeta wandMeta = WAND.getItemMeta();;
			wandMeta.displayName(ItemUtils.noItalics(Component.text("Magical healing fishing rod", ITEM_NAME_COLOUR)));

			Style style = Style.style(TextUtils.RIGHT_CLICK_TO).decoration(TextDecoration.ITALIC, false);
			wandMeta.lore(TextUtils.wrapString("Right Click: Hook a teammate and heal them. Unhook or hold different item to release", style));
			WAND.setItemMeta(wandMeta);
		}

		{ // Init potion
			//Create our Regeneration potion effect first
			PotionEffect effect = new PotionEffect(PotionEffectType.REGENERATION, 5 * 20, 0, true, true, true);

			POTION = new ItemStack(Material.POTION);
			PotionMeta meta = (PotionMeta) POTION.getItemMeta();
			meta.displayName(ItemUtils.noItalics(Component.text("Magical unlimited healing potion", ITEM_NAME_COLOUR)));
			meta.lore(List.of(ItemUtils.noItalics(Component.text("Right Click: Drink Me", TextUtils.RIGHT_CLICK_TO))));
			meta.clearCustomEffects();
			meta.addCustomEffect(effect, true); // add our regen effect
			POTION.setItemMeta(meta);
		}
	}

	public KitMedic() {
		super("Medic", "Heal your teammates with wand. Heal 1 teammate at a time. All your base are belong to us."
				, POTION);

		this.setArmor(new ItemStack(Material.GOLDEN_HELMET),
				new ItemStack(Material.GOLDEN_CHESTPLATE),
				new ItemStack(Material.GOLDEN_LEGGINGS),
				new ItemStack(Material.GOLDEN_BOOTS));

		this.setItems(new ItemStack(Material.IRON_SWORD), WAND, POTION);

		this.setAbilities(new MedicAbility());

		this.setCategory(KitCategory.SUPPORT);
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
		private static final Component NEED_LINE_OF_SIGHT = Component.text("You need a direct line of sight " +
				"to the healing target", TextColors.ERROR_RED);
		private static final Component NEED_LINE_OF_SIGHT_ACTIONBAR = Component.text("Can't see the target",
				TextColors.ERROR_RED);

		private static final Component TOO_FAR = Component.text("You need to be close to the healing target", TextColors.ERROR_RED);
		private static final Component TOO_FAR_ACTION_BAR = Component.text("Too far from the target", TextColors.ERROR_RED);

		private static final double HEAL_PER_TICK = 1.5d / 20d; // 0.75 hearts per second
		private static final double MAX_HEALING_DISTANCE = 15d;
		private static final double MAX_HEALING_DISTANCE_SQR = MAX_HEALING_DISTANCE * MAX_HEALING_DISTANCE;

		private record HealInfo(LivingEntity healed, int startTime, AttachedMedicGuardian guardian, AttachedMedicGuardian selfGuardian) {}
		private final Map<Player, HealInfo> currentHeals = new LinkedHashMap<>();

		@Override
		public void removeAbility(Player medic) {
			HealInfo hinfo = currentHeals.remove(medic);
			if(hinfo != null) {
				stopGlowing(medic, hinfo);
			}
		}

		@Override
		public void unregisterAbility() {
			var iter = currentHeals.entrySet().iterator();
			while(iter.hasNext()) {
				var entry = iter.next();
				stopGlowing(entry.getKey(), entry.getValue());
				iter.remove();
			}
		}

		/** Prevent losing the potion when drinking it */
		@Override
		public void onConsumeItem(PlayerItemConsumeEvent event) {
			if(event.getItem().isSimilar(POTION)) {
				event.setReplacement(POTION); // Setting the replacement should effectively prevent losing the item
			}
		}

		/** Prevent casting the fishing rod
		 *  If the medic is healing someone (directly right clicked on in onInteractEntity), then cast the
		 *  fishing rod and attach it to the healed entity for visual effect.
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
			if(!Main.getGame().canHeal(medic, healed))
				return;

			// Spawn an invisible guardian to use its beam.
			// One guardian that follows the medic and targets the healed. Everyone except the medic sees this.
			// One guardian that follows the healed and targets the medic. Only the medic sees this.
			// The reason for these two guardians is that having one that the medics sees at its feet blocks the
			// medic from interacting with anything directly below it. So spawn one at the healed loc instead, and
			// reverse the aim to the medic. Unfortunately the direction of the beam animation will be reversed for
			// the medic but hopefully no-one will notice.
			AttachedMedicGuardian guardian = new AttachedMedicGuardian(medic, viewer -> viewer != medic);
			AttachedMedicGuardian selfGuardian = new AttachedMedicGuardian(healed, viewer -> viewer == medic);
			HealInfo hinfo = currentHeals.put(medic, new HealInfo(healed, TeamArena.getGameTick(), guardian, selfGuardian));
			if(hinfo != null) { // Stop healing the previous if there was any.
				stopGlowing(medic, hinfo);
			}

			medic.sendMessage(getHealingMessage(healed));

			// Set the healed to glow for the medic only.
			MetadataViewer viewer = pinfo.getMetadataViewer();
			viewer.setViewedValue(MetaIndex.BASE_BITFIELD_IDX, MetaIndex.GLOWING_METADATA_VALUE, healed);
			viewer.refreshViewer(healed);

			guardian.setTarget(healed.getEntityId());
			guardian.respawn();

			selfGuardian.setTarget(medic.getEntityId());
			selfGuardian.respawn();

			// Hide the fishing hook from the hooked player as it gets in their vision and covers their view.
			if(healed instanceof Player hookedPlayer) {
				// Must be done after the fishing hook has spawned.
				Bukkit.getScheduler().runTask(Main.getPlugin(), bukkitTask -> {
					FishHook hook = medic.getFishHook();
					if(hook != null) // Need to null check as medic.getFishHook may be null 1 tick later.
						PlayerUtils.sendPacket(hookedPlayer, EntityUtils.getRemoveEntitiesPacket(hook));
				});
			}
		}

		private void stopGlowing(Player medic, HealInfo hinfo) {
			PlayerUtils.sendKitMessage(medic, null, STOPPED_HEALING);
			// Remove the medic-only glowing effect.
			MetadataViewer viewer = Main.getPlayerInfo(medic).getMetadataViewer();
			viewer.removeBitfieldValue(hinfo.healed(), MetaIndex.BASE_BITFIELD_IDX, MetaIndex.BASE_BITFIELD_GLOWING_IDX);
			viewer.refreshViewer(hinfo.healed());

			hinfo.guardian().remove();
			hinfo.selfGuardian().remove();
		}

		/** Process players that have cast their fishing rod (not healing)
		 *  If they have hooked a valid entity, start healing them.
		 */
		@Override
		public void onPlayerTick(final Player medic) {
			if(!medic.getEquipment().getItemInMainHand().isSimilar(WAND)) // Must hold in main hand.
				return;

			final FishHook fishHook = medic.getFishHook();
			if(fishHook == null) // Fishing rod not cast
				return;

			final Entity hookedEntity = fishHook.getHookedEntity();
			if(!(hookedEntity instanceof LivingEntity hookedLiving)) // Hooked onto a non-living entity
				return;

			// If they aren't already healing this living entity, and they can heal this living entity,
			// then start healing them.
			HealInfo healInfo = currentHeals.get(medic);
			if(healInfo != null && healInfo.healed() == hookedLiving) // They are already healing this target, return
				return;

			if(Main.getGame().canHeal(medic, hookedLiving)) {
				final LineOfSight returnVal = lineOfSightCheck(medic, hookedLiving);
				if(returnVal == LineOfSight.CAN_HEAL) {
					startHealing(medic, hookedLiving);
				}
				else { // Can't hook for no line of sight or they're too far - tell them why but keep the hook there.
					PlayerInfo pinfo = Main.getPlayerInfo(medic);
					if (returnVal == LineOfSight.NO_LINE_OF_SIGHT) {
						if (pinfo.messageHasCooldowned("mdLOS", 5 * 20 * 60)) {
							PlayerUtils.sendKitMessage(medic, NEED_LINE_OF_SIGHT, null, pinfo);
						}
						if (pinfo.messageHasCooldowned("mdLOSAB", 10)) {
							PlayerUtils.sendKitMessage(medic, null, NEED_LINE_OF_SIGHT_ACTIONBAR, pinfo);
						}
					}
					else { // TOO_FAR
						if (pinfo.messageHasCooldowned("mdLOSDist", 5 * 20 * 60)) {
							PlayerUtils.sendKitMessage(medic, TOO_FAR, null, pinfo);
						}
						if (pinfo.messageHasCooldowned("mdLOSDistAB", 10)) {
							PlayerUtils.sendKitMessage(medic, null, TOO_FAR_ACTION_BAR, pinfo);
						}
					}
				}
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
				final LivingEntity healed = hinfo.healed();

				// The PlayerFishEvent is not called when the player stops holding the rod by switching items
				// or something. So check the item every tick here and stop healing apporpriately
				// The PlayerFishEvent is also not called when the hook is released by moving our of range,
				// so check for that here too
				final boolean stopHealing = !medic.getEquipment().getItemInMainHand().isSimilar(WAND) ||
						medic.getFishHook() == null || medic.getFishHook().getHookedEntity() != hinfo.healed() ||
						lineOfSightCheck(medic, healed) != LineOfSight.CAN_HEAL;
				if (stopHealing) {
					iter.remove();
					stopGlowing(medic, hinfo);
					continue;
				}

				if (healed instanceof Player healedPlayer) {
					PlayerUtils.heal(healedPlayer, HEAL_PER_TICK, EntityRegainHealthEvent.RegainReason.MAGIC_REGEN);
				}
				else {
					final double maxHealth = healed.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
					healed.setHealth(Math.min(healed.getHealth() + HEAL_PER_TICK, maxHealth));
				}

				final int mod = (TeamArena.getGameTick() - hinfo.startTime()) % 20;
				if (mod == 0 || mod == 10) {
					PlayerUtils.sendKitMessage(medic, null, getHealingMessage(healed));

					if(mod == 0) {
						final Location healedLoc = healed.getLocation();
						healed.getWorld().spawnParticle(Particle.HEART, healedLoc.add(0, healed.getHeight(), 0), 1);
					}
				}
			}
		}

		/**
		 * Do the line of sight check. A medic and target must have a direct line between each other to heal
		 * and be within MAX_HEALING_DISTANCE length.
		 * @return true if can heal, false if not.
		 */
		private LineOfSight lineOfSightCheck(Player medic, LivingEntity healed) {
			// Start from medic loc + half guardian height as that's where the laser is coming from (likely)
			//final double guardianHeight = 0.85;
			// Actually, use their eye loc for more intuitive gameplay (I can see the target == I can heal them)
			final Location medicLoc = medic.getEyeLocation();
			final Vector medicLocVec = medicLoc.toVector();
			final Vector healedLoc = healed.getLocation().toVector();

			// From medic's eyes to target's eyes
			Vector targetEyes = MathUtils.add(healedLoc.clone(), 0, healed.getEyeHeight(), 0).subtract(medicLocVec);

			if(targetEyes.lengthSquared() > MAX_HEALING_DISTANCE_SQR)
				return LineOfSight.TOO_FAR;

			final Vector[] directions = new Vector[]{
					// From medic's eyes to target's middle
					MathUtils.add(healedLoc, 0, healed.getHeight() / 2, 0).subtract(medicLocVec),
					targetEyes
			};

			for(Vector direction : directions) {
				RayTraceResult result = medic.getWorld().rayTraceBlocks(medicLoc, direction,
						direction.length(), FluidCollisionMode.NEVER, true);

				// A ray didn't hit any blocks, meaning there is a LOS to the target
				if(result == null || result.getHitBlock().getType().isAir())
					return LineOfSight.CAN_HEAL;
			}

			return LineOfSight.NO_LINE_OF_SIGHT;
		}

		private enum LineOfSight {
			TOO_FAR, NO_LINE_OF_SIGHT, CAN_HEAL
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

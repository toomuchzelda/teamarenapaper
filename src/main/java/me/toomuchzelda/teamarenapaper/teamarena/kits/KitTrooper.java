package me.toomuchzelda.teamarenapaper.teamarena.kits;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.abilities.CritAbility;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.teamarena.kits.filter.KitOptions;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class KitTrooper extends Kit {

    public KitTrooper(TeamArena game) {
        super("Trooper", "Your standard issue melee fighter, it can handle most 1-on-1 sword fights " +
                "and can heal itself with Golden Apples. It has a small appetite though, so it can't eat them too often."
				, Material.IRON_SWORD);

        ItemStack[] armour = new ItemStack[4];
        armour[3] = new ItemStack(Material.IRON_HELMET);
        armour[2] = new ItemStack(Material.IRON_CHESTPLATE);
        armour[1] = new ItemStack(Material.IRON_LEGGINGS);
        armour[0] = new ItemStack(Material.IRON_BOOTS);
        this.setArmour(armour);

		ItemStack sword;
		if (KitOptions.trooperRatio)
			sword = ItemBuilder.of(Material.STONE_SWORD)
				.displayName(Component.text("Splitter sword"))
				.lore(
					List.of(Component.text("Hit enemies right in the middle to inflict critical damage", TextUtils.LEFT_CLICK_TO))
				).build();
		else {
			sword = new ItemStack(Material.IRON_SWORD);
			ItemMeta swordMeta = sword.getItemMeta();
			swordMeta.addEnchant(Enchantment.SHARPNESS, 1, false);
			sword.setItemMeta(swordMeta);
		}


        ItemStack gapples = new ItemStack(Material.GOLDEN_APPLE);
        gapples.setAmount(5);

        setItems(sword, gapples);

		setCategory(KitCategory.FIGHTER);

		if (KitOptions.trooperRatio)
			this.setAbilities(new TrooperAbility(), new CritAbility(game));
		else
			this.setAbilities(new TrooperAbility());
    }

	public static class TrooperAbility extends Ability
	{
		private static final int GAPPLE_COOLDOWN_TIME = 15 * 20; //15 secs
		private static final String COOLDOWN_MSG_KEY = "troopercd";

		@Override
		public void removeAbility(Player trooper) {
			// Remove gapple cooldown so it doesn't persist after death.
			trooper.setCooldown(Material.GOLDEN_APPLE, 0);
		}

		/**
		 * Set a cooldown on eating golden apples
		 */
		@Override
		public void onConsumeItem(PlayerItemConsumeEvent event) {
			if(!event.isCancelled()) {
				if(event.getItem().getType() == Material.GOLDEN_APPLE) {
					final int ticksLeft = event.getPlayer().getCooldown(Material.GOLDEN_APPLE);
					if(ticksLeft > 0) { //still has cooldown - cancel event
						event.setCancelled(true);
					}
					else { //just ate - set cooldown
						event.getPlayer().setCooldown(Material.GOLDEN_APPLE, GAPPLE_COOLDOWN_TIME);

					}
				}
			}
			else if(KitVenom.VenomAbility.isVenomBlockingEating(event.getPlayer())) { //event was cancelled before reaching this handler: check if it's venom
				event.getPlayer().sendMessage(Component.text("You're too sick to eat right now!", TextColor.color(100, 166, 58)));
			}
		}

		/**
		 * Tell them if they try to eat a golden apple while they can't
		 */
		@Override
		public void onInteract(PlayerInteractEvent event) {
			final Player eater = event.getPlayer();
			final int ticksLeft = eater.getCooldown(Material.GOLDEN_APPLE);
			if(event.getMaterial() == Material.GOLDEN_APPLE && ticksLeft > 0 && ticksLeft < (GAPPLE_COOLDOWN_TIME - 20) &&
					event.getAction().isRightClick() &&
					Main.getPlayerInfo(eater).messageHasCooldowned(COOLDOWN_MSG_KEY, 3 * 20)) {

				eater.sendMessage(Component.text("You're too full! You can eat another apple in " +
								(ticksLeft / 20) + " seconds.",	NamedTextColor.LIGHT_PURPLE));

				event.getPlayer().playSound(event.getPlayer(), Sound.ENTITY_DONKEY_EAT, SoundCategory.PLAYERS, 1.5f, 1f);
			}
		}
	}
}

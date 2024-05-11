package me.toomuchzelda.teamarenapaper.teamarena.kits;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;


public class KitDwarf extends Kit
{
	public static final int MAX_LEVELS = 20;
	public static final int LEVELS_PER_ENCHANT = 3;
	public static final int MAX_PROTECTION = 5;

	private static final AttributeModifier[] LEVELS_TO_MODIFIER = new AttributeModifier[MAX_LEVELS + 1];

	static {
		//credit jacky8399
		for(int i = 0; i < LEVELS_TO_MODIFIER.length; i++) {
			double slowness = ((double) MAX_LEVELS / 2) - i;
			slowness /= ((double) MAX_LEVELS / 2);
			slowness -= 1;

			if(slowness > 1)
				slowness = 1;

			AttributeModifier modifier = new AttributeModifier("DwarfSlowness" + i, slowness,
					AttributeModifier.Operation.MULTIPLY_SCALAR_1);

			LEVELS_TO_MODIFIER[i] = modifier;
		}
	}

	public KitDwarf() {
		super("Dwarf", "Starting off very weak, It slowly gains power by sneaking, eventually reaching God-like levels." +
						" The stronger it gets though, the slower it becomes.\n\n" +
						"While sneaking, it takes less knockback, becoming harder to push around.",
				Material.EXPERIENCE_BOTTLE);

		ItemStack[] armour = new ItemStack[4];

		armour[3] = new ItemStack(Material.LEATHER_HELMET);
		armour[2] = new ItemStack(Material.LEATHER_CHESTPLATE);
		armour[1] = new ItemStack(Material.LEATHER_LEGGINGS);
		armour[0] = new ItemStack(Material.NETHERITE_BOOTS);

		for(ItemStack armorPiece : armour) {
			ItemMeta meta = armorPiece.getItemMeta();
			if (meta instanceof LeatherArmorMeta leatherMeta) {
				leatherMeta.setColor(Color.BLUE);
				armorPiece.setItemMeta(leatherMeta);
			}
		}
		this.setArmour(armour);

		ItemStack sword = new ItemStack(Material.STONE_SWORD);
		ItemStack[] items = new ItemStack[]{sword};
		this.setItems(items);

		setAbilities(new DwarfAbility());

		setCategory(KitCategory.SUPPORT);
	}

	public static class DwarfAbility extends Ability
	{
		@Override
		public void removeAbility(Player player) {
			//they should only have 1 of these attributemodifiers on at a time, but admin abuse does things
			for(AttributeModifier modifier : player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getModifiers()) {
				if(modifier.getName().startsWith("DwarfSlowness")) {
					player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(modifier);
				}
			}
			player.setExp(0);
		}

		@Override // Reduce knockback taken if shifting
		public void onAttemptedDamage(DamageEvent event) {
			if(event.hasKnockback() && event.getPlayerVictim().isSneaking()) {
				event.setKnockback(event.getKnockback().multiply(0.6d));
			}
		}

		@Override
		public void onPlayerTick(Player player) {
			float expToGain; //perTick

			if (player.isSprinting()) {
				expToGain = -0.03f;
			}
			else if (player.isSneaking()) {
				expToGain = 0.01f;
			}
			else
				expToGain = -0.005f;

			expToGain *= 1 + player.getExp() / 20; // slight acceleration at higher levels

			float newExp = player.getExp() + expToGain;

			int currentLevel = player.getLevel();
			if (newExp > 1) {
				if (currentLevel < MAX_LEVELS) {
					//level up
					player.setLevel(currentLevel + 1);
					newExp = 0;

					level(player, currentLevel);
					player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS,
							1f, 0f);
				}
				else
					newExp = 1;
			}
			else if (newExp < 0) {
				if (currentLevel > 0) {
					//level down
					player.setLevel(currentLevel - 1);
					newExp = 1;

					level(player, currentLevel);
					player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, SoundCategory.PLAYERS, 1f, 0f);
				}
				else
					newExp = 0;
			}
			player.setExp(newExp);
		}

		private void level(Player player, int prevLevel) {
			int enchantLevels = player.getLevel() / LEVELS_PER_ENCHANT;

			AttributeModifier oldMod = LEVELS_TO_MODIFIER[prevLevel];
			player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(oldMod);
			player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addModifier(LEVELS_TO_MODIFIER[player.getLevel()]);

			PlayerInventory inventory = player.getInventory();
			TeamArena game = Main.getGame();
			for (int i = 0; i < inventory.getSize(); i++) {
				ItemStack item = inventory.getItem(i);
				if (item == null)
					continue;

				if(!game.isWearableArmorPiece(item))
					continue;

				ItemMeta meta = item.getItemMeta();
				if (ItemUtils.isArmor(item)) {
					if (enchantLevels == 0)
						meta.removeEnchant(Enchantment.PROTECTION);
					else if (enchantLevels <= MAX_PROTECTION)
						meta.addEnchant(Enchantment.PROTECTION, enchantLevels, true);
				}
				else if (ItemUtils.isSword(item)) {
					if (enchantLevels == 0)
						meta.removeEnchant(Enchantment.SHARPNESS);
					else
						meta.addEnchant(Enchantment.SHARPNESS, enchantLevels, true);
				}
				item.setItemMeta(meta);
			}

			//enchantment levels have changed
			if (prevLevel / LEVELS_PER_ENCHANT != enchantLevels) {
				PlayerInfo pinfo = Main.getPlayerInfo(player);
				int armorLevel = Math.min(MAX_PROTECTION, enchantLevels);
				Component text = Component.text("Armor Protection: " + armorLevel + "    Sword Sharpness: "
						+ enchantLevels).color(TextColor.color(223, 94, 255));

				if (pinfo.getPreference(Preferences.KIT_ACTION_BAR)) {
					player.sendActionBar(text);
				}
				if(pinfo.getPreference(Preferences.KIT_CHAT_MESSAGES)) {
					player.sendMessage(text);
				}
			}
		}
	}
}

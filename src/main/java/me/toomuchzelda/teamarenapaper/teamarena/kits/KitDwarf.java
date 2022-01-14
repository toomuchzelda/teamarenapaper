package me.toomuchzelda.teamarenapaper.teamarena.kits;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.core.ItemUtils;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
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
	
	public KitDwarf(TeamArena teamArena) {
		super("Dwarf", "Starting off very weak, It slowly gains power by sneaking, eventually reaching God-like levels." +
						" The stronger it gets though, the slower it becomes.",
				Material.EXPERIENCE_BOTTLE, teamArena);
		
		ItemStack[] armour = new ItemStack[4];
		
		armour[3] = new ItemStack(Material.LEATHER_HELMET);
		armour[2] = new ItemStack(Material.LEATHER_CHESTPLATE);
		armour[1] = new ItemStack(Material.LEATHER_LEGGINGS);
		armour[0] = new ItemStack(Material.LEATHER_BOOTS);
		
		for(ItemStack armorPiece : armour) {
			LeatherArmorMeta meta = (LeatherArmorMeta) armorPiece.getItemMeta();
			meta.setColor(Color.BLUE);
			armorPiece.setItemMeta(meta);
		}
		this.setArmour(armour);
		
		ItemStack sword = new ItemStack(Material.STONE_SWORD);
		ItemStack[] items = new ItemStack[]{sword};
		this.setItems(items);
		
		setAbilities(new DwarfAbility());
		
		
	}
	
	public static class DwarfAbility extends Ability
	{
		@Override
		public void removeAbility(Player player) {
			player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(LEVELS_TO_MODIFIER[player.getLevel()]);
		}
		
		@Override
		public void onTick(Player player) {
			float expToGain; //perTick
			
			if (player.isSprinting()) {
				expToGain = -0.02f;
			}
			else if (player.isSneaking()) {
				expToGain = 0.005f;
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
			for (int i = 0; i < inventory.getSize(); i++) {
				ItemStack item = inventory.getItem(i);
				if (item == null)
					continue;
				ItemMeta meta = item.getItemMeta();
				if (ItemUtils.isArmor(item)) {
					if (enchantLevels == 0)
						meta.removeEnchant(Enchantment.PROTECTION_ENVIRONMENTAL);
					else if (enchantLevels <= MAX_PROTECTION)
						meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, enchantLevels, true);
				}
				else if (ItemUtils.isSword(item)) {
					if (enchantLevels == 0)
						meta.removeEnchant(Enchantment.DAMAGE_ALL);
					else
						meta.addEnchant(Enchantment.DAMAGE_ALL, enchantLevels, true);
				}
				item.setItemMeta(meta);
			}
			
			//enchantment levels have changed
			if (prevLevel / LEVELS_PER_ENCHANT != enchantLevels) {
				PlayerInfo pinfo = Main.getPlayerInfo(player);
				int armorLevel = Math.min(MAX_PROTECTION, enchantLevels);
				Component text = Component.text("Armor Protection: " + armorLevel + "    Sword Sharpness: "
						+ enchantLevels).color(TextColor.color(223, 94, 255));
				
				if (pinfo.kitActionBar.getValue().value) {
					player.sendActionBar(text);
				}
				if(pinfo.kitChatMessages.getValue().value) {
					player.sendMessage(text);
				}
			}
		}
	}
}
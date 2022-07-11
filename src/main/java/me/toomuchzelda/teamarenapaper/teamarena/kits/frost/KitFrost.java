package me.toomuchzelda.teamarenapaper.teamarena.kits.frost;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.KitCategory;
import me.toomuchzelda.teamarenapaper.teamarena.kits.KitExplosive;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.potion.PotionData;

import java.util.ArrayList;
import java.util.List;

import static me.toomuchzelda.teamarenapaper.teamarena.kits.KitExplosive.ExplosiveAbility.RPG_ARROW_COLOR;
import static me.toomuchzelda.teamarenapaper.teamarena.kits.KitPyro.MOLOTOV_ARROW_COLOR;

public class KitFrost extends Kit
{
	public KitFrost() {
		super("Frost", "Parry + League of Legends Flash", Material.ICE);

		ItemStack flashFreeze = ItemBuilder.of(Material.AMETHYST_SHARD)
				.displayName(Component.text("Flash Freeze"))
				.build();
		ItemStack chest = ItemBuilder.of(Material.LEATHER_CHESTPLATE)
						.color(Color.AQUA)
								.build();

		setItems(new ItemStack(Material.IRON_SWORD), flashFreeze);
		setArmor(new ItemStack(Material.IRON_HELMET), chest,
				new ItemStack(Material.IRON_LEGGINGS), new ItemStack(Material.IRON_BOOTS));

		setAbilities(new FrostAbility());

		setCategory(KitCategory.UTILITY);
	}

	public static class FrostAbility extends Ability {

		@Override
		public void onPlayerTick(Player player){
			deflectTest(player);
		}

		public void deflectTest(Player player){
			List<Entity> nearbyEnt = player.getNearbyEntities(4,4,4);
			nearbyEnt.stream()
					.filter(entity -> (entity.getVelocity().lengthSquared()  >= 0 &&
							(entity instanceof Projectile || entity instanceof Item)))
					.forEach(entity -> {
						//Prevents rockets from being deflected
						// b/c they can cause Frost to TP to very high locations
						if(entity instanceof ShulkerBullet){
							return;
						}
						if(entity instanceof Firework firework){
							ProjDeflect.deflectBurstFirework(player, firework);
						}
						else if(entity instanceof AbstractArrow arrow){
							//For arrows which are associated with special abilities,
							//The shooter must be changed last second to preserve the properties
							if(arrow instanceof Arrow abilityArrow &&
								abilityArrow.getColor() != null){
								if(abilityArrow.getColor().equals(MOLOTOV_ARROW_COLOR) ||
										abilityArrow.getColor().equals(RPG_ARROW_COLOR)){
									//Used to mark the "true" shooter of the arrow
									//the actual shooter must be preserved so the arrows behave
									//according to their respective kit's implementation
									if(abilityArrow.hasMetadata("shooterOverride")){
										//First, clear any other past overrides if
										//another Frost had already deflected the projectile
										abilityArrow.removeMetadata("shooterOverride",
												Main.getPlugin());
									}
									abilityArrow.setMetadata("shooterOverride",
											new FixedMetadataValue(Main.getPlugin(), player));
									ProjDeflect.deflectSameShooter(player, abilityArrow);
								}
								else{
									ProjDeflect.deflectArrow(player, arrow);
								}
							}
							else{
								ProjDeflect.deflectArrow(player, arrow);
							}
						}
						else if(entity instanceof EnderPearl pearl){
							ProjDeflect.deflectSameShooter(player, pearl);
						}
						else if (entity instanceof Item item){
							if(item.getItemStack().getType() == Material.TURTLE_HELMET ||
									item.getItemStack().getType() == Material.HEART_OF_THE_SEA ||
									item.getItemStack().getType() == Material.FIREWORK_STAR) {
								ProjDeflect.addShooterOverride(player, item);
								ProjDeflect.deflectSameShooter(player, item);
							}
						}
						else{
							//For non-specific projectiles that are not used for kits
							ProjDeflect.deflectProj(player, (Projectile) entity);
						}
					});
		}
	}
}

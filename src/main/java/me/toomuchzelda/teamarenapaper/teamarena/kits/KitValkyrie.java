package me.toomuchzelda.teamarenapaper.teamarena.kits;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import net.kyori.adventure.sound.SoundStop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.Particle.DustTransition;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;

import static me.toomuchzelda.teamarenapaper.utils.TextUtils.*;

//Kit Description:
/*
    Kit Goal: Initiator / Fighter kit w/ cool melee mechanics

    Slowed while holding axe

    Sub-Ability: Spin Attack
        Attacks have a short charge uptime and short CD
        Attacks hit ALL enemies within 3 block range
        Damage based on distance from player
            [0,1) block = Regular dmg
            [1,2] block = +1 Sharpness
            [2, 3) block = +3 Sharpness


	Main Ability: Gravity Bomb
		Charge CD: 12 seconds
        Detonation Time: 1.5 seconds

        Launches a Heart of the Sea like an explosive grenade,
        which will detonate after a short fuse time.
        (Can be thrown with low or high velocity depending on type of click)

	    Gravity Bomb Detonation
            Upon detonation, the bomb will pull enemies towards itself
            Before exploding and dealing very light damage
*/
/**
 * @author onett425
 */
public class KitValkyrie extends Kit{
	public static final ItemStack VALK_AXE;
	public static final Material VALK_AXE_MAT;
    public static final int AXE_SHARP = 1;
    public static final AttributeModifier AXE_SLOW = new AttributeModifier("Valk Axe Slow", -0.30 , AttributeModifier.Operation.ADD_SCALAR);
    public static final int AXE_WINDUP = 10;
    public static final int AXE_CD = 30;
    public static final int ATTACK_RADIUS = 3;
    //Bonus damage adds extra sharpness levels to the given hit
    public static final int NORMAL_BONUS = 1;
    public static final int SWEET_SPOT_BONUS = 3;

    public static final ItemStack GRAV_BOMB;
    public static final int BOMB_CD = 10 * 20;
    public static final int DETONATION_TIME = 30;
    public static final double PULL_AMP = 0.2;

	public static final Component BOMB_DESC = ItemUtils.noItalics(
			Component.text("Pulls enemies towards itself upon detonation").color(LORE_YELLOW));

    public static final Set<BukkitTask> VALK_TASKS = new HashSet<>();

	public static final Component AXE_DESC = ItemUtils.noItalics(
			Component.text("After a short windup period (as seen in exp bar), ")
					.color(LORE_YELLOW));
	public static final Component AXE_DESC2 = ItemUtils.noItalics(
			Component.text("Battle Axe hits all enemies within your attack radius")
					.color(LORE_YELLOW));
	public static final Component AXE_DESC3 = ItemUtils.noItalics(
			Component.text("Hit enemies near the edge to maximize damage!").color(LORE_BROWN));
	public static final Component AXE_DESC4 = ItemUtils.noItalics(
			Component.text("Direct Attacks are disabled").color(ERROR_RED));
	public static final List<Component> AXE_LORE_LIST;

    static{
        VALK_AXE = new ItemStack(Material.DIAMOND_AXE);
        ItemMeta valkMeta = VALK_AXE.getItemMeta();
        valkMeta.addEnchant(Enchantment.DAMAGE_ALL, AXE_SHARP, true);
        valkMeta.addEnchant(Enchantment.KNOCKBACK, 1, false);
        valkMeta.displayName(ItemUtils.noItalics(Component.text("Battle Axe")));
        valkMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

		ArrayList<Component> axeLore = new ArrayList<>(4);
		axeLore.add(AXE_DESC);
		axeLore.add(AXE_DESC2);
		axeLore.add(AXE_DESC3);
		axeLore.add(AXE_DESC4);
		AXE_LORE_LIST = Collections.unmodifiableList(axeLore);
		valkMeta.lore(AXE_LORE_LIST);
		VALK_AXE.setItemMeta(valkMeta);

        GRAV_BOMB = new ItemStack(Material.HEART_OF_THE_SEA);
        ItemMeta gravMeta = GRAV_BOMB.getItemMeta();
        gravMeta.displayName(ItemUtils.noItalics(Component.text("Gravity Bomb")));
        ArrayList<Component> bombLore = new ArrayList<>(3);
		bombLore.add(BOMB_DESC);
		bombLore.add(BOMB_LORE);
		bombLore.add(BOMB_LORE2);
        gravMeta.lore(bombLore);
        GRAV_BOMB.setItemMeta(gravMeta);

		VALK_AXE_MAT = VALK_AXE.getType();
    }

    public KitValkyrie(){
        super("Valkyrie", "A tanky melee kit which must pay special attention to timing and spacing. " +
						"It can easily deal with large groups of enemies with its AoE attack and gravity bomb!",
				Material.DIAMOND_AXE);
        ItemStack helm = new ItemStack(Material.IRON_HELMET);
        ItemStack chest = new ItemStack(Material.DIAMOND_CHESTPLATE);
        ItemMeta chestMeta = chest.getItemMeta();
        chestMeta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, false);
        chest.setItemMeta(chestMeta);
        ItemStack legs = new ItemStack(Material.CHAINMAIL_LEGGINGS);
        ItemStack boots = new ItemStack(Material.IRON_BOOTS);
        ItemMeta bootMeta = boots.getItemMeta();
        bootMeta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, false);
        boots.setItemMeta(bootMeta);

        this.setArmor(helm, chest, legs, boots);
        setItems(VALK_AXE, GRAV_BOMB);
        setAbilities(new ValkyrieAbility());

		setCategory(KitCategory.FIGHTER);
    }

    public static class ValkyrieAbility extends Ability{

        //ACTIVE_AXE tracks when attacks should be allowed (should only be via axe ability)
        //If it contains a player, they are allowed to attack with axe
        public static final Set<Player> ACTIVE_AXE = new HashSet<>();
        public final HashMap<Player, Integer> BOMB_RECHARGES = new HashMap<>();
		private final Set<Player> RECEIVED_AXE_CHAT_MESSAGE = new HashSet<>();

        public void unregisterAbility() {
			Iterator<BukkitTask> iter = VALK_TASKS.iterator();
			while(iter.hasNext()) {
				BukkitTask task = iter.next();
				task.cancel();
				iter.remove();
			}
		}

        public void removeAbility(Player player) {
			player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(AXE_SLOW);
            player.setExp(0);
            ACTIVE_AXE.remove(player);
			RECEIVED_AXE_CHAT_MESSAGE.remove(player);
		}

        public void onAttemptedAttack(DamageEvent event) {
            Player player = (Player) event.getAttacker();
            ItemStack item = player.getInventory().getItemInMainHand();
            if(item.getType() == Material.DIAMOND_AXE){
                //This means an attack is being initialized,
                //Wind up is called, no damage is dealt by the actual hit.
                if(player.getExp() == 0 && !player.hasCooldown(Material.DIAMOND_AXE)){
                    windUp(player);
                    event.setCancelled(true);
                }
                if(!ACTIVE_AXE.contains(player)){
                    event.setCancelled(true);
					//O
					if(RECEIVED_AXE_CHAT_MESSAGE.add(player)){
						player.sendMessage(Component.text("Damage can only be dealt with your spin attack!").color(ERROR_RED));
					}
                }
            }
            //Preventing friendly-fire from explosion
            if(event.getDamageType() == DamageType.EXPLOSION && Main.getGame().canAttack(player, event.getPlayerVictim())){
                event.setCancelled(true);
            }
        }

        public void onPlayerTick(Player player) {
            //stolen from jacky, applies slow when axe is held
            AttributeInstance instance = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
            World world = player.getWorld();
			if(player.getInventory().getItemInMainHand().getType() == Material.DIAMOND_AXE){
				if(!instance.getModifiers().contains(AXE_SLOW)) {
					instance.addModifier(AXE_SLOW);
				}
			}
			else{
				if(instance.getModifiers().contains(AXE_SLOW)){
					instance.removeModifier(AXE_SLOW);
				}
			}

            //Controls when Axe is allowed to attack
            if(player.getCooldown(Material.DIAMOND_AXE) <= AXE_CD - 3 && ACTIVE_AXE.contains(player)){
                ACTIVE_AXE.remove(player);
            }

            //Particle effect: Indicator that Axe is off CD
            if(player.getInventory().getItemInMainHand().getType() == Material.DIAMOND_AXE && !player.hasCooldown(Material.DIAMOND_AXE)){
                world.spawnParticle(Particle.CRIT, player.getLocation().add(0, 1, 0), 1);
            }
            //Sound Effect: Indicates Axe is now off CD
            if(player.getCooldown(Material.DIAMOND_AXE) == 1){
                player.playSound(player, Sound.ITEM_AXE_SCRAPE, 1.0f, 1.3f);
            }

            //Gravity Bomb Action Bar
            if(Main.getPlayerInfo(player).getPreference(Preferences.KIT_ACTION_BAR) &&
				player.getInventory().getItemInMainHand().getType() == Material.HEART_OF_THE_SEA){
                Component actionBar = Component.text("Left Click to THROW    Right Click to TOSS").color(TextColor.color(3, 182, 252));
                player.sendActionBar(actionBar);
            }
        }

        public void onTick() {
            //Stolen from toomuchzelda, manages giving out gravity bombs
			int currentTick = TeamArena.getGameTick();
			var itemIter = BOMB_RECHARGES.entrySet().iterator();
			while(itemIter.hasNext()) {
				Map.Entry<Player, Integer> entry = itemIter.next();
				if(currentTick - entry.getValue() >= BOMB_CD){
					entry.getKey().getInventory().addItem(GRAV_BOMB);
					itemIter.remove();
				}
			}

			//clean up cancelled or ended bukkit tasks
			VALK_TASKS.removeIf(BukkitTask::isCancelled);
        }

        public void onInteract(PlayerInteractEvent event) {
            Player player = event.getPlayer();
            PlayerInventory inv = player.getInventory();
            Material mat = event.getMaterial();
            Action action = event.getAction();

            //Initializes Wind-Up when an attack does not hit a player
            if((action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) && mat == Material.DIAMOND_AXE && player.getExp() == 0 && !player.hasCooldown(Material.DIAMOND_AXE)){
                    windUp(player);
            }

            //Throwing Gravity Bombs
            if(mat == Material.HEART_OF_THE_SEA){
                if(action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK){
                    inv.setItem(inv.getHeldItemSlot(), inv.getItem(inv.getHeldItemSlot()).subtract());
					throwGrenade(player, 1.5d);
                }
                if(action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK){
                    inv.setItem(inv.getHeldItemSlot(), inv.getItem(inv.getHeldItemSlot()).subtract());
					throwGrenade(player, 0.8d);
                }
            }
        }

        //When Axe is swung, wind-up period must be completed to initiate an attack
        public void windUp(Player player){
            World world = player.getWorld();
            world.playSound(player, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.78f);
            BukkitTask runnable = new BukkitRunnable() {
                int duration = AXE_WINDUP;
                //isAxe ensures that the axe is held in hand for the whole wind-up time.
                boolean isAxe = true;
                float expInc = 1/(float)AXE_WINDUP;

                public void run(){
                    if(duration <= 0){
                        valkAttack(player);
                        cancel();
                        player.setExp(0);
                    }
                    else{
                        duration--;
                        isAxe = player.getInventory().getItemInMainHand().getType() == Material.DIAMOND_AXE;
                        //Exp bar visually indicates time until hit is initiated
                        if(player.getExp() + expInc >= 1){
                            player.setExp(0.999f);
                        }
                        else{
                            player.setExp(player.getExp() + expInc);
                        }

                        if(!isAxe){
                            cancel();
                            player.setExp(0);
                        }
                    }
                }
            }.runTaskTimer(Main.getPlugin(), 0, 0);
            VALK_TASKS.add(runnable);
        }

        //An AOE attack that follows a succesful windup, hits all enemies within an area, applies bonus damage based on distance
        public void valkAttack(Player player){
            List<Entity> nearbyEnt = player.getNearbyEntities(ATTACK_RADIUS, ATTACK_RADIUS, ATTACK_RADIUS);
            Location playerLoc = player.getLocation();
            ItemStack item = player.getInventory().getItemInMainHand();
            ItemMeta meta = item.getItemMeta();
            World world = player.getWorld();
            Iterator<Entity> iter = nearbyEnt.iterator();
            while(iter.hasNext()){
                Entity entity = iter.next();
				double distance = entity.getLocation().distance(playerLoc);
                if (entity instanceof LivingEntity victim && !victim.getType().equals(EntityType.ARMOR_STAND) &&
						distance <= ATTACK_RADIUS){

					Vector attackerToVictim = victim.getEyeLocation().subtract(player.getEyeLocation()).toVector().normalize();
					//Raytrace checks for blocks between attacker and victim,
					//if collidable block is found, the attack is cancelled
					RayTraceResult rayTrace = null;
					//When distance = 0, rayTrace throws exception (i.e. you TP to someone and are in the exact same spot)
					if(distance != 0){
						rayTrace = player.getWorld().rayTraceBlocks(
								player.getEyeLocation(), attackerToVictim, distance,
								FluidCollisionMode.NEVER, true);
					}

					if(rayTrace == null){
						ACTIVE_AXE.add(player);

						//If distance is < 1, it is a "sour hit" and no bonus damage is given
						if (distance < 1) {

						} else if (distance < 2.6 && meta != null) {
							meta.addEnchant(Enchantment.DAMAGE_ALL, AXE_SHARP + NORMAL_BONUS, true);
						} else if (distance >= 2.6 && meta != null) {
							meta.addEnchant(Enchantment.DAMAGE_ALL, AXE_SHARP + SWEET_SPOT_BONUS, true);
							player.playSound(player, Sound.BLOCK_GRINDSTONE_USE, 1.0f, 1.9f);
						}
						if(meta != null){
							item.setItemMeta(meta);
							item.addEnchantment(Enchantment.DAMAGE_ALL, AXE_SHARP);
						}
						player.attack(victim);
					}
				}
                iter.remove();
            }
            axeParticles(player);
            world.playSound(player, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.6f);
            player.setCooldown(Material.DIAMOND_AXE, AXE_CD);
        }

        //Particle effect to show the effective range of valkAttack
        public void axeParticles(Player player){
            Location loc = player.getLocation();
            loc.add(0, 1, 0);
            World world = player.getWorld();
            final TeamArenaTeam team = Main.getPlayerInfo(player).team;
			Color teamColor = team.getColour();
            DustTransition particle = new DustTransition(teamColor, Color.WHITE, 2);
            BukkitTask runnable = new BukkitRunnable() {

                int duration = 4;
                int increment = 0;

                public void run() {
                    if(increment >= duration){
                        cancel();
                    }
                    else{
                        for(int i = 0; i < 10; i++){
                            double rad = (increment * Math.PI / 2) + (i * Math.PI / (10 * 2));
                            double radius = 3;
                            loc.add(radius * Math.cos(rad), 0, radius * Math.sin(rad));
                            world.spawnParticle(Particle.DUST_COLOR_TRANSITION, loc, 1, 0,0,0, 5.0f, particle);
                            loc.subtract(radius * Math.cos(rad), 0, radius * Math.sin(rad));
                        }
                        increment++;


                    }

                }
            }.runTaskTimer(Main.getPlugin(), 0, 0);
            VALK_TASKS.add(runnable);
        }

        public void throwGrenade(Player player, double amp){
			World world = player.getWorld();
			Location location = player.getEyeLocation();
            final TeamArenaTeam team = Main.getPlayerInfo(player).team;
			Color teamColor = team.getColour();

			Item activeGrenade = world.dropItem(location, new ItemStack(Material.HEART_OF_THE_SEA), item -> {
				item.setCanPlayerPickup(false);
				item.setCanMobPickup(false);
				item.setVelocity(location.getDirection().multiply(amp));
			});
		    world.playSound(activeGrenade, Sound.ENTITY_CREEPER_PRIMED, 1f, 1.1f);

			BukkitTask runnable = new BukkitRunnable(){
				//Grenade explosion
				int timer = DETONATION_TIME;
                DustTransition particle = new DustTransition(teamColor, Color.WHITE, 2);
				public void run() {
						//Grenade Particles when it is thrown
						//In Motion
						if(activeGrenade.getVelocity().length() > 0){
							world.spawnParticle(Particle.REDSTONE, activeGrenade.getLocation(), 1, new Particle.DustOptions(teamColor, 2f));
                            world.spawnParticle(Particle.ENCHANTMENT_TABLE, activeGrenade.getLocation(), 8, 0,0,0, 0.5f);
						}
						else{
							//On the ground
							world.spawnParticle(Particle.DUST_COLOR_TRANSITION, activeGrenade.getLocation().add(Vector.getRandom().subtract(new Vector(-0.5,-0.5,-0.5)).multiply(4)), 2, particle);
                            world.spawnParticle(Particle.ENCHANTMENT_TABLE, activeGrenade.getLocation(), 20,  1,1,1, 1.5f);
						}
                        timer--;
					if(timer <= 0){
                        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.0f, 1.3f);
                        //Pull-in + Explosion
                        BukkitTask runnable = new BukkitRunnable(){

                            List<Entity> affectedEnemies = activeGrenade.getNearbyEntities(5, 5, 5);
                            int duration = 5;
                            Location currLoc = activeGrenade.getLocation().clone();

                            public void run(){
                                if(duration <= 0){
                                    //Explosion
                                    world.createExplosion(activeGrenade.getLocation(), 0.2f, false, false);
                                    player.stopSound(Sound.ENTITY_GENERIC_EXPLODE);
                                    player.getWorld().stopSound(SoundStop.named(Sound.ENTITY_GENERIC_EXPLODE));
                                    activeGrenade.remove();
                                    BOMB_RECHARGES.put(player, TeamArena.getGameTick());
                                    cancel();
                                }
                                else{
                                    for(Entity entity : affectedEnemies){
                                        //Pull-in
                                        if(entity.getLocation().distanceSquared(activeGrenade.getLocation()) <= 25 &&
										entity instanceof org.bukkit.entity.LivingEntity victim && !(entity.getType().equals(EntityType.ARMOR_STAND))){
                                            if(!(victim instanceof Player p) || Main.getGame().canAttack(player, p)) {
												//Preventing spectators from getting pulled in
												if(victim instanceof Player p && Main.getPlayerInfo(p).activeKit != null){
													//Vector currVel = entity.getVelocity().clone();
													Vector nadeLoc = activeGrenade.getLocation().clone().toVector();
													Vector entLoc = entity.getLocation().clone().toVector();
													entity.setVelocity(nadeLoc.subtract(entLoc).multiply(new Vector(PULL_AMP, PULL_AMP/8, PULL_AMP)));
												}
                                            }
                                        }
                                    }

                                    //Particle Effect, Shows the max radius of grenade pull then shrinks
                                    //duration * (grenadeRadius / 5.0);
									double radius = duration * (5.0/5.0);
                                    for(int i = 0; i < 10; i++){
                                        Location locClone = currLoc.clone();
                                        double rad = i * (2 * Math.PI) / 10;
                                        double x = radius * Math.sin(rad);
                                        double z = radius * Math.cos(rad);
                                        locClone.add(x, 0, z);
                                        world.spawnParticle(Particle.REDSTONE, locClone, 1, new Particle.DustOptions(teamColor, 2f));
                                    }
                                    duration--;
                                }
                            }
                        }.runTaskTimer(Main.getPlugin(), 0, 0);
                        VALK_TASKS.add(runnable);

                        cancel();
					}
				}
			}.runTaskTimer(Main.getPlugin(), 0, 0);
            VALK_TASKS.add(runnable);
		}
    }
}

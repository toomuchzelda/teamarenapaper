package me.toomuchzelda.teamarenapaper.core;

import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import net.kyori.adventure.text.Component;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import org.bukkit.EntityEffect;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.craftbukkit.v1_17_R1.CraftSound;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class EntityUtils {

    public static Method getHurtSoundMethod;
    public static Method getSoundVolumeMethod;

    public static void cacheReflection() {
        try {
            //Mojang Mapping: getHurtSound(), Spigot: getSoundHurt
            getHurtSoundMethod = net.minecraft.world.entity.LivingEntity.class
                    .getDeclaredMethod("getSoundHurt", DamageSource.class);
            getHurtSoundMethod.setAccessible(true);

            getSoundVolumeMethod = net.minecraft.world.entity.LivingEntity.class.getDeclaredMethod("getSoundVolume");
            getSoundVolumeMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public static Component getName(Entity entity) {
        Component entityName;
        if(entity == null)
            entityName = Component.text("Unknown");
        else if(entity instanceof Player p)
            entityName = p.playerListName();
        else if(entity.customName() != null)
            entityName = entity.customName();
        else
            entityName = entity.name();

        return entityName;
    }

    /**
     * play entity hurt animation and sound
     * @param entity LivingEntity being damaged
     */
    public static void playHurtAnimation(LivingEntity entity, DamageType damageType) {

        //never mind, there's API for it...
        // double never mind, doesn't always choose the right sound? i.e direct fire damage sounds wrong to the person
        // in the fire
        /*EntityEffect effect;
        if (DamageType.DROWNED == damageType) {
            effect = EntityEffect.HURT_DROWN;
        } else if (DamageType.EXPLOSION == damageType) {
            effect = EntityEffect.HURT_EXPLOSION;
        } else if (DamageType.BERRY_BUSH == damageType) {
            effect = EntityEffect.HURT_BERRY_BUSH;
        } else if (DamageType.THORNS == damageType) {
            effect = EntityEffect.THORNS_HURT;
        } else {
            effect = EntityEffect.HURT;
        }
        entity.playEffect(effect);*/

        //byte argument from wiki.vg
        // https://wiki.vg/Protocol#Entity_Status
        // https://wiki.vg/Entity_statuses

        net.minecraft.world.entity.LivingEntity nmsLivingEntity = ((CraftLivingEntity) entity).getHandle();
        ClientboundAnimatePacket packet = new ClientboundAnimatePacket(nmsLivingEntity, ClientboundAnimatePacket.HURT);

        //get and construct sound
        SoundEvent nmsSound;
        float pitch;
        float volume;

        try {
            nmsSound = (SoundEvent) getHurtSoundMethod.invoke(nmsLivingEntity, damageType.getDamageSource());
            pitch = nmsLivingEntity.getVoicePitch();
            volume = (float) getSoundVolumeMethod.invoke(nmsLivingEntity);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();

            nmsSound = SoundEvents.GOAT_SCREAMING_HURT;
            pitch = 0.5f;
            volume = 9999f;
        }
        Sound sound = CraftSound.getBukkit(nmsSound);

        //if a player send the packet to self as well
        // and use player sound category for the sound
        SoundCategory category = SoundCategory.NEUTRAL;
        if(entity instanceof Player p) {
            PlayerUtils.sendPacket(p, packet);
            category = SoundCategory.PLAYERS;
            p.playSound(entity.getLocation(), sound, category, volume, pitch);
        }

        for(Player p : entity.getTrackedPlayers()) {
            PlayerUtils.sendPacket(p, packet);
            p.playSound(entity.getLocation(), sound, category, volume, pitch);
        }
    }
}

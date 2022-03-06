package me.toomuchzelda.teamarenapaper.utils;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import net.kyori.adventure.text.Component;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.craftbukkit.v1_18_R2.CraftSound;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftLivingEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class EntityUtils {

    public static Method getHurtSoundMethod;

    public static void cacheReflection() {
        try {
            getHurtSoundMethod = net.minecraft.world.entity.LivingEntity.class
                    .getDeclaredMethod("c", DamageSource.class);
            getHurtSoundMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public static Component getName(Entity entity) {
        Component entityName;
        if (entity == null)
            entityName = Component.text("Unknown");
        else if (entity instanceof Player p)
            entityName = p.playerListName();
        else if (entity.customName() != null)
            entityName = entity.customName();
        else
            entityName = entity.name();

        return entityName;
    }

    /**
     * play critical hit animation on entity
     *
     * @param entity Entity playing the effect on
     */
    public static void playCritEffect(Entity entity) {
        net.minecraft.world.entity.Entity nmsEntity = ((CraftEntity) entity).getHandle();
        ClientboundAnimatePacket packet = new ClientboundAnimatePacket(nmsEntity, ClientboundAnimatePacket.CRITICAL_HIT);

        //if a player, send packet to self
        if (entity instanceof Player p) {
            PlayerUtils.sendPacket(p, packet);
        }

        //send to all viewers
        for (Player p : entity.getTrackedPlayers()) {
            PlayerUtils.sendPacket(p, packet);
        }
    }

    /**
     * play entity hurt animation and sound
     *
     * @param entity LivingEntity being damaged
     */
    public static void playHurtAnimation(LivingEntity entity, DamageType damageType, boolean deathSound) {
        net.minecraft.world.entity.LivingEntity nmsLivingEntity = ((CraftLivingEntity) entity).getHandle();
        ClientboundAnimatePacket packet = new ClientboundAnimatePacket(nmsLivingEntity, ClientboundAnimatePacket.HURT);

        //get and construct sound
        SoundEvent nmsSound;
        float pitch;
        float volume;

        try {
            if (deathSound)
                nmsSound = nmsLivingEntity.getDeathSound();
                //nmsSound = (SoundEvent) getDeathSoundMethod.invoke(nmsLivingEntity);
            else
                nmsSound = (SoundEvent) getHurtSoundMethod.invoke(nmsLivingEntity, damageType.getDamageSource());

            pitch = nmsLivingEntity.getVoicePitch();
            volume = nmsLivingEntity.getSoundVolume(); //(float) getSoundVolumeMethod.invoke(nmsLivingEntity);
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
        if (entity instanceof Player p) {
            //optional tilt the screen
            if (Main.getPlayerInfo(p).getPreference(Preferences.DAMAGE_TILT))
                PlayerUtils.sendPacket(p, packet);

            category = SoundCategory.PLAYERS;
            p.playSound(entity.getLocation(), sound, category, volume, pitch);
        }

        for (Player p : entity.getTrackedPlayers()) {
            PlayerUtils.sendPacket(p, packet);
            p.playSound(entity.getLocation(), sound, category, volume, pitch);
        }
    }
}

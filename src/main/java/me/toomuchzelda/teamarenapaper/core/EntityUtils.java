package me.toomuchzelda.teamarenapaper.core;

import net.kyori.adventure.text.Component;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
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
     * play entity hurt animation on an Entity with packet
     * @param entity LivingEntity being damaged
     */
    public static void playHurtAnimation(LivingEntity entity) {
        //byte argument from wiki.vg
        // https://wiki.vg/Protocol#Entity_Status
        // https://wiki.vg/Entity_statuses

        net.minecraft.world.entity.LivingEntity nmsLivingEntity = ((CraftLivingEntity) entity).getHandle();
        ClientboundEntityEventPacket packet = new ClientboundEntityEventPacket(nmsLivingEntity, (byte) 2);


        SoundCategory category = SoundCategory.NEUTRAL;
        //if a player send the packet to self
        // and use player sound category for the sound
        if(entity instanceof Player p) {
            PlayerUtils.sendPacket(p, packet);
            category = SoundCategory.PLAYERS;
        }

        //get and construct sound
        SoundEvent nmsSound;
        float pitch;
        float volume;

        try {
            nmsSound = (SoundEvent) getHurtSoundMethod.invoke(nmsLivingEntity, DamageSource.GENERIC);
            pitch = nmsLivingEntity.getVoicePitch();
            volume = (float) getSoundVolumeMethod.invoke(nmsLivingEntity);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();

            nmsSound = SoundEvents.GOAT_SCREAMING_HURT;
            pitch = 0.5f;
            volume = 9999f;
        }

        Sound sound = CraftSound.getBukkit(nmsSound);

        for(Player p : entity.getTrackedPlayers()) {
            PlayerUtils.sendPacket(p, packet);
            p.playSound(entity.getLocation(), sound, category, volume, pitch);
        }
    }
}
